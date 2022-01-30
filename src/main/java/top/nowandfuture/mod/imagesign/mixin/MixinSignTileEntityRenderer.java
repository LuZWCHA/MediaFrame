package top.nowandfuture.mod.imagesign.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.StandingSignBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.EditSignScreen;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.Texture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.tileentity.SignTileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.resources.I18n;
import net.minecraft.tileentity.SignTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import top.nowandfuture.mod.imagesign.RenderQueue;
import top.nowandfuture.mod.imagesign.caches.*;
import top.nowandfuture.mod.imagesign.loader.*;
import top.nowandfuture.mod.imagesign.caches.Vector3i;
import top.nowandfuture.mod.imagesign.utils.ParamsParser;
import top.nowandfuture.mod.imagesign.utils.RenderHelper;
import top.nowandfuture.mod.imagesign.utils.Utils;


/**
 * To maintain the compatibility with optifine, render the images in the Minecraft RenderBuffer and upload the vertexes with other Minecraft entities.
 */
@Mixin(SignTileEntityRenderer.class)
public abstract class MixinSignTileEntityRenderer extends TileEntityRenderer<SignTileEntity> {

    public MixinSignTileEntityRenderer(TileEntityRendererDispatcher rendererDispatcherIn) {
        super(rendererDispatcherIn);
    }

    @Shadow
    @Override
    public abstract void render(@NotNull SignTileEntity tileEntityIn, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn, int combinedOverlayIn);

    private static final String HEADER = "[Image]";
    private static final String LR_HEADER = "[ImageT]";

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

        IVertexBuilder builder = bufferIn.getBuffer(RenderType.getEntityTranslucent(location));
        RenderHelper.blit3(builder, matrixStackIn,
                0, 0, 0, 0, 0f,
                w, h, h, w,
                combinedLightIn);

        matrixStackIn.pop();
    }

    private void renderInfo(MatrixStack matrixStackIn, TileEntity tileEntityIn, Stage stage, IRenderTypeBuffer bufferIn, int combinedLightIn, int combinedOverlayIn) {

        matrixStackIn.push();
        matrixStackIn.translate(.5, .5, .5);

        BlockState blockstate = tileEntityIn.getBlockState();
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

        matrixStackIn.push();
        matrixStackIn.scale(1 / 96f, -1 / 96f, 1 / 96f);

        FontRenderer fontrenderer = this.renderDispatcher.getFontRenderer();
        IReorderingProcessor processor =
                ITextComponent
                        .getTextComponentOrEmpty(I18n
                                .format(LoadStageLangKeyMap
                                        .key(stage)))
                        .func_241878_f();
        fontrenderer.drawEntityText(processor, -fontrenderer.func_243245_a(processor) / 2f, 0, 10526880, true, matrixStackIn.getLast().getMatrix(), bufferIn, false, 0, combinedLightIn);
        int p = stage.getProgress();
        StringBuilder progressStr = new StringBuilder().append("□□□□□");
        for (int i = 0; i < Stage.totalStageNum(); i++, p--) {
            if (p >= 0) {
                progressStr.setCharAt(i, '■');
            } else {
                break;
            }
        }

        IReorderingProcessor processor2 = ITextComponent.getTextComponentOrEmpty(progressStr.toString()).func_241878_f();

        fontrenderer.drawEntityText(processor2, -fontrenderer.func_243245_a(processor2) / 2f, fontrenderer.FONT_HEIGHT + 1, 10526880, false, matrixStackIn.getLast().getMatrix(), bufferIn, false, 0, combinedLightIn);

        matrixStackIn.pop();


        matrixStackIn.pop();


    }

    @Inject(method = "render(Lnet/minecraft/tileentity/SignTileEntity;FLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;II)V",
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

        final String header = tileEntityIn.getText(0).getUnformattedComponentText();
        final boolean normalImage = HEADER.equals(header);
        final boolean thuImage = LR_HEADER.equals(header);
        if (normalImage || thuImage && tileEntityIn instanceof ISignBlockEntityAccessor) {
            ISignBlockEntityAccessor entityAccessor = (ISignBlockEntityAccessor) (tileEntityIn);
            Stage stage = entityAccessor.getStage();

            ImageFetcher fetcher = ImageFetcher.INSTANCE;

            String url = tileEntityIn.getText(1).getUnformattedComponentText();

            if (fetcher.isInBlackList(url)) {
                renderInfo(matrixStackIn, tileEntityIn, stage, bufferIn, combinedLightIn, combinedOverlayIn);
                return;
            }

            //Add a fake image entity to create it at next frames.
            BlockPos pos = tileEntityIn.getPos();
            RenderQueue.addNextFrameRenderObj(null, new Vector3i(pos.getX(), pos.getY(), pos.getZ()), tileEntityIn.getPos().distanceSq(TileEntityRendererDispatcher.instance.renderInfo.getBlockPos()));
            if (!RenderQueue.isInRenderRange(tileEntityIn.getPos())) {
                //Too many tiles to render, do not render this one
                //or the one has not been added to the query set at the previous frame;
                //Render or upload texture next frame.
                renderInfo(matrixStackIn, tileEntityIn, stage, bufferIn, combinedLightIn, combinedOverlayIn);
                return;
            }

            ImageEntity imageEntity = fetcher.grabImage(url, tileEntityIn.getPos().toLong());
            if (imageEntity != null && !ImageEntity.EMPTY.equals(imageEntity)) {

                if (imageEntity.getOrgImages().isEmpty()) {
                    renderInfo(matrixStackIn, tileEntityIn, stage, bufferIn, combinedLightIn, combinedOverlayIn);
                    return;
                }

                //The first image of the "Image"(gif has more than one image);
                OpenGLImage entity = imageEntity.getOrgImages().get(0);

                String[] lines = {
                        header,
                        url,
                        tileEntityIn.getText(2).getUnformattedComponentText(),
                        tileEntityIn.getText(3).getUnformattedComponentText()
                };

                ParamsParser.Params params = ParamsParser.parse(lines, combinedLightIn);

                int w = entity.getWidth();
                int h = entity.getHeight();
                float wd = w / (float) params.width;
                float hd = h / (float) params.height;
                float scale = Math.max(wd, hd);

                if (imageEntity.isToUpdate()) {//The image is not upload to GPU (if GPU is valid).
                    imageEntity.uploadImage(thuImage, 16 / scale);
                    imageEntity.setToUpdate(false);
                } else if (entity.isThumbnail() && entity.getScale() > 16 / scale && imageEntity.hasOpenGLSource()) { //If the image is rendered as normal image but this image will render a low resolution one, then, re-upload a lr-image.
                    imageEntity.uploadImage(thuImage, 16 / scale);
                }

                if (imageEntity.hasOpenGLSource()) {
                    //Do render
                    renderImages(imageEntity, tileEntityIn, params.width, params.height, params.offsetX, params.offsetY, params.offsetZ,
                            partialTicks, matrixStackIn, bufferIn, combinedLightIn, combinedOverlayIn, params.doOffset);

                    callbackInfo.cancel();
                } else {
                    renderInfo(matrixStackIn, tileEntityIn, stage, bufferIn, combinedLightIn, combinedOverlayIn);
                }


            } else {
                ImageLoadManager.INSTANCE.addToLoad(
                        fetcher.createImageLoadTask(url, tileEntityIn.getPos().toLong())
                );
                renderInfo(matrixStackIn, tileEntityIn, stage, bufferIn, combinedLightIn, combinedOverlayIn);
            }

        }
    }
}

