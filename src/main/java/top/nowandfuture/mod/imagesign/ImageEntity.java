package top.nowandfuture.mod.imagesign;

import com.google.common.collect.Lists;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import net.minecraft.util.math.BlockPos;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class ImageEntity {
    private static final String EMPTY_URL = "404";
    public static final ImageEntity EMPTY = new ImageEntity();

    public String url;

    List<OpenGLImage> orgImages;
    ImageLoader.ImageInfo imageInfo;

    List<BlockPos> posList;

    CompositeDisposable uploadDisposables;

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
        return create(url, pos, data.images);
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
            openGLImage.uploadImage(uploadDisposables);
        }
    }

    public boolean hasOpenGLSource(){
        for (OpenGLImage openGLImage : orgImages) {
            if(!openGLImage.hasGLSource()){
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

        for (OpenGLImage openGLImage : orgImages) {
            openGLImage.dispose();
        }

        uploadDisposables.dispose();
    }



}
