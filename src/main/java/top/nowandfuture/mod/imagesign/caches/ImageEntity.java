package top.nowandfuture.mod.imagesign.caches;

import com.google.common.collect.Lists;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import net.minecraft.util.math.BlockPos;
import top.nowandfuture.mod.imagesign.loader.ImageLoader;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.glIsTexture;

public class ImageEntity {
    private static final String EMPTY_URL = "404";
    public static final ImageEntity EMPTY = new ImageEntity();

    public String url;

    //for gif the image number may be more than one
    private List<OpenGLImage> orgImages;
    public ImageLoader.ImageInfo imageInfo;

    public List<BlockPos> posList;

    public CompositeDisposable uploadDisposables;

    private ImageEntity(){
        this(EMPTY_URL, BlockPos.ZERO);
    }

    private ImageEntity(String url, BlockPos posList, OpenGLImage ... images){
        this.orgImages = Lists.newArrayList(images);
        this.url = url;
        this.posList = new ArrayList<>();
        this.posList.add(posList);
        this.uploadDisposables = new CompositeDisposable();
    }

    public BlockPos getFirstPos() {
        return posList.get(0);
    }

    public static ImageEntity create(String url, BlockPos pos){
        return new ImageEntity(url, pos);
    }

    public static ImageEntity create(String url, BlockPos pos, OpenGLImage ... images){
        return new ImageEntity(url, pos, images);
    }

    public static ImageEntity create(String url, BlockPos pos, BufferedImage... images){
        OpenGLImage[] openGLImages = new OpenGLImage[images.length];
        for (int i = 0; i < openGLImages.length; i++) {
            openGLImages[i] = new OpenGLImage(images[i]);
        }

        return new ImageEntity(url, pos, openGLImages);
    }

    public static ImageEntity create(String url, BlockPos pos, ImageLoader.ImageData data){
        return create(url, pos, data.getImages());
    }

    public void merge(String url, BlockPos pos){
        if(url.equals(this.url)){
            if(!this.posList.contains(pos)) {
                this.posList.add(pos);
            }
        }
    }

    public void setImageInfo(ImageLoader.ImageInfo imageInfo) {
        this.imageInfo = imageInfo;
    }

    public void uploadImage(){
        uploadImage(false);
    }

    public void uploadImage(boolean thumbnail){
        for (OpenGLImage openGLImage : orgImages) {
            openGLImage.setUseThumbnail(thumbnail);
            openGLImage.uploadImage(new SingleObserver<Boolean>() {
                private Disposable disposable;

                @Override
                public void onSubscribe(@NonNull Disposable d) {
                    disposable = d;
                    uploadDisposables.add(d);
                    System.out.println("uploading image:" + url);
                }

                @Override
                public void onSuccess(@NonNull Boolean aBoolean) {
                    System.out.println("uploaded image:" + url + aBoolean);
                    uploadDisposables.delete(disposable);
                    if(aBoolean && !glIsTexture(openGLImage.getGlTextureId())) {
                        openGLImage.markUpdate();
                    }
                }

                @Override
                public void onError(@NonNull Throwable e) {
                    uploadDisposables.remove(disposable);
                }
            });
        }
    }

    public boolean hasOpenGLSource(){
        for (OpenGLImage openGLImage : orgImages) {
            if(!openGLImage.hasGLSourcePrepared()){
                return false;
            }
        }
        return !orgImages.isEmpty();
    }

    public boolean isUploading(){
        return uploadDisposables.size() > 0;
    }

    public List<OpenGLImage> getOrgImages() {
        return orgImages;
    }

    public void dispose(){
        System.out.println("dispose: " + url);
        for (OpenGLImage openGLImage : orgImages) {
            openGLImage.dispose();
        }
        posList.clear();
        uploadDisposables.dispose();
        uploadDisposables = new CompositeDisposable();
    }

    public void markUpdate(){
        for (OpenGLImage openGLImage : orgImages) {
            openGLImage.markUpdate();
        }
    }

    public void disposeGLSource(){
        if(!uploadDisposables.isDisposed() && uploadDisposables.size() > 0) {
            uploadDisposables.dispose();
        }

        if(uploadDisposables.isDisposed()){
            uploadDisposables = new CompositeDisposable();
        }

        for (OpenGLImage openGLImage : orgImages) {
            openGLImage.disposeGLSource();
        }
    }


}
