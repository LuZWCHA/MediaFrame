package top.nowandfuture.mod.imagesign.caches;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.Texture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.resources.IResourceManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.Checks;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import top.nowandfuture.mod.imagesign.loader.ImageFetcher;
import top.nowandfuture.mod.imagesign.schedulers.MyWorldRenderScheduler;
import top.nowandfuture.mod.imagesign.schedulers.OpenGLScheduler;
import top.nowandfuture.mod.imagesign.utils.ImageUtils;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL14.GL_GENERATE_MIPMAP;
import static org.lwjgl.opengl.GL14.GL_TEXTURE_LOD_BIAS;

public class OpenGLImage extends Texture {
    private static final int BYTES_PER_PIXEL = 4;
    private final ReentrantLock lock = new ReentrantLock();
    private BufferedImage bufferedImage;
    private boolean updThumbnail;
    private AtomicBoolean updated;

    public OpenGLImage(BufferedImage bufferedImage) {
        this.bufferedImage = bufferedImage;
        this.glTextureId = -1;
        this.updated = new AtomicBoolean(false);
    }

    public void markUpdate() {
        updated.set(false);
    }

    public void uploadImage(@NonNull SingleObserver<Boolean> listener) {
        uploadImage(listener, true);
    }

    private static class ImageUpload {
        public byte[] data;
        public int w;
        public int h;
        public int channel;

        private ImageUpload() {

        }

        public ImageUpload(byte[] data, int w, int h, int channel) {
            this.data = data;
            this.w = w;
            this.h = h;
            this.channel = channel;
        }

        public static ImageUpload create(byte[] data, int w, int h, int channel) {
            return new ImageUpload(data, w, h, channel);
        }
    }

