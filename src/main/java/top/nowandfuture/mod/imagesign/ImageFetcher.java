package top.nowandfuture.mod.imagesign;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.vector.Vector3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.nowandfuture.mod.imagesign.utils.Utils;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import net.minecraft.util.math.BlockPos;

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
    private final DistanceList list;

    private ImageLoader loader;
    private final Logger logger;

    private Set<String> blackUrls;

    ImageFetcher(){
        //default config
        this(new SaveConfig(100, 100 * 1024 * 1024,
                Minecraft.getInstance().gameDir.getAbsolutePath(), "image_temps","image_temps2"));
    }

    ImageFetcher(@NonNull SaveConfig config){
        logger =  LoggerFactory.getLogger(getClass());
        list = new DistanceList(config.cacheMaxSize, config.cacheMemoryLimit);
        //default image loader
        loader = new ImageIOLoader();
        blackUrls = new HashSet<>();

        logger.debug("Initializing image fetcher..., the config setting is: {}",config);
    }

    public DistanceList getCache() {
        return list;
    }

    public void setSavePath(String path, String dirName){
        config.defaultDiskSavePath = path;
        config.orgImageSaveDir = dirName;
    }

    public ImageEntity grabImage(String url){
        return checkCache(url).blockingFirst(ImageEntity.EMPTY);
    }

    public synchronized void clear(String url, boolean deleteFile) {
        if(list.contain(url)){
            list.remove(url);
        }

        blackUrls.remove(url);

        if(deleteFile){
            try {
                deleteTempFile(url);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
                        logger.debug("Observer stream: ", throwable);
                    }
                });
    }

    private Observable<ImageEntity> checkCache(String url){
        return Observable.create(new ObservableOnSubscribe<ImageEntity>() {
            @Override
            public void subscribe(ObservableEmitter<ImageEntity> e) throws Exception {
                if(getCache().contain(url)) {
                    logger.debug("Find the image: {}", url);
                    e.onNext(getCache().get(url));
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
                ImageLoader.ImageData data = loadFromDisk(diskPath);
                if (data != null) {
                    logger.debug("Loading image: {}", url);
                    ImageEntity entity = ImageEntity.create(url, blockPos, data);
                    entity.setImageInfo(data.getImageInfo());
                    logger.debug("Caching image: {}", url);
                    ImageEntity added = list.add(entity);
                    if(added.equals(ImageEntity.EMPTY)){
                        entity.dispose();
                        e.tryOnError(
                                new OutOfMemoryError(
                                        String.format("Out of memory of Cache: cache left memory is %d bytes, " +
                                                        "but the object entry's size is %d bytes.",
                                                list.getLeftMemory(), entity.imageInfo.getSize())));
                        return;
                    }else{
                        logger.debug("Cached image: {}", url);
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
                logger.debug("Downloading image: {}", url);
                File file = loader.fetch(url, diskPath.toFile());
                if (file != null) {
                    logger.debug("Loading image: {}", url);
                    ImageLoader.ImageData data = loadFromDisk(diskPath);
                    if(data != null) {
                        ImageEntity entity = ImageEntity.create(url, blockPos, data);
                        entity.setImageInfo(data.getImageInfo());
                        logger.debug("Caching image: {}", url);
                        ImageEntity added = list.add(entity);
                        if(added.equals(ImageEntity.EMPTY)){
                            entity.dispose();
                            e.tryOnError(new OutOfMemoryError("out of memory of Cache: cache left memory is " +
                                    list.getLeftMemory() + "the image entry is " + entity.imageInfo.getSize()));
                        }else{
                            logger.debug("Cached image: {}", url);
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

    public boolean isInBlackList(String url){
        return blackUrls.contains(url);
    }

    public void onTick(Vector3d vector3d){
        list.updateViewerPos(vector3d);
    }

    public static class SaveConfig{
        public int cacheMaxSize;
        public long cacheMemoryLimit;
        public String defaultDiskSavePath;
        public String orgImageSaveDir;
        public String thumbnailSaveDir;

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
                    "defaultDiskSavePath='" + defaultDiskSavePath + '\'' +
                    ", orgImageSaveDir='" + orgImageSaveDir + '\'' +
                    ", thumbnailSaveDir='" + thumbnailSaveDir + '\'' +
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


