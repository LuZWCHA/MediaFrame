package top.nowandfuture.mod.imagesign.loader;

import com.mojang.blaze3d.systems.IRenderCall;
import com.mojang.blaze3d.systems.RenderSystem;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.nowandfuture.mod.imagesign.caches.ImageEntity;
import top.nowandfuture.mod.imagesign.caches.ImageEntityCache;
import top.nowandfuture.mod.imagesign.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public enum ImageFetcher {
    INSTANCE;

    private CacheSetting config;

    private ImageEntityCache cache;

    private ImageLoader loader;
    private final Logger LOGGER;

    private final Set<String> blackUrls;
    private boolean isInit = false;

    ImageFetcher() {
        this.LOGGER = LogManager.getLogger(getClass());
        //default image loader
        this.loader = new ImageIOLoader();
        this.blackUrls = new HashSet<>();
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

    public ImageEntity grabImage(String url, BlockPos pos) {
        return checkCache(url, pos).blockingFirst(ImageEntity.EMPTY);
    }

    public void dispose() {
        if (RenderSystem.isOnRenderThreadOrInit()) {
            cache.dispose();
            blackUrls.clear();
        } else {
            RenderSystem.recordRenderCall(new IRenderCall() {
                @Override
                public void execute() {
                    dispose();
                }
            });
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

    public void reloadImageSmooth(String url, BlockPos blockPos) {
        if (cache.contain(url)) {
            try {
                deleteTempFile(url);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ImageLoadTask loadTask = new ImageLoadTask.SignImageLoadTask(blockPos, url);
            ImageLoadManager.INSTANCE.addToLoad(loadTask);
        }
    }

    public @NonNull Observable<ImageEntity> get(String url, BlockPos blockPos, Scheduler curSch) {
        //fetch from cache
        return Observable.concat(this.load(url, blockPos), this.fetch(url, blockPos))
                .first(ImageEntity.EMPTY)
                .observeOn(curSch)
                .toObservable()
                .doOnError(throwable -> {
                    if (throwable instanceof OutOfMemoryError) {
                        LOGGER.warn("Check the config to increase the memory:)");
                    } else {
                        LOGGER.info("Observer stream: ", throwable);
                    }
                });
    }

    private Observable<ImageEntity> checkCache(String url, BlockPos pos) {
        return Observable.create(e -> {
            final ImageEntity queryEntity = getCache().findByPos(pos);
            //If the url changed, remove the position.
            if (queryEntity != ImageEntity.EMPTY && !url.equals(queryEntity.url)) {
                getCache().removeEntity(queryEntity.url, pos.toLong());
            }

            //If find the url, try to merge the position.
            if (getCache().contain(url)) {
                final ImageEntity entity = getCache().get(url);
                entity.merge(url, pos.toLong());
                e.onNext(entity);
            }

            e.onComplete();
        });
    }

    private Observable<ImageEntity> load(String url, BlockPos blockPos) {
        return Observable.create((ObservableOnSubscribe<ImageEntity>) e -> {
            final String name = encodeUrl(url);
            final Path diskPath = Paths.get(config.defaultDiskSavePath, config.orgImageSaveDir, name);
            LOGGER.info("Loading image: {}", url);
            final ImageLoader.ImageData data = loadFromDisk(diskPath);
            if (data != null) {
                final ImageEntity entity = ImageEntity.create(url, blockPos.toLong(), data);
                LOGGER.info("Image loaded: {}", url);

                entity.setImageInfo(data.getImageInfo());
                LOGGER.info("Caching image: {}", url);

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

    private Observable<ImageEntity> fetch(String url, BlockPos blockPos) {
        return Observable.create((ObservableOnSubscribe<ImageEntity>) e -> {
            final String name = encodeUrl(url);
            final Path diskPath = Paths.get(config.defaultDiskSavePath, config.orgImageSaveDir, name);
            final File parentDir = diskPath.toFile().getParentFile();
            if (!parentDir.exists()) {
                Files.createDirectories(parentDir.toPath());
            }
            LOGGER.info("Downloading image: {}", url);
            final File file = loader.fetch(url, diskPath.toFile());
            if (file != null) {
                LOGGER.info("Loading image: {}", url);
                ImageLoader.ImageData data = loadFromDisk(diskPath);
                if (data != null) {
                    final ImageEntity entity = ImageEntity.create(url, blockPos.toLong(), data);
                    entity.setImageInfo(data.getImageInfo());
                    LOGGER.info("Caching image: {}", url);
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

    public String encodeUrl(String url) {
        String encoded = Utils.urlEncode(url);
        if (encoded.length() > 128) {
            return "scr0" + Utils.md5(url);
        }
        return "scr1" + encoded;
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

    public void refresh(BlockPos pos) {
        ImageEntity entity = cache.findByPos(pos);
        if (entity != null && entity != ImageEntity.EMPTY) {
            clear(entity.url, true);
        }
    }

    public void refreshSmooth(BlockPos pos) {
        ImageEntity entity = cache.findByPos(pos);
        if (entity != null && entity != ImageEntity.EMPTY) {
            reloadImageSmooth(entity.url, pos);
        }
    }

    public void removeByPos(BlockPos pos) {
        cache.removeEntityByPos(pos.toLong());
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
                    "cacheMaxSize=" + cacheMaxSize +
                    ", cacheMemoryLimit=" + cacheMemoryLimit +
                    ", defaultDiskSavePath='" + defaultDiskSavePath + '\'' +
                    ", orgImageSaveDir='" + orgImageSaveDir + '\'' +
                    ", thumbnailSaveDir='" + thumbnailSaveDir + '\'' +
                    '}';
        }
    }

}


