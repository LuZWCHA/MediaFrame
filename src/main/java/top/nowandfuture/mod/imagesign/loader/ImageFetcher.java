package top.nowandfuture.mod.imagesign.loader;

import com.mojang.blaze3d.systems.RenderSystem;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.nowandfuture.mod.imagesign.caches.ImageEntity;
import top.nowandfuture.mod.imagesign.caches.ImageEntityCache;
import top.nowandfuture.mod.imagesign.caches.Vector3d;
import top.nowandfuture.mod.imagesign.utils.DownloadUtil;
import top.nowandfuture.mod.imagesign.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Image Fetcher will get the image from caches in order: Memory -> Disk -> Network.
 * And also save the network picture to disk and try to add it into the memory-limit-cache.
 */
public enum ImageFetcher {
    INSTANCE;

    private CacheSetting config;

    private ImageEntityCache cache;

    private ImageLoader loader;

    private List<Consumer<IEvent>> stageListener;

    private final Logger LOGGER;

    private final Set<String> blackUrls;
    private boolean isInit = false;

    ImageFetcher() {
        this.LOGGER = LogManager.getLogger(getClass());
        //default image loader
        this.loader = new ImageIOLoader();
        this.blackUrls = new HashSet<>();
        this.stageListener = new LinkedList<>();
    }

    public synchronized void init(@NonNull ImageFetcher.CacheSetting config) {
        this.config = config;

        if (this.cache == null) {
            this.cache = new ImageEntityCache(config.cacheMaxSize, config.imageMaxSize, config.cacheMemoryLimit);
            isInit = true;
            this.LOGGER.info("Initializing image fetcher..., the config setting is: {}", config);
        } else {
            isInit = false;
            resetCache(config);
            isInit = true;
        }

    }

    public synchronized void resetCache(@NonNull ImageFetcher.CacheSetting config) {
        this.cache.reset(config.cacheMaxSize, config.imageMaxSize, config.cacheMemoryLimit);
        this.LOGGER.info("Reset image fetcher..., the config setting is: {}", config);
    }

    public ImageEntityCache getCache() {
        return cache;
    }

    public void setSavePath(String path, String dirName) {
        config.defaultDiskSavePath = path;
        config.orgImageSaveDir = dirName;
    }

    public ImageEntity grabImage(String url, long pos) {
        return checkCache(url, pos).blockingFirst(ImageEntity.EMPTY);
    }

    public void dispose() {
        if (RenderSystem.isOnRenderThreadOrInit()) {
            cache.dispose();
            blackUrls.clear();
        } else {
            RenderSystem.recordRenderCall(this::dispose);
        }

    }

    @Deprecated
    public void reload() {
        LOGGER.info("Reload GLSources: {} images to reload.", cache.size());
        cache.markUpdate();
    }

