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
import net.minecraft.client.renderer.texture.Texture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.tileentity.SignTileEntityRenderer;
import net.minecraft.tileentity.SignTileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import top.nowandfuture.mod.imagesign.caches.ImageEntity;
import top.nowandfuture.mod.imagesign.caches.OpenGLImage;
import top.nowandfuture.mod.imagesign.loader.ImageFetcher;
import top.nowandfuture.mod.imagesign.loader.SignImageLoadManager;
import top.nowandfuture.mod.imagesign.schedulers.OpenGLScheduler;
import top.nowandfuture.mod.imagesign.utils.RenderHelper;

@Mixin(SignTileEntityRenderer.class)
public abstract class MixinSignTileEntityRenderer {

    private long frame = Minecraft.getInstance().getFrameTimer().getIndex();

    private void renderImages(ImageEntity imageEntity, SignTileEntity tileEntityIn, double width, double height, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn, int combinedOverlayIn) {
        BlockState blockstate = tileEntityIn.getBlockState();
//        if(imageEntity.checkShader(ImageSign.getShaderLoaded())){
//            //shader changed.
//            ResourceLocation location = new ResourceLocation(
//                    String.valueOf(tileEntityIn.getPos().toLong())
//            );
//            TextureManager manager = Minecraft.getInstance().getTextureManager();
//            manager.deleteTexture(location);
//            return;
//        }

        if (imageEntity.getOrgImages().size() == 1) {
            OpenGLImage entity = imageEntity.getOrgImages().get(0);
            ResourceLocation location = new ResourceLocation(
                    String.valueOf(tileEntityIn.getPos().toLong())
            );
            TextureManager manager = Minecraft.getInstance().getTextureManager();
            Texture texture = manager.getTexture(location);
            manager.loadTexture(
                    location,
                    entity
            );
            if (texture == null) {
                return;
            }else{
                boolean isT = GL11.glIsTexture(texture.getGlTextureId());

                if(!isT){
                    System.out.println(texture.getGlTextureId() + ", " + isT);
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
            if (blockstate.getBlock() instanceof StandingSignBlock) {
                float f1 = -((float) (blockstate.get(StandingSignBlock.ROTATION) * 360) / 16.0F);
                matrixStackIn.rotate(Vector3f.YP.rotationDegrees(f1));
            } else {
                float f4 = -blockstate.get(WallSignBlock.FACING).getHorizontalAngle();
                matrixStackIn.rotate(Vector3f.YP.rotationDegrees(f4));
                matrixStackIn.translate(0.0D, -0.2725D, -0.4375D);
                //0 -30 42
            }

            if(scale == wd){
                matrixStackIn.translate(0,-(height - h / scale) / 2,0);
            }else{
                matrixStackIn.translate((width - w / scale) / 2, 0, 0);
            }

            matrixStackIn.translate(-.5 , 0.7725F, .046666667F);
            matrixStackIn.scale(1 / scale, -1 / scale, 1 / scale);

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
                    value = "HEAD"
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT,
            cancellable = true)
    public void inject_render(SignTileEntity tileEntityIn, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn, int combinedOverlayIn, CallbackInfo callbackInfo) {
        if (Minecraft.getInstance().currentScreen != null &&
                Minecraft.getInstance().currentScreen instanceof EditSignScreen
                || Minecraft.getInstance().world == null)
            return;

        String header = tileEntityIn.getText(0).getString();
        if ("[Image]".equals(header)) {
            ImageFetcher fetcher = ImageFetcher.INSTANCE;

            String url = tileEntityIn.getText(1).getString();

            if (fetcher.isInBlackList(url)) {
                return;
            }

            ImageEntity imageEntity = fetcher.grabImage(url, tileEntityIn.getPos());
            if (imageEntity != null && !ImageEntity.EMPTY.equals(imageEntity)) {
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
                    double width = 1, height = 1;

                    if(pars.size() >= 2){
                        if(pars.getDouble(0) > 0){
                            width = pars.getDouble(0);
                        }

                        if(pars.getDouble(1) > 0){
                            height = pars.getDouble(1);
                        }
                    }
                    int[] lights = RenderHelper.decodeCombineLight(combinedLightIn);
                    combinedLightIn = RenderHelper.getCombineLight(lights[0], lights[1], light);

                    renderImages(imageEntity, tileEntityIn, width, height, partialTicks, matrixStackIn, bufferIn, combinedLightIn, combinedOverlayIn);

                    callbackInfo.cancel();
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
                                        throwable.printStackTrace();
                                    }, () -> {
                                        loadManager.removeFromLoadingList(tileEntityIn);
                                    });
                }
            }


        }
    }
}

