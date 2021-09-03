package top.nowandfuture.mod.imagesign.caches;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.schedulers.Schedulers;
import net.minecraft.client.renderer.texture.Texture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.resources.IResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;
import top.nowandfuture.mod.imagesign.schedulers.MyWorldRenderScheduler;
import top.nowandfuture.mod.imagesign.utils.ImageUtils;

import java.awt.image.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import static org.lwjgl.opengl.GL11.*;

//The methods in should be called in Render Thread.
public class OpenGLImage extends Texture {
    private static final Logger LOGGER = LogManager.getLogger(OpenGLImage.class);
//    private static final  MemoryStack MEMORY_STACK;
//
//    static {
//        MEMORY_STACK = MemoryStack.create(20 << 20);
//    }

    private final ReentrantLock lock = new ReentrantLock();
    private BufferedImage bufferedImage;
    private boolean updThumbnail;
    private BufferedImage thum;
    private float scale;
    private final AtomicBoolean updated;
    private int w, h;


    public OpenGLImage(BufferedImage bufferedImage) {
        this.bufferedImage = bufferedImage;
        this.glTextureId = -1;
        this.scale = 1;
        this.updated = new AtomicBoolean(false);
        this.w = bufferedImage.getWidth();
        this.h = bufferedImage.getHeight();
    }

    @Deprecated
    public void markUpdate() {
        this.updated.set(false);
    }

    public void setImageData(@NonNull BufferedImage bufferedImage){
        this.lock.lock();
        this.bufferedImage = bufferedImage;
        this.lock.unlock();
    }

    public BufferedImage getImageData(){
        return this.bufferedImage;
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
        this.lock.lock();
        if (bufferedImage != null) {
            Observable.just(bufferedImage)
                    .observeOn(Schedulers.io())
                    .map(image -> {
                        int channel = 3;
                        if (updThumbnail && scale != 1) {
                            image = ImageUtils.scale(image, scale, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
                        }

                        if (image.getType() != BufferedImage.TYPE_3BYTE_BGR || image.getType() != BufferedImage.TYPE_4BYTE_ABGR || image.getType() != BufferedImage.TYPE_CUSTOM) {
                            if (image.getColorModel().hasAlpha()) {
                                image = ImageUtils.convert2RGBA(image);
                            } else {
                                image = ImageUtils.convert2RGB(image);
                            }
                        }

                        if (image.getColorModel().hasAlpha()) {
                            channel = 4;
                        }
                        Raster raster = image.getRaster();
                        DataBuffer dataBuffer = raster.getDataBuffer();
                        byte[] data = null;
                        if(dataBuffer instanceof DataBufferByte){
                            data = ((DataBufferByte) dataBuffer).getData();
                        }

                        return ImageUpload.create(data, image.getWidth(), image.getHeight(), channel);
                    })
                    .observeOn(MyWorldRenderScheduler.mainThread())
                    .map(image -> uploadImageInner(image.data, image.w, image.h, image.channel, deleteMemory))
                    .singleOrError()
                    .doOnError(throwable -> markUpdate())
                    .doOnDispose(() -> LOGGER.info("Canceled data upload to GPU."))
                    .subscribe(listener);
        }
        this.lock.unlock();
    }

    public boolean uploadImageInner(byte[] data, int width, int height, int channel, boolean deleteMemory) {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
        if(data == null) return false;
//        int mipmapLevel = Minecraft.getInstance().gameSettings.mipmapLevels;
        int mipmapLevel = 1;
        int tempId = glTextureId;
        if(tempId == -1) {
            tempId = TextureUtil.generateTextureId();
        }

        int error = GL11.glGetError();
        if (error != GL_NO_ERROR) {
            return false;
        }

        if (tempId != -1) {
            ByteBuffer byteBuffer = null;
            try {
                byteBuffer = MemoryUtil.memAlloc(data.length);
                byteBuffer.put(data).flip();

                if (byteBuffer.limit() > 0) {
                    GlStateManager.bindTexture(tempId);
                    if (mipmapLevel >= 0) {
                        GlStateManager.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
                        GlStateManager.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
//                        GlStateManager.texParameter(GL_TEXTURE_2D, GL_GENERATE_MIPMAP, GL_TRUE);
                        GlStateManager.texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
                        GlStateManager.texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
//                        GlStateManager.texParameter(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
//                        GlStateManager.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, mipmapLevel);
//                        GlStateManager.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_LOD, 0.0F);
//                        GlStateManager.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAX_LOD, mipmapLevel);
//                        GlStateManager.texParameter(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, 0.0F);
                        GlStateManager.pixelStore(GL_UNPACK_SKIP_PIXELS, 0);
                        GlStateManager.pixelStore(GL_UNPACK_SKIP_ROWS, 0);
                    }

                    error = GL11.glGetError();
                    if (error != GL_NO_ERROR) {
                        return false;
                    }

                    for (int i = 0; i < mipmapLevel; i++) {
                        //Send texel data to OpenGL
                        if (channel == 4) {
                            GL11.glTexImage2D(GL_TEXTURE_2D, i, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) byteBuffer);
//                            GL11.glTexSubImage2D(GL_TEXTURE_2D, i, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, byteBuffer);
                        } else if (channel == 3) {
                            GL11.glTexImage2D(GL_TEXTURE_2D, i, GL_RGB8, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, (ByteBuffer) byteBuffer);
//                            GL11.glTexSubImage2D(GL_TEXTURE_2D, i, 0, 0, width, height, GL_RGB, GL_UNSIGNED_BYTE, byteBuffer);
                        }
                    }

                    error = GL11.glGetError();
                    if (error != GL_NO_ERROR) {
                        return false;
                    }
                }
            } finally {
                MemoryUtil.memFree(byteBuffer);
                int oldGlTextureId = this.glTextureId;
                this.glTextureId = tempId;
                w = width;
                h = height;
                updated.set(true);

                if (oldGlTextureId != -1 && oldGlTextureId != this.glTextureId) {
                    TextureUtil.releaseTextureId(oldGlTextureId);
                }

            }

            error = GL11.glGetError();
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

    public void setUseThumbnail(boolean v, float scale) {
        this.updThumbnail = v;
        this.scale = scale;
    }

    public synchronized void disposeGLSource() {
        if (glTextureId != -1) {
            deleteGlTexture();
            glTextureId = -1;
            updated.set(false);
        }
    }

    public void dispose() {
        if (RenderSystem.isOnRenderThreadOrInit()) {
            disposeGLSource();
            if (bufferedImage != null) {
                bufferedImage.flush();
                bufferedImage = null;
            }
            LOGGER.info("Disposed the image.");
        } else {
            RenderSystem.recordRenderCall(this::dispose);
        }
    }

    @Override
    public void close() {
        this.dispose();
        LOGGER.info("Close Image.");
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
        return w;
    }

    public int getHeight() {
        return h;
    }

    public float getScale() {
        return scale;
    }

    public boolean isThumbnail() {
        return updThumbnail;
    }

    public boolean hasGLSourcePrepared(){
        return this.glTextureId != -1 && updated.get();
    }
}
