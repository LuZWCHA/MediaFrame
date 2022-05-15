package top.nowandfuture.mod.imagesign.mixin;

import com.mojang.blaze3d.platform.GlStateManager;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.spongepowered.asm.mixin.Final;
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

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_BASE_LEVEL;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_MAX_LEVEL;
import static org.lwjgl.opengl.GL14.GL_TEXTURE_LOD_BIAS;


/**
 * To maintain the compatibility with optifine, render the images in the Minecraft RenderBuffer and upload the vertexes with other Minecraft entities.
 */
@Mixin(SignRenderer.class)
public abstract class MixinSignTileEntityRenderer implements BlockEntityRenderer<SignBlockEntity> {


    @Shadow
    @Final
    private Font font;
    private static final String HEADER = "[Image]";
    private static final String LR_HEADER = "[ImageT]";
    private static final String HEADER_LIE = "[ImageL]";
    private static final String LR_HEADER_LIE = "[ImageTL]";

    private void renderInfo(PoseStack matrixStackIn, BlockEntity tileEntityIn, Stage stage, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {

        matrixStackIn.pushPose();
        matrixStackIn.translate(.5, .5, .5);

        BlockState blockstate = tileEntityIn.getBlockState();
        float yaw;
        if (blockstate.getBlock() instanceof StandingSignBlock) {
            yaw = -((float) (blockstate.getValue(StandingSignBlock.ROTATION) * 360) / 16.0F);
            matrixStackIn.mulPose(Vector3f.YP.rotationDegrees(yaw));
        } else {
            yaw = -blockstate.getValue(WallSignBlock.FACING).toYRot();
            matrixStackIn.mulPose(Vector3f.YP.rotationDegrees(yaw));
            matrixStackIn.translate(0.0D, -.2725D, -.4375D);
            //0 -30 42
        }

        matrixStackIn.pushPose();
        matrixStackIn.scale(1 / 96f, -1 / 96f, 1 / 96f);

        Font fontrenderer = this.font;
        FormattedCharSequence processor =
                Component.nullToEmpty(I18n
                        .get(LoadStageLangKeyMap
                                .key(stage)))
                        .getVisualOrderText();
        fontrenderer.drawInBatch(processor, -fontrenderer.width(processor) / 2f, 0, 10526880, true, matrixStackIn.last().pose(), bufferIn, false, 0, combinedLightIn);
        int p = stage.getProgress();
        StringBuilder progressStr = new StringBuilder().append("□□□□□");
        for (int i = 0; i < Stage.totalStageNum(); i++, p--) {
            if (p >= 0) {
                progressStr.setCharAt(i, '■');
            } else {
                break;
            }
        }

        FormattedCharSequence processor2 = Component.nullToEmpty(progressStr.toString()).getVisualOrderText();

        fontrenderer.drawInBatch(processor2, -fontrenderer.width(processor2) / 2f, fontrenderer.lineHeight + 1, 10526880, false, matrixStackIn.last().pose(), bufferIn, false, 0, combinedLightIn);

        matrixStackIn.popPose();
        matrixStackIn.popPose();
    }

    @Override
    public int getViewDistance() {
        double distance = Minecraft.getInstance().options.renderDistance * 16;
        return (int) (distance * distance);
    }

    @Override
    public boolean shouldRender(SignBlockEntity pBlockEntity, Vec3 pCameraPos) {
        if(((ISignBlockEntityAccessor)pBlockEntity).getStage() == Stage.SUCCESS)
            return BlockEntityRenderer.super.shouldRender(pBlockEntity, pCameraPos);
        else{
            return Vec3.atCenterOf(pBlockEntity.getBlockPos()).closerThan(pCameraPos, 64);
        }
    }

    @Inject(method = "render(Lnet/minecraft/world/level/block/entity/SignBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(
                    value = "HEAD"
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT,
            cancellable = true)
    public void inject_render(SignBlockEntity tileEntityIn, float partialTicks, PoseStack matrixStackIn, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn, CallbackInfo callbackInfo) {
        if (Minecraft.getInstance().screen != null &&
                Minecraft.getInstance().screen instanceof SignEditScreen
                || Minecraft.getInstance().level == null)
            return;

        final String header = tileEntityIn.getMessage(0, true).getContents();

        final boolean normalImage = HEADER.equals(header) || HEADER_LIE.equals(header);
        final boolean thuImage = LR_HEADER.equals(header) || LR_HEADER_LIE.equals(header);
        final boolean lie = HEADER_LIE.equals(header) || LR_HEADER_LIE.equals(header);

        if (normalImage || thuImage && tileEntityIn instanceof ISignBlockEntityAccessor) {
            ISignBlockEntityAccessor entityAccessor = (ISignBlockEntityAccessor) (tileEntityIn);
            Stage stage = entityAccessor.getStage();

            ImageFetcher fetcher = ImageFetcher.INSTANCE;

            String url = tileEntityIn.getMessage(1, true).getContents();

            if (fetcher.isInBlackList(url)) {
                renderInfo(matrixStackIn, tileEntityIn, stage, bufferIn, combinedLightIn, combinedOverlayIn);
                return;
            }

            //Add a fake image entity to create it at next frames.
            BlockPos pos = tileEntityIn.getBlockPos();
            RenderQueue.addNextFrameRenderObj(null, new Vector3i(pos.getX(), pos.getY(), pos.getZ()), tileEntityIn.getBlockPos().distSqr(Minecraft.getInstance().getBlockEntityRenderDispatcher().camera.getBlockPosition()));
            if (!RenderQueue.isInRenderRange(tileEntityIn.getBlockPos())) {
                //Too many tiles to render, do not render this one
                //or the one has not been added to the query set at the previous frame;
                //Render or upload texture next frame.
                renderInfo(matrixStackIn, tileEntityIn, stage, bufferIn, combinedLightIn, combinedOverlayIn);
                return;
            }

            ImageEntity imageEntity = fetcher.grabImage(url, tileEntityIn.getBlockPos().asLong());
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
                        tileEntityIn.getMessage(2, true).getContents(),
                        tileEntityIn.getMessage(3, true).getContents()
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
                    RenderHelper.renderImages(imageEntity, tileEntityIn, params.width, params.height, params.offsetX, params.offsetY, params.offsetZ,
                            partialTicks, matrixStackIn, bufferIn, params.combineLight, combinedOverlayIn, params.doOffset, lie);
                    ((ISignBlockEntityAccessor) tileEntityIn).setStage(Stage.SUCCESS);

                    callbackInfo.cancel();
                } else {
                    renderInfo(matrixStackIn, tileEntityIn, stage, bufferIn, combinedLightIn, combinedOverlayIn);
                }


            } else {
                ImageLoadManager.INSTANCE.addToLoad(
                        fetcher.createImageLoadTask(url, tileEntityIn.getBlockPos().asLong())
                );
                renderInfo(matrixStackIn, tileEntityIn, stage, bufferIn, combinedLightIn, combinedOverlayIn);
            }

        }
    }
}

