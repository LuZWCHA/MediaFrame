package top.nowandfuture.mod.imagesign.caches;

import com.google.common.collect.Lists;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.nowandfuture.mod.imagesign.loader.ImageLoader;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.glIsTexture;

public class ImageEntity {
    private static final Logger LOGGER = LogManager.getLogger(ImageEntity.class);
    private static final String EMPTY_URL = "404";
    public static final ImageEntity EMPTY = new ImageEntity();

    public String url;

    //for gif the image number may be more than one
    private List<OpenGLImage> orgImages;
    public ImageLoader.ImageInfo imageInfo;

    public List<BlockPos> posList;

    public CompositeDisposable uploadDisposables;

    private ImageEntity() {
        this(EMPTY_URL, BlockPos.ZERO);
    }

    private ImageEntity(String url, BlockPos posList, OpenGLImage... images) {
        this.orgImages = Lists.newArrayList(images);
        this.url = url;
        this.posList = new ArrayList<>();
        this.posList.add(posList);
        this.uploadDisposables = new CompositeDisposable();
    }

    public BlockPos getFirstPos() {
        return posList.get(0);
    }

    public static ImageEntity create(String url, BlockPos pos) {
        return new ImageEntity(url, pos);
    }

    public static ImageEntity create(String url, BlockPos pos, OpenGLImage... images) {
        return new ImageEntity(url, pos, images);
    }

    public static ImageEntity create(String url, BlockPos pos, BufferedImage... images) {
        OpenGLImage[] openGLImages = new OpenGLImage[images.length];
        for (int i = 0; i < openGLImages.length; i++) {
            openGLImages[i] = new OpenGLImage(images[i]);
        }

        return new ImageEntity(url, pos, openGLImages);
    }

    public static ImageEntity create(String url, BlockPos pos, ImageLoader.ImageData data) {
        return create(url, pos, data.getImages());
    }

    public void merge(String url, BlockPos pos) {
        if (url.equals(this.url)) {
            if (!this.posList.contains(pos)) {
                this.posList.add(pos);
            }
        }
    }

    public void setImageInfo(ImageLoader.ImageInfo imageInfo) {
        this.imageInfo = imageInfo;
    }

    public void uploadImage() {
        uploadImage(false, 1);
    }

    public void uploadImage(boolean thumbnail, float scale) {
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
                    LOGGER.info("Uploaded image {} {}.", url, aBoolean ? "successful" : "failed");
                    uploadDisposables.delete(disposable);
                    if (aBoolean && !glIsTexture(openGLImage.getGlTextureId())) {
                        openGLImage.markUpdate();
                    }
                }

                @Override
                public void onError(@NonNull Throwable e) {
                    LOGGER.info("Uploaded image {} {}.", url, "failed");
                    uploadDisposables.delete(disposable);
                }
            });
        }
    }

    public boolean hasOpenGLSource() {
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

    public List<OpenGLImage> getOrgImages() {
        return orgImages;
    }

    public synchronized void dispose() {
        uploadDisposables.dispose();
        for (OpenGLImage openGLImage : orgImages) {
            openGLImage.dispose();
        }
        uploadDisposables = new CompositeDisposable();
    }

    public void markUpdate() {
        for (OpenGLImage openGLImage : orgImages) {
            openGLImage.markUpdate();
        }
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
