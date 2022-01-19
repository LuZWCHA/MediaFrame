package top.nowandfuture.mod.imagesign.caches;

import com.google.common.collect.Lists;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.nowandfuture.mod.imagesign.loader.ImageLoader;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.lwjgl.opengl.GL11.glIsTexture;

//Thread Safe
public class ImageEntity {
    private static final Logger LOGGER = LogManager.getLogger(ImageEntity.class);
    private static final String EMPTY_URL = "404";
    public static final ImageEntity EMPTY = new ImageEntity();

    public String url;

    //for gif the image number may be more than one
    private List<OpenGLImage> orgImages;
    public ImageLoader.ImageInfo imageInfo;

    public LongList posList;

    public CompositeDisposable uploadDisposables;

    private ImageEntity() {
        this(EMPTY_URL, new LongArrayList());
    }

    private ImageEntity(String url, long pos, OpenGLImage... images){
        this(url, singleLongList(pos), images);
    }

    private static LongList singleLongList(long e){
        LongList list = new LongArrayList();
        list.add(e);
        return list;
    }

    private ImageEntity(String url, LongList posList, OpenGLImage... images) {
        this.orgImages = Lists.newArrayList(images);
        this.url = url;
        this.posList = posList;
        this.uploadDisposables = new CompositeDisposable();
    }

    public long getFirstID() {
        return posList.getLong(0);
    }

    public static ImageEntity create(String url, long pos) {
        return new ImageEntity(url, pos);
    }

    public static ImageEntity create(String url, long pos, OpenGLImage... images) {
        return new ImageEntity(url, pos, images);
    }

    public static ImageEntity create(String url, long pos, BufferedImage... images) {
        OpenGLImage[] openGLImages = new OpenGLImage[images.length];
        for (int i = 0; i < openGLImages.length; i++) {
            openGLImages[i] = new OpenGLImage(images[i]);
        }

        return new ImageEntity(url, pos, openGLImages);
    }

    public static ImageEntity create(String url, long pos, ImageLoader.ImageData data) {
        return create(url, pos, data.getImages());
    }

    public synchronized void merge(String url, long pos) {
        if (url.equals(this.url)) {
            if (!this.posList.contains(pos)) {
                this.posList.add(pos);
            }
        }
    }

    public synchronized void refreshImagesData(ImageEntity entity){
        List<OpenGLImage> images = entity.getOrgImages();
        //If the images can be mapped one by one.
        if(images.size() == orgImages.size()){
            for (int i = 0; i < images.size(); i++) {
                orgImages.get(i).setImageData(images.get(i).getImageData());
            }
        }else{
            orgImages = images;
        }
        imageInfo = entity.imageInfo;
        //To upload the data and replace the old one.
        updateFailCount = 0;
        setToUpdate(true);
    }

    public synchronized void setImageInfo(ImageLoader.ImageInfo imageInfo) {
        this.imageInfo = imageInfo;
    }

    private int updateFailCount = 0;
    private static int MAX_FAILED_COUNT = 3;

    public void uploadImage() {
        uploadImage(false, 1);
    }

    public synchronized void uploadImage(boolean thumbnail, float scale) {
        for (OpenGLImage openGLImage : orgImages) {
            openGLImage.setUseThumbnail(thumbnail, scale);
            openGLImage.uploadImage(new SingleObserver<Boolean>() {
                private Disposable disposable;

                @Override
                public void onSubscribe(@NonNull Disposable d) {
                    disposable = d;
                    uploadDisposables.add(d);
                    LOGGER.info("Uploading image: {}", url);
                }

                @Override
                public void onSuccess(@NonNull Boolean aBoolean) {
                    LOGGER.info("Upload image {} {}.", url, aBoolean ? "successful" : "failed");
                    uploadDisposables.delete(disposable);
//                    if (aBoolean && !glIsTexture(openGLImage.getGlTextureId())) {
//                        setToUpdate(true);
//                    }else if(!aBoolean){
//                        setToUpdate(true);
//                    }
                    if(updateFailCount < MAX_FAILED_COUNT * getOrgImages().size() && !aBoolean || !glIsTexture(openGLImage.getGlTextureId())){
                        toUpdate.set(true);
                        updateFailCount ++;
                    }
                }

                @Override
                public void onError(@NonNull Throwable e) {
                    LOGGER.info("Upload image {} failed.", url);
                    uploadDisposables.delete(disposable);
                    if(updateFailCount < MAX_FAILED_COUNT * getOrgImages().size()) {
                        toUpdate.set(true);
                        updateFailCount ++;
                    }
                }
            });
        }
    }

    public synchronized boolean hasOpenGLSource() {
        for (OpenGLImage openGLImage : orgImages) {
            if (!openGLImage.hasGLSourcePrepared()) {
                return false;
            }
        }
        return !orgImages.isEmpty();
    }

    public synchronized boolean isUploading() {
        return uploadDisposables.size() > 0;
    }

    public synchronized List<OpenGLImage> getOrgImages() {
        return orgImages;
    }

    public synchronized void dispose() {
        uploadDisposables.dispose();
        for (OpenGLImage openGLImage : orgImages) {
            openGLImage.dispose();
        }
        uploadDisposables = new CompositeDisposable();
        updateFailCount = 0;
    }

    @Deprecated
    public synchronized void markUpdated() {
        for (OpenGLImage openGLImage : orgImages) {
            openGLImage.markUpdate();
        }
    }

    private final AtomicBoolean toUpdate = new AtomicBoolean(true);

    public void setToUpdate(boolean v){
        toUpdate.set(v);
    }

    public boolean isToUpdate(){
        return toUpdate.get();
    }

    public synchronized void disposeGLSource() {
        if (!uploadDisposables.isDisposed() && uploadDisposables.size() > 0) {
            uploadDisposables.dispose();
        }

        if (uploadDisposables.isDisposed()) {
            uploadDisposables = new CompositeDisposable();
        }

        for (OpenGLImage openGLImage : orgImages) {
            openGLImage.disposeGLSource();
        }
    }
}