    public void clear(String url, boolean deleteFile) {
        if (cache.contain(url)) {
            if (deleteFile) {
                try {
                    deleteTempFile(url);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            cache.removeImage(url);
        }

        blackUrls.remove(url);
    }

    public ImageLoadTask createImageLoadTask(String url, long blockPos){
        return new MinecraftSignLoadTask(blockPos, url);
    }

    public void reloadImageSmooth(String url, long blockPos) {
        if (cache.contain(url)) {
            try {
                deleteTempFile(url);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ImageLoadTask loadTask = createImageLoadTask(url, blockPos);
            ImageLoadManager.INSTANCE.addToLoad(loadTask);
        }
    }

    public @NonNull Observable<ImageEntity> get(String url, long blockPos, Scheduler curSch) {
        //Fetch from cache
        return Observable.concat(this.load(url, blockPos), this.fetch(url, blockPos))
                .first(ImageEntity.EMPTY)
                .observeOn(curSch)
                .toObservable()
                .doOnError(throwable -> {
                    if (throwable instanceof OutOfMemoryError) {
                        LOGGER.warn("Check the config to increase the memory:)");
                        sendEvent(new FetchInfo(Stage.FAILED, blockPos, "Check the config to increase the memory:)"));
                    } else {
                        LOGGER.info("Observer stream: ", throwable);
                        sendEvent(new FetchInfo(Stage.FAILED, blockPos, throwable.getMessage()));
                    }
                });
    }

    private Observable<ImageEntity> checkCache(String url, long pos) {
        return Observable.create(e -> {
            final ImageEntity queryEntity = getCache().findByPos(pos);
            //If the url changed, remove the position.
            if (queryEntity != ImageEntity.EMPTY && !url.equals(queryEntity.url)) {
                getCache().removeEntity(queryEntity.url, pos);
            }

            //If find the url, try to merge the position.
            if (getCache().contain(url)) {
                final ImageEntity entity = getCache().get(url);
                entity.merge(url, pos);
                e.onNext(entity);
            }

            e.onComplete();
        });
    }

    private Observable<ImageEntity> load(String url, long blockPos) {
        return Observable.create((ObservableOnSubscribe<ImageEntity>) e -> {
            final String name = encodeUrl(url);
            final Path diskPath = Paths.get(config.defaultDiskSavePath, config.orgImageSaveDir, name);
            LOGGER.info("Loading image: {}", url);
            sendEvent(new FetchInfo(Stage.LOADING, blockPos, url));

            final ImageLoader.ImageData data = loadFromDisk(diskPath);
            if (data != null) {
                final ImageEntity entity = ImageEntity.create(url, blockPos, data);
                LOGGER.info("Image loaded: {}", url);

                entity.setImageInfo(data.getImageInfo());
                LOGGER.info("Caching image: {}", url);
                sendEvent(new FetchInfo(Stage.CACHING, blockPos, url));

                long sizeLimit = ImageFetcher.INSTANCE.getCache().getSingleImageMaxSize();
                long imageSize = entity.imageInfo.getSize();
                if (imageSize > sizeLimit) {
                    e.tryOnError(new RuntimeException(
                            String.format("Image is too big, the image max size limit is: %d, but the image size is %d.",
                                    sizeLimit, imageSize)
                    ));
                } else {
                    final ImageEntity added = cache.add(entity);
                    if (added.equals(ImageEntity.EMPTY)) {
                        e.tryOnError(
                                new OutOfMemoryError(
                                        String.format("Out of memory of Cache: cache left memory is %d bytes, " +
                                                        "but the object entry's size is %d bytes.",
                                                cache.getLeftMemory(), entity.imageInfo.getSize())));
                    } else {
                        LOGGER.info("Cached image: {}", url);
                        e.onNext(entity);
                    }
                }
            }
            e.onComplete();
        }).subscribeOn(Schedulers.io());
    }

    private Observable<ImageEntity> fetch(String url, long blockPos) {
        return Observable.create((ObservableOnSubscribe<ImageEntity>) e -> {
            final String name = ImageFetcher.this.encodeUrl(url);
            final Path diskPath = Paths.get(config.defaultDiskSavePath, config.orgImageSaveDir, name);
            final File parentDir = diskPath.toFile().getParentFile();
            if (!parentDir.exists()) {
                Files.createDirectories(parentDir.toPath());
            }
            LOGGER.info("Downloading image: {}", url);
            sendEvent(new FetchInfo(Stage.DOWNLOADING, blockPos, url));
            File file = null;
            try {
                file = loader.fetch(url, diskPath.toFile(), new DownloadUtil.DownloadListener(){
                    @Override
                    public void onProgress(long p, long total) {
                        // TODO: 2022/1/30
                        super.onProgress(p, total);
                    }
                });
            }catch (Exception exception){
                e.tryOnError(exception);
            }

            if (file != null) {
                LOGGER.info("Loading image: {}", url);
                sendEvent(new FetchInfo(Stage.LOADING, blockPos, url));
                ImageLoader.ImageData data = ImageFetcher.this.loadFromDisk(diskPath);
                if (data != null) {
                    final ImageEntity entity = ImageEntity.create(url, blockPos, data);
                    entity.setImageInfo(data.getImageInfo());
                    LOGGER.info("Caching image: {}", url);
                    sendEvent(new FetchInfo(Stage.CACHING, blockPos, url));
                    long sizeLimit = ImageFetcher.INSTANCE.getCache().getSingleImageMaxSize();
                    long imageSize = entity.imageInfo.getSize();
                    if (imageSize > sizeLimit) {
                        e.tryOnError(new RuntimeException(
                                String.format("Image is too big, the image max size limit is: %d, but the image size is %d.",
                                        sizeLimit, imageSize)
                        ));
                    } else {
                        final ImageEntity added = cache.add(entity);
                        if (added.equals(ImageEntity.EMPTY)) {
                            e.tryOnError(new OutOfMemoryError("out of memory of Cache: cache left memory is " +
                                    cache.getLeftMemory() + "the image entry is " + entity.imageInfo.getSize()));
                        } else {
                            LOGGER.info("Cached image: {}", url);
                            e.onNext(entity);
                            e.onComplete();
                        }
                    }
                } else {
                    e.tryOnError(new RuntimeException("Unknown Error: download success but the file can not load!"));
                }
            }
            e.tryOnError(new RuntimeException("Download failed, please check the Url of the file: " + url));

        }).subscribeOn(Schedulers.io());
    }

    private ImageLoader.ImageData loadFromDisk(Path path) throws Exception {
        if (Files.exists(path)) {
            return loader.load(path);
        }

        return null;
    }

    private void deleteTempFile(String url) throws IOException {
        String name = encodeUrl(url);

        Path diskPath = Paths.get(config.defaultDiskSavePath, config.orgImageSaveDir, name);

        Files.delete(diskPath);
    }

    //To reduce the hash conflicts, we only encode the url which is longer than 128.
    public String encodeUrl(String url) {
        String encoded = Utils.urlEncode(url);
        if (encoded.length() > 128) {
            return "scr0" + Utils.md5(url);
        }
        return "scr1" + encoded;
    }

    public void sendEvent(FetchInfo fetchInfo){
        for(Consumer<IEvent> iEventConsumer: stageListener){
            iEventConsumer.accept(fetchInfo);
        }
    }

    public void addListener(Consumer<IEvent> eventConsumer){
        stageListener.add(eventConsumer);
    }

    public Consumer<IEvent> removeListener(Consumer<IEvent> eventConsumer){
        stageListener.remove(eventConsumer);
        return eventConsumer;
    }

    public void clearListeners(){
        stageListener.clear();
    }

    public void setConfig(CacheSetting config) {
        this.config = config;
    }

    public void setLoader(ImageLoader loader) {
        this.loader = loader;
    }

    public void addToBlackList(String url) {
        blackUrls.add(url);
    }

    public void removeFromBlackList(String url) {
        blackUrls.remove(url);
    }

    public void refresh(long pos) {
        blackUrls.clear();
        ImageEntity entity = cache.findByPos(pos);
        if (entity != null && entity != ImageEntity.EMPTY) {
            clear(entity.url, true);
        }
    }

    public void refreshSmooth(long pos) {
        ImageEntity entity = cache.findByPos(pos);
        if (entity != null && entity != ImageEntity.EMPTY) {
            reloadImageSmooth(entity.url, pos);
        }
    }

    public void removeByPos(long pos) {
        cache.removeEntityByPos(pos);
    }

    public boolean isInBlackList(String url) {
        return blackUrls.contains(url);
    }


    public void onTick(Vector3d vector3d) {
        if (isInit) cache.updateViewerPos(vector3d);
    }

    public void setCacheListener(ImageEntityCache.CacheChangeListener listener) {
        cache.setCacheChangeListener(listener);
    }

    public static class CacheSetting {
        public int cacheMaxSize;
        public long cacheMemoryLimit;
        public String defaultDiskSavePath;
        public String orgImageSaveDir;
        public String thumbnailSaveDir;
        public long imageMaxSize;

        private static final long DEFAULT_IMAGE_MAX_SIZE = 4L << 20;
        private static final int DEFAULT_CACHE_MAX_SIZE = 100;
        private static final long DEFAULT_CACHE_MEMORY_LIMIT = 200L << 20;

        public CacheSetting(String defaultDiskSavePath, String orgImageSaveDir, String thumbnailSaveDir) {
            this(DEFAULT_CACHE_MAX_SIZE, DEFAULT_CACHE_MEMORY_LIMIT, DEFAULT_IMAGE_MAX_SIZE, defaultDiskSavePath, orgImageSaveDir, thumbnailSaveDir);
            //Test small cache
            //Test small memory limit
//            this(2, 2 << 20, defaultDiskSavePath, orgImageSaveDir, thumbnailSaveDir, DEFAULT_IMAGE_MAX_SIZE);
        }

        public CacheSetting(int cacheMaxSize, long cacheMemoryLimit, long imageMaxSize, String defaultDiskSavePath, String orgImageSaveDir, String thumbnailSaveDir) {
            this.cacheMaxSize = cacheMaxSize;
            this.cacheMemoryLimit = cacheMemoryLimit;
            this.defaultDiskSavePath = defaultDiskSavePath;
            this.orgImageSaveDir = orgImageSaveDir;
            this.thumbnailSaveDir = thumbnailSaveDir;
            this.imageMaxSize = imageMaxSize;
        }

        public CacheSetting(int cacheMaxSize, long cacheMemoryLimit, String defaultDiskSavePath, String orgImageSaveDir, String thumbnailSaveDir) {
            this.cacheMaxSize = cacheMaxSize;
            this.cacheMemoryLimit = cacheMemoryLimit;
            this.defaultDiskSavePath = defaultDiskSavePath;
            this.orgImageSaveDir = orgImageSaveDir;
            this.thumbnailSaveDir = thumbnailSaveDir;
        }

        @Override
        public String toString() {
            return "SaveConfig{" +
                    ", imageMaxSize=" + imageMaxSize +
                    ", cacheMaxSize=" + cacheMaxSize +
                    ", cacheMemoryLimit=" + cacheMemoryLimit +
                    ", defaultDiskSavePath='" + defaultDiskSavePath + '\'' +
                    ", orgImageSaveDir='" + orgImageSaveDir + '\'' +
                    ", thumbnailSaveDir='" + thumbnailSaveDir + '\'' +
                    '}';
        }
    }

}


