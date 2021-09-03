package top.nowandfuture.mod.imagesign.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.block.BlockState;
import net.minecraft.block.StandingSignBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.EditSignScreen;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.Texture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.tileentity.SignTileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.SignTileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.math.vector.Vector3i;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import top.nowandfuture.mod.imagesign.RenderQueue;
import top.nowandfuture.mod.imagesign.caches.*;
import top.nowandfuture.mod.imagesign.loader.ImageFetcher;
import top.nowandfuture.mod.imagesign.loader.ImageLoadManager;
import top.nowandfuture.mod.imagesign.loader.ImageLoadTask;
import top.nowandfuture.mod.imagesign.utils.RenderHelper;
import top.nowandfuture.mod.imagesign.utils.Utils;

@Mixin(SignTileEntityRenderer.class)
public abstract class MixinSignTileEntityRenderer {

    private void renderImages(ImageEntity imageEntity, SignTileEntity tileEntityIn, double width, double height,
                              double offsetX, double offsetY, double offsetZ, float partialTicks, MatrixStack matrixStackIn,
                              IRenderTypeBuffer bufferIn, int combinedLightIn, int combinedOverlayIn, boolean doOffset) {
        BlockState blockstate = tileEntityIn.getBlockState();

        if (imageEntity.getOrgImages().isEmpty()) {
            return;
        }

        int index = 0;
        int size = imageEntity.getOrgImages().size();
        if (size > 1) {
            long posLong = tileEntityIn.getPos().toLong();
            Object p = imageEntity.imageInfo.getPram();
            if (p instanceof IParam) {
                if (p instanceof GIFParam) {
                    long curTick = GIFImagePlayManager.INSTANCE.getTick();
                    if (!GIFImagePlayManager.INSTANCE.contains(posLong)) {
                        GIFImagePlayManager.INSTANCE.setStartTickForPos(posLong, curTick);
                    }

                    long startTick = GIFImagePlayManager.INSTANCE.getStartTick(posLong);

                    long delay = ((GIFParam) p).getDelay() * 100L;
                    int leftPos = ((GIFParam) p).getLeftPosition();
                    int topPos = ((GIFParam) p).getTopPosition();
                    long t = delay * size;

                    long mills = (curTick - startTick) * 50;//50ms per tick
                    if (mills >= t) {
                        GIFImagePlayManager.INSTANCE.setStartTickForPos(posLong, curTick);
                    }
                    index = (int) ((mills / delay) % size);
                }
            }
        }
        OpenGLImage entity = imageEntity.getOrgImages().get(index);
        ResourceLocation location = new ResourceLocation(
                Utils.urlToByteString(imageEntity.url),
                String.valueOf(index)
        );
        TextureManager manager = Minecraft.getInstance().getTextureManager();
        Texture texture = manager.getTexture(location);
        manager.loadTexture(
                location,
                entity
        );
        if (texture == null) {
            return;
        } else {
            boolean isT = GL11.glIsTexture(texture.getGlTextureId());

            if (!isT) {
                return;
            }
        }

        int w = entity.getWidth();
        int h = entity.getHeight();
        float wd = w / (float) width;
        float hd = h / (float) height;
        float scale = Math.max(wd, hd);
        matrixStackIn.push();
        matrixStackIn.translate(.5, .5, .5);
        float yaw;
        if (blockstate.getBlock() instanceof StandingSignBlock) {
            yaw = -((float) (blockstate.get(StandingSignBlock.ROTATION) * 360) / 16.0F);
            matrixStackIn.rotate(Vector3f.YP.rotationDegrees(yaw));
        } else {
            yaw = -blockstate.get(WallSignBlock.FACING).getHorizontalAngle();
            matrixStackIn.rotate(Vector3f.YP.rotationDegrees(yaw));
            matrixStackIn.translate(0.0D, -.2725D, -.4375D);
            //0 -30 42
        }

        if (scale == wd) {
            matrixStackIn.translate(0, -(height - h / scale) / 2, 0);
        } else {
            matrixStackIn.translate((width - w / scale) / 2, 0, 0);
        }

        if (doOffset) matrixStackIn.translate(offsetX, offsetY, offsetZ);
        matrixStackIn.translate(-.5, .7725F, doOffset ? .001 : .046666667F);
        matrixStackIn.scale(1 / scale, -1 / scale, 1 / scale);

//        RenderHelper.blit2(matrixStackIn,
//                0, 0, 0, 0, 0f,
//                w, h, h, w,
//                combinedLightIn, location);
        IVertexBuilder builder = bufferIn.getBuffer(RenderType.getEntityTranslucent(location));
        RenderHelper.blit3(builder, matrixStackIn,
                0, 0, 0, 0, 0f,
                w, h, h, w,
                combinedLightIn);

        matrixStackIn.pop();
    }

