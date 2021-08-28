package top.nowandfuture.mod.imagesign.loader;

import com.mojang.blaze3d.systems.IRenderCall;
import com.mojang.blaze3d.systems.RenderSystem;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private SaveConfig config;
    private final ImageEntityCache list;

    private ImageLoader loader;
    private final Logger logger;

    private final Set<String> blackUrls;

    ImageFetcher(){
        //default config
        this(new SaveConfig(Minecraft.getInstance().gameDir.getAbsolutePath(), "image_temps","image_temps2"));
    }

    ImageFetcher(@NonNull SaveConfig config){
        this.config = config;
        this.logger =  LoggerFactory.getLogger(getClass());
        this.list = new ImageEntityCache(config.cacheMaxSize, config.imageMaxSize, config.cacheMemoryLimit);
        //default image loader
        this.loader = new ImageIOLoader();
        this.blackUrls = new HashSet<>();

        this.logger.info("Initializing image fetcher..., the config setting is: {}",config);
    }

    public ImageEntityCache getCache() {
        return list;
    }

    public void setSavePath(String path, String dirName){
        config.defaultDiskSavePath = path;
        config.orgImageSaveDir = dirName;
    }

    public ImageEntity grabImage(String url, BlockPos pos){
        return checkCache(url, pos).blockingFirst(ImageEntity.EMPTY);
    }

    public void dispose(){
        if(RenderSystem.isOnRenderThreadOrInit()){
            list.dispose();
            blackUrls.clear();
        }else{
            RenderSystem.recordRenderCall(new IRenderCall() {
                @Override
                public void execute() {
                    dispose();
                }
            });
        }

    }

    public void reload(){
        logger.info("Reload GLSources: {} images to reload.", list.size());
        list.markUpdate();
    }

    public void clear(String url, boolean deleteFile) {
        if(list.contain(url)){
            if(deleteFile){
                try {
                    deleteTempFile(url);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            list.removeImage(url);
        }

        blackUrls.remove(url);
    }

    public @NonNull Observable<ImageEntity> get(String url, BlockPos blockPos, Scheduler curSch){
        //fetch from cache
        return Observable.concat(this.load(url, blockPos), this.fetch(url, blockPos))
                .first(ImageEntity.EMPTY)
                .observeOn(curSch)
                .toObservable()
                .doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Throwable {
                        if(throwable instanceof OutOfMemoryError){
                            logger.debug("check the config to expand the memory: ", throwable);
                        }else {
                            logger.info("Observer stream: ", throwable);
                        }
                    }
                });
    }

    private Observable<ImageEntity> checkCache(String url, BlockPos pos){
        return Observable.create(new ObservableOnSubscribe<ImageEntity>() {
            @Override
            public void subscribe(ObservableEmitter<ImageEntity> e) throws Exception {
                if(getCache().contain(url)) {
                    logger.debug("Find the image: {}", url);
                    ImageEntity entity = getCache().get(url);
                    entity.merge(url, pos);
                    e.onNext(entity);
                }
                e.onComplete();
            }
        });
    }

    private Observable<ImageEntity> load(String url, BlockPos blockPos){
        return Observable.create(new ObservableOnSubscribe<ImageEntity>() {
            @Override
            public void subscribe(ObservableEmitter<ImageEntity> e) throws Exception {
                String name = encodeUrl(url);
                Path diskPath = Paths.get(config.defaultDiskSavePath, config.orgImageSaveDir, name);
                logger.info("Loading image: {}", url);
                ImageLoader.ImageData data = loadFromDisk(diskPath);
                if (data != null) {
                    ImageEntity entity = ImageEntity.create(url, blockPos, data);
                    logger.info("Image loaded: {}", url);

                    entity.setImageInfo(data.getImageInfo());
                    logger.info("Caching image: {}", url);
                    ImageEntity added = list.add(entity);
                    if(added.equals(ImageEntity.EMPTY)){
                        e.tryOnError(
                                new OutOfMemoryError(
                                        String.format("Out of memory of Cache: cache left memory is %d bytes, " +
                                                        "but the object entry's size is %d bytes.",
                                                list.getLeftMemory(), entity.imageInfo.getSize())));
                    }else{
                        logger.info("Cached image: {}", url);
                        e.onNext(entity);
                    }
                }
                e.onComplete();
            }

        }).subscribeOn(Schedulers.io());
    }

    private Observable<ImageEntity> fetch(String url, BlockPos blockPos){
        return Observable.create(new ObservableOnSubscribe<ImageEntity>() {
            @Override
            public void subscribe(ObservableEmitter<ImageEntity> e) throws Exception {
                String name = encodeUrl(url);
                Path diskPath = Paths.get(config.defaultDiskSavePath, config.orgImageSaveDir, name);
                File parentDir = diskPath.toFile().getParentFile();
                if(!parentDir.exists()){
                    Files.createDirectories(parentDir.toPath());
                }
                logger.info("Downloading image: {}", url);
                File file = loader.fetch(url, diskPath.toFile());
                if (file != null) {
                    logger.info("Loading image: {}", url);
                    ImageLoader.ImageData data = loadFromDisk(diskPath);
                    if(data != null) {
                        ImageEntity entity = ImageEntity.create(url, blockPos, data);
                        entity.setImageInfo(data.getImageInfo());
                        logger.info("Caching image: {}", url);
                        ImageEntity added = list.add(entity);
                        if(added.equals(ImageEntity.EMPTY)){
                            e.tryOnError(new OutOfMemoryError("out of memory of Cache: cache left memory is " +
                                    list.getLeftMemory() + "the image entry is " + entity.imageInfo.getSize()));
                        }else{
                            logger.info("Cached image: {}", url);
                            e.onNext(entity);
                            e.onComplete();
                        }
                    }else{
                        e.tryOnError(new RuntimeException("Unknown Error: download success but the file can not load!"));
                    }
                }
                e.tryOnError(new RuntimeException("Download failed, please check the Url of the file: " + url));

            }
        }).subscribeOn(Schedulers.io());
    }

    private ImageLoader.ImageData loadFromDisk(Path path) throws Exception {
        if(Files.exists(path)){
            return loader.load(path);
        }

        return null;
    }

    private void deleteTempFile(String url) throws IOException {
        String name = encodeUrl(url);

        Path diskPath = Paths.get(config.defaultDiskSavePath, config.orgImageSaveDir, name);

        Files.delete(diskPath);
    }

    public String encodeUrl(String url){
        String encoded = Utils.urlEncode(url);
        if(encoded.length() > 128){
            return "scr0" + Utils.md5(url);
        }
        return "scr1" + encoded;
    }

    public void setConfig(SaveConfig config) {
        this.config = config;
    }

    public void setLoader(ImageLoader loader) {
        this.loader = loader;
    }

    public void addToBlackList(String url){
        blackUrls.add(url);
    }

    public void removeFromBlackList(String url){
        blackUrls.remove(url);
    }

    public void refresh(BlockPos pos){
        ImageEntity entity = list.findByPos(pos);
        if(entity != null && entity != ImageEntity.EMPTY){
            clear(entity.url, true);
        }
    }

    public void reRender(BlockPos pos){
        ImageEntity entity = list.findByPos(pos);
        if(entity != null && entity != ImageEntity.EMPTY){
            entity.markUpdate();
        }
    }

    public boolean isInBlackList(String url){
        return blackUrls.contains(url);
    }

    public void onTick(Vector3d vector3d){
        list.updateViewerPos(vector3d);
    }

    public void setCacheListener(ImageEntityCache.CacheChangeListener listener){
        list.setCacheChangeListener(listener);
    }

    public static class SaveConfig{
        public int cacheMaxSize;
        public long cacheMemoryLimit;
        public String defaultDiskSavePath;
        public String orgImageSaveDir;
        public String thumbnailSaveDir;
        public long imageMaxSize;

        private static final long DEFAULT_IMAGE_MAX_SIZE = 4L << 20;
        private static final int DEFAULT_CACHE_MAX_SIZE = 100;
        private static final long DEFAULT_CACHE_MEMORY_LIMIT = 200L << 20;

        public SaveConfig(String defaultDiskSavePath, String orgImageSaveDir, String thumbnailSaveDir){
            this(DEFAULT_CACHE_MAX_SIZE, DEFAULT_CACHE_MEMORY_LIMIT, defaultDiskSavePath, orgImageSaveDir, thumbnailSaveDir, DEFAULT_IMAGE_MAX_SIZE);
            //Do test
//            this(2, 2 << 20, defaultDiskSavePath, orgImageSaveDir, thumbnailSaveDir, DEFAULT_IMAGE_MAX_SIZE);
        }

        public SaveConfig(int cacheMaxSize, long cacheMemoryLimit, String defaultDiskSavePath, String orgImageSaveDir, String thumbnailSaveDir, long imageMaxSize) {
            this.cacheMaxSize = cacheMaxSize;
            this.cacheMemoryLimit = cacheMemoryLimit;
            this.defaultDiskSavePath = defaultDiskSavePath;
            this.orgImageSaveDir = orgImageSaveDir;
            this.thumbnailSaveDir = thumbnailSaveDir;
            this.imageMaxSize = imageMaxSize;
        }

        public SaveConfig(int cacheMaxSize, long cacheMemoryLimit, String defaultDiskSavePath, String orgImageSaveDir, String thumbnailSaveDir) {
            this.cacheMaxSize = cacheMaxSize;
            this.cacheMemoryLimit = cacheMemoryLimit;
            this.defaultDiskSavePath = defaultDiskSavePath;
            this.orgImageSaveDir = orgImageSaveDir;
            this.thumbnailSaveDir = thumbnailSaveDir;
        }

        @Override
        public String toString() {
            return "SaveConfig{" +
                    "cacheMaxSize=" + cacheMaxSize +
                    ", cacheMemoryLimit=" + cacheMemoryLimit +
                    ", defaultDiskSavePath='" + defaultDiskSavePath + '\'' +
                    ", orgImageSaveDir='" + orgImageSaveDir + '\'' +
                    ", thumbnailSaveDir='" + thumbnailSaveDir + '\'' +
                    ", imageMaxSize=" + imageMaxSize +
                    '}';
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ImageFetcher.INSTANCE.get("https://pic3.zhimg.com/80/v2-178de5817581ff4f82ef768cb35bfc66_720w.jpg", BlockPos.ZERO, Schedulers.single())
                .blockingSubscribe(new Consumer<ImageEntity>() {
                    @Override
                    public void accept(ImageEntity imageEntity) throws Throwable {
                        System.out.println("success!!!");
                        System.out.println(imageEntity.url);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Throwable {
                        System.out.println("error!!!");
                        System.out.println(throwable.toString());
                    }
                }, new Action() {
                    @Override
                    public void run() throws Throwable {
                        System.out.println("complete!!!");
                    }
                });
        ImageFetcher.INSTANCE.get("https://pic1.zhimg.com/v2-4bba972a094eb1bdc8cbbc55e2bd4ddf_1440w.jpg?source=172ae18b", BlockPos.ZERO, Schedulers.single())
                .blockingSubscribe(new Consumer<ImageEntity>() {
                    @Override
                    public void accept(ImageEntity imageEntity) throws Throwable {
                        System.out.println("success!!!");
                        System.out.println(imageEntity.url);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Throwable {
                        System.out.println("error!!!");
                        System.out.println(throwable.toString());
                    }
                }, new Action() {
                    @Override
                    public void run() throws Throwable {
                        System.out.println("complete!!!");
                    }
                });
        
    }
}


