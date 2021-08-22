package top.nowandfuture.mod.imagesign;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.Texture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.resources.IResourceManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;
import top.nowandfuture.mod.imagesign.utils.ImageUtils;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;

public class OpenGLImage extends Texture {
    private static final int BYTES_PER_PIXEL = 4;
    private BufferedImage bufferedImage;
    private boolean updThumbnail;

    public OpenGLImage(BufferedImage bufferedImage) {
        this.bufferedImage = bufferedImage;
        this.glTextureId = -1;
    }

    public void uploadImage(CompositeDisposable compositeDisposable) {
        uploadImage(compositeDisposable, true);
    }

    public void uploadImage(CompositeDisposable compositeDisposable, boolean deleteMemory) {
        Observable.just(bufferedImage)
                .observeOn(Schedulers.io())
                .map(new Function<BufferedImage, BufferedImage>() {
                    @Override
                    public BufferedImage apply(BufferedImage image) throws Throwable {
                        if (image.getType() != BufferedImage.TYPE_4BYTE_ABGR || image.getType() != BufferedImage.TYPE_CUSTOM) {
                            image = ImageUtils.convert2RGBA(image);
                        }
                        bufferedImage = image;
                        return image;
                    }
                })
                .subscribe(new Observer<BufferedImage>() {
                    private Disposable disposable;

                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        disposable = d;
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onNext(@NonNull BufferedImage image) {
                        if(RenderSystem.isOnRenderThreadOrInit()){
                            uploadImageInner(deleteMemory);
                        }else{
                            RenderSystem.recordRenderCall(() -> uploadImageInner(deleteMemory));
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        compositeDisposable.remove(disposable);
                    }

                    @Override
                    public void onComplete() {
                        compositeDisposable.remove(disposable);
                    }
                });

    }

    public boolean uploadImageInner(boolean deleteMemory) {
        RenderSystem.assertThread(RenderSystem::isOnRenderThreadOrInit);
        int mipmapLevel = Minecraft.getInstance().gameSettings.mipmapLevels;

        if (glTextureId == -1) {
            glTextureId = TextureUtil.generateTextureId();
        }

        if (glTextureId != -1 && bufferedImage != null) {
            TextureUtil.prepareImage(glTextureId, mipmapLevel, bufferedImage.getWidth(), bufferedImage.getHeight());
            BufferedImage upload = bufferedImage;

            if(updThumbnail){
                upload = ImageUtils.scale(bufferedImage, .5f);
            }

            DataBufferByte buffer = (DataBufferByte) upload.getRaster().getDataBuffer();
            ByteBuffer byteBuffer = BufferUtils.createByteBuffer(upload.getWidth() * upload.getHeight() * BYTES_PER_PIXEL)
                    .put(buffer.getData());

            byteBuffer.flip();

            for(int i = 0; i < mipmapLevel; i++) {
                //Send texel data to OpenGL
                GlStateManager.texSubImage2D(GL_TEXTURE_2D, i, 0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(),
                        GL_RGBA, GL_UNSIGNED_BYTE, MemoryUtil.memAddress(byteBuffer));
            }



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

    public void setUseThumbnail(boolean v){
        updThumbnail = v;
    }

    public boolean hasGLSource(){
        return glTextureId > -1;
    }

    public void disposeGLSource(){
        if (glTextureId != -1) {
            TextureUtil.releaseTextureId(glTextureId);
            glTextureId = -1;
        }
    }

    public void dispose() {
        disposeGLSource();
        if (bufferedImage != null) {
            bufferedImage.flush();
            bufferedImage = null;
        }
    }

    public int getGlTextureId() {
        RenderSystem.assertThread(RenderSystem::isOnRenderThreadOrInit);
        return this.glTextureId;
    }

    @Override
    public void loadTexture(IResourceManager manager) throws IOException {
        //do nothing ...
    }

    public int getWidth(){
        return bufferedImage.getWidth();
    }
    public int getHeight(){
        return bufferedImage.getHeight();
    }

}
