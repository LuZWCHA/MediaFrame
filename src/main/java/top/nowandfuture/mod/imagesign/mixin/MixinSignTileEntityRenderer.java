package top.nowandfuture.mod.imagesign.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.block.BlockState;
import net.minecraft.block.StandingSignBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.EditSignScreen;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.tileentity.SignTileEntityRenderer;
import net.minecraft.tileentity.SignTileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import top.nowandfuture.mod.imagesign.ImageEntity;
import top.nowandfuture.mod.imagesign.ImageFetcher;
import top.nowandfuture.mod.imagesign.OpenGLImage;
import top.nowandfuture.mod.imagesign.schedulers.OpenGLScheduler;
import top.nowandfuture.mod.imagesign.SignImageLoadManager;
import top.nowandfuture.mod.imagesign.utils.RenderHelper;

@Mixin(SignTileEntityRenderer.class)
public abstract class MixinSignTileEntityRenderer {

    private void renderImages(ImageEntity imageEntity, SignTileEntity tileEntityIn, int width, int height, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn, int combinedOverlayIn) {
        BlockState blockstate = tileEntityIn.getBlockState();
        matrixStackIn.push();

        if (imageEntity.getOrgImages().size() == 1) {
            OpenGLImage entity = imageEntity.getOrgImages().get(0);
            ResourceLocation location = new ResourceLocation(
                    tileEntityIn.getPos().getCoordinatesAsString().replace(", ", "/")
            );
            TextureManager manager = Minecraft.getInstance().textureManager;
            if (manager.getTexture(location) == null) {
                manager.loadTexture(
                        location,
                        entity
                );
            }

            int w = entity.getWidth();
            int h = entity.getHeight();
            float wd = (float) w / width;
            float hd = (float) h / height;
            float d = Math.max(wd, hd);
            float scale = d;

            matrixStackIn.translate(.5, .5, .5);
            if (blockstate.getBlock() instanceof StandingSignBlock) {
                float f1 = -((float) (blockstate.get(StandingSignBlock.ROTATION) * 360) / 16.0F);
                matrixStackIn.rotate(Vector3f.YP.rotationDegrees(f1));
            } else {
                float f4 = -blockstate.get(WallSignBlock.FACING).getHorizontalAngle();
                matrixStackIn.rotate(Vector3f.YP.rotationDegrees(f4));
                matrixStackIn.translate(0.0D, -0.2725D, -0.4375D);
                //0 -30 42
            }
            matrixStackIn.translate(-.5 , 0.7725F, .046666667F);
            matrixStackIn.scale(1 / scale, -1 / scale, 1 / scale);
//        matrixStackIn.translate(0.0D, (double) 32F, (double) 44.8F);
//            matrixStackIn.scale(d, d, 1);

            RenderHelper.blit2(matrixStackIn,
                    0, 0, 0, 0, 0f,
                    w, h, h, w,
                    combinedLightIn, location);
        } else {
            // TODO: 2021/8/21 to display gif
        }

        matrixStackIn.pop();
    }

    @Inject(method = "render",
            at = @At(
                    value = "TAIL"
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    public void inject_render(SignTileEntity tileEntityIn, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn, int combinedOverlayIn, CallbackInfo callbackInfo) {
        if (Minecraft.getInstance().currentScreen instanceof EditSignScreen)
            return;

        String header = tileEntityIn.getText(0).getString();
        if ("[Image]".equals(header)) {
            ImageFetcher fetcher = ImageFetcher.INSTANCE;

            String url = tileEntityIn.getText(1).getString();

            if (fetcher.isInBlackList(url)) {
                return;
            }

            ImageEntity imageEntity = fetcher.grabImage(url);
            if (imageEntity != null && !imageEntity.equals(ImageEntity.EMPTY)) {
                //do render
                if (!imageEntity.isUploading() && !imageEntity.hasOpenGLSource()) {
                    imageEntity.uploadImage(false);
                } else if (imageEntity.hasOpenGLSource()) {
                    String pram = tileEntityIn.getText(2).getString();
                    String[] res = pram.split(",");
                    DoubleList pars = new DoubleArrayList();
                    String brightness = tileEntityIn.getText(3).getString();
                    int light = 0;
                    try {
                        light = Integer.parseInt(brightness);
                        for (String re : res) {
                            double data = Double.parseDouble(re);
                            pars.add(data);
                        }

                    } catch (NumberFormatException ignored) {

                    }

                    int[] lights = RenderHelper.decodeCombineLight(combinedLightIn);
                    combinedLightIn = RenderHelper.getCombineLight(lights[0], lights[1], light);

                    renderImages(imageEntity, tileEntityIn, 1, 1, partialTicks, matrixStackIn, bufferIn, combinedLightIn, combinedOverlayIn);
                }
            } else {
                SignImageLoadManager loadManager = SignImageLoadManager.INSTANCE;
                if (!loadManager.isLoading(tileEntityIn)) {
                    //noinspection ResultOfMethodCallIgnored
                    fetcher.get(url, tileEntityIn.getPos(), OpenGLScheduler.renderThread())
                            .doOnSubscribe(disposable1 -> {
                                loadManager.addToLoadingList(tileEntityIn, disposable1);
                            })
                            .subscribe(
                                    imageEntity1 -> {

                                    }, throwable -> {
                                        loadManager.removeFromLoadingList(tileEntityIn);
                                        fetcher.addToBlackList(url);

                                    }, () -> {
                                        loadManager.removeFromLoadingList(tileEntityIn);
                                    });
                }
            }


        }
    }
}