    public void uploadImage(@NonNull SingleObserver<Boolean> listener, boolean deleteMemory) {
        updated.set(false);
        lock.lock();
        if (bufferedImage != null) {
            Observable.just(bufferedImage)
                    .observeOn(Schedulers.computation())
                    .map(new Function<BufferedImage, ImageUpload>() {
                        @Override
                        public ImageUpload apply(BufferedImage image) throws Throwable {
                            int channel = 3;
                            if (image.getType() != BufferedImage.TYPE_3BYTE_BGR || image.getType() != BufferedImage.TYPE_4BYTE_ABGR || image.getType() != BufferedImage.TYPE_CUSTOM) {
                                if (image.getColorModel().hasAlpha()) {
                                    image = ImageUtils.convert2RGBA(image);
                                } else {
                                    image = ImageUtils.convert2RGB(image);
                                }
                            }
                            if (updThumbnail) {
                                image = ImageUtils.scale(image, .5f);
                            }

                            if (image.getColorModel().hasAlpha()) {
                                channel = 4;
                            }
                            bufferedImage = image;
                            Raster raster = image.getRaster();
                            byte[] data = (byte[]) raster.getDataElements(0, 0, image.getWidth(), image.getHeight(), null);

//                            byte[] data = ((DataBufferByte) raster.getDataBuffer()).getData();
                            return ImageUpload.create(data, image.getWidth(), image.getHeight(), channel);
                        }
                    })
                    .observeOn(MyWorldRenderScheduler.mainThread())
                    .map(new Function<ImageUpload, Boolean>() {
                        @Override
                        public Boolean apply(ImageUpload image) throws Throwable {
//                            Raster raster = bufferedImage.getRaster();
//                            byte[] data = (byte[])raster.getDataElements(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), null);
//
//                            ImageUpload image = ImageUpload.create(data, bufferedImage.getWidth(), bufferedImage.getHeight(), bufferedImage.getColorModel().hasAlpha() ? 4: 3);
                            return uploadImageInner(image.data, image.w, image.h, image.channel, deleteMemory);
                        }
                    })
                    .singleOrError()
                    .doOnSuccess(new Consumer<Boolean>() {
                        @Override
                        public void accept(Boolean aBoolean) throws Throwable {
                            updated.set(aBoolean);
                        }
                    })
                    .doOnError(new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Throwable {
                            throwable.printStackTrace();
                            updated.set(false);
                        }
                    })
                    .doOnDispose(new Action() {
                        @Override
                        public void run() throws Throwable {
                            System.out.println("canceled upload!");
                        }
                    })
                    .subscribe(listener);
        }
        lock.unlock();
    }

    public boolean uploadImageInner(byte[] data, int width, int height, int channel, boolean deleteMemory) {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
        int mipmapLevel = 1;

        if (glTextureId == -1) {
            glTextureId = TextureUtil.generateTextureId();
            int error = GL11.glGetError();
            if (error != GL_NO_ERROR) {
                return false;
            }
        } else {
            if (updated.get()) {
                return false;
            }

            int error = GL11.glGetError();
            if (error != GL_NO_ERROR || glTextureId == -1) {
                glTextureId = -1;
                return false;
            }
        }

        if (glTextureId != -1) {
            lock.lock();

            ByteBuffer byteBuffer = null;
            try {
                byteBuffer = MemoryUtil.memAlloc(data.length);
                byteBuffer.put(data);
                byteBuffer.flip();

                int error = GL11.glGetError();
                if (error != GL_NO_ERROR) {
                    lock.unlock();
                    return false;
                }

                if (byteBuffer.limit() > 0) {
                    bindTexture();
                    if (mipmapLevel >= 0) {
                        GlStateManager.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                        GlStateManager.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
//                        GlStateManager.texParameter(GL_TEXTURE_2D, GL_GENERATE_MIPMAP, GL_TRUE);
                        GlStateManager.texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
                        GlStateManager.texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
//                        GlStateManager.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, mipmapLevel);
//                        GlStateManager.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_LOD, 0.0F);
//                        GlStateManager.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAX_LOD, mipmapLevel);
//                        GlStateManager.texParameter(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, 0.0F);
                    }

                    for (int i = 0; i < mipmapLevel; i++) {
                        //Send texel data to OpenGL
                        if (channel == 4) {
                            GL20.glTexImage2D(GL_TEXTURE_2D, i, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, byteBuffer);
                        } else if (channel == 3) {
                            GL11.glTexImage2D(GL_TEXTURE_2D, i, GL_RGB, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, byteBuffer);
                        }
                    }
                    GlStateManager.bindTexture(0);
                }
            } catch (OutOfMemoryError error) {
                return false;
            } finally {
                MemoryUtil.memFree(byteBuffer);
            }

            lock.unlock();
            int error = GL11.glGetError();

            if (error == GL_NO_ERROR) {
                if (deleteMemory) {
                    // TODO: 2021/8/20 remove the bufferedImage
                }
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public void setUseThumbnail(boolean v) {
        updThumbnail = v;
    }

    public boolean hasGLSourcePrepared() {
        return glTextureId > -1 && glIsTexture(glTextureId) && updated.get();
    }

    public void disposeGLSource() {
        if (glTextureId != -1) {
            deleteGlTexture();
            glTextureId = -1;
            updated.set(false);
        }
    }

    public void dispose() {
        disposeGLSource();
        lock.lock();
        if (bufferedImage != null) {
            bufferedImage.flush();
            bufferedImage = null;
        }
        lock.unlock();
    }

    @Override
    public void close() {
        this.dispose();
        System.out.println("close");
    }

    @Override
    public void deleteGlTexture() {
        super.deleteGlTexture();
    }

    @Override
    public void bindTexture() {
        super.bindTexture();
    }

    public int getGlTextureId() {
        RenderSystem.assertThread(RenderSystem::isOnRenderThreadOrInit);
        return this.glTextureId;
    }

    @Override
    public void loadTexture(IResourceManager manager) throws IOException {
        //do nothing ...
    }

    public int getWidth() {
        return bufferedImage.getWidth();
    }

    public int getHeight() {
        return bufferedImage.getHeight();
    }

}