    @Inject(method = "render",
            at = @At(
                    value = "HEAD"
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT,
            cancellable = true)
    public void inject_render(SignTileEntity tileEntityIn, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn, int combinedOverlayIn, CallbackInfo callbackInfo) {
        if (Minecraft.getInstance().currentScreen != null &&
                Minecraft.getInstance().currentScreen instanceof EditSignScreen
                || Minecraft.getInstance().world == null)
            return;

        String header = tileEntityIn.getText(0).getUnformattedComponentText();
        boolean normalImage = "[Image]".equals(header);
        boolean thuImage = "[ImageT]".equals(header);
        if (normalImage || thuImage) {
            ImageFetcher fetcher = ImageFetcher.INSTANCE;

            String url = tileEntityIn.getText(1).getUnformattedComponentText();

            if (fetcher.isInBlackList(url)) {
                return;
            }

            RenderQueue.addNextFrameRenderObj(null, tileEntityIn.getPos(), tileEntityIn.getPos().distanceSq(TileEntityRendererDispatcher.instance.renderInfo.getBlockPos()));
            if (!RenderQueue.isInPosSet(tileEntityIn.getPos())) {
                //Too many tiles to render, do not render this one.
                //Or the one has not been added to the query set at the previous frame;
                //Render or upload texture next frame.
                return;
            }

            ImageEntity imageEntity = fetcher.grabImage(url, tileEntityIn.getPos().toLong());
            if (imageEntity != null && !ImageEntity.EMPTY.equals(imageEntity)) {

                if (imageEntity.getOrgImages().isEmpty())
                    return;

                OpenGLImage entity = imageEntity.getOrgImages().get(0);

                String pram = tileEntityIn.getText(2).getUnformattedComponentText();
                String[] res = pram.split(",");
                DoubleList pars = new DoubleArrayList();
                String brightness = tileEntityIn.getText(3).getUnformattedComponentText();
                int light = 0;
                try {
                    light = Integer.parseInt(brightness);

                } catch (NumberFormatException ignored) {

                }
                try {
                    for (String re : res) {
                        double data = Double.parseDouble(re);
                        pars.add(data);
                    }
                } catch (NumberFormatException ignored) {

                }

                double width = 1, height = 1;
                boolean doOffset = false;
                double offsetX = 0D, offsetY = 0D, offsetZ = 0D;

                if (pars.size() == 2) {
                    if (pars.getDouble(0) > 0) {
                        width = pars.getDouble(0);
                    }

                    if (pars.getDouble(1) > 0) {
                        height = pars.getDouble(1);
                    }
                }

                if (pars.size() == 3) {
                    doOffset = true;
                    offsetX = pars.getDouble(0);
                    offsetY = pars.getDouble(1);
                    offsetZ = pars.getDouble(2);
                }

                if (pars.size() == 5) {
                    doOffset = true;
                    if (pars.getDouble(0) > 0) {
                        width = pars.getDouble(0);
                    }

                    if (pars.getDouble(1) > 0) {
                        height = pars.getDouble(1);
                    }
                    offsetX = pars.getDouble(2);
                    offsetY = pars.getDouble(3);
                    offsetZ = pars.getDouble(4);
                }

                int[] lights = RenderHelper.decodeCombineLight(combinedLightIn);
                combinedLightIn = RenderHelper.getCombineLight(lights[0], lights[1], light);

                int w = entity.getWidth();
                int h = entity.getHeight();
                float wd = w / (float) width;
                float hd = h / (float) height;
                float scale = Math.max(wd, hd);

                if (imageEntity.isToUpdate()) {
                    imageEntity.uploadImage(thuImage, 16 / scale);
                    imageEntity.setToUpdate(false);
                } else if (entity.isThumbnail() && entity.getScale() > 16 / scale && imageEntity.hasOpenGLSource()) {
                    imageEntity.uploadImage(thuImage, 16 / scale);
                }

                if (imageEntity.hasOpenGLSource()) {
                    //Do render
                    renderImages(imageEntity, tileEntityIn, width, height, offsetX, offsetY, offsetZ,
                            partialTicks, matrixStackIn, bufferIn, combinedLightIn, combinedOverlayIn, doOffset);

                    callbackInfo.cancel();
                }
            } else {
                ImageLoadManager.INSTANCE.addToLoad(
                        new ImageLoadTask.SignImageLoadTask(tileEntityIn.getPos().toLong(), url)
                );
            }


        }
    }
}

