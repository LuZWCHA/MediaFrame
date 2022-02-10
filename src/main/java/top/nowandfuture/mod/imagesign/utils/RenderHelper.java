package top.nowandfuture.mod.imagesign.utils;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

public class RenderHelper {
    public static final RenderType QUAD = RenderType.create("quad",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 256, false, false,
            RenderType.CompositeState.builder().createCompositeState(false));

    public static final RenderStateShard.TransparencyStateShard TRANSPARENCY_STATE = new RenderStateShard.TransparencyStateShard("translucent_transparency", () -> {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });
    public static final RenderType QUAD_TEX = RenderType.create("quad_tex",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS, 255650, false, false,
            RenderType.CompositeState.builder()
                    .setTexturingState(new RenderStateShard.TexturingStateShard("texture_enable",
                            new Runnable() {
                                @Override
                                public void run() {
                                    RenderSystem.enableTexture();
                                }
                            }, new Runnable() {
                        @Override
                        public void run() {
                            RenderSystem.disableTexture();
                            RenderSystem.bindTexture(0);
                        }
                    }))
                    .createCompositeState(false));

    

    public static int colorInt(int r, int g, int b, int a) {
        return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | (b & 255);
    }

    public static void blit(PoseStack matrixStack, int x, int y, int blitOffset, int width, int height, TextureAtlasSprite sprite) {
        innerBlit(matrixStack.last().pose(), x, x + width, y, y + height, blitOffset, sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1());
    }

    public static void blit(PoseStack matrixStack, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight) {
        blit(matrixStack, x, y, 0, (float) uOffset, (float) vOffset, uWidth, vHeight, 256, 256);
    }

    public static void blit(PoseStack matrixStack, int x, int y, int blitOffset, float uOffset, float vOffset, int uWidth, int vHeight, int textureHeight, int textureWidth) {
        innerBlit(matrixStack, x, x + uWidth, y, y + vHeight, blitOffset, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight);
    }

    public static void blit1(PoseStack matrixStack, int x, int y, int blitOffset, float uOffset, float vOffset, int uWidth, int vHeight, int textureHeight, int textureWidth, ResourceLocation id) {
        innerBlit(matrixStack, x, x + uWidth, y, y + vHeight, blitOffset, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight, id);
    }

    public static void blit(PoseStack matrixStack, int x, int y, int blitOffset, float uOffset, float vOffset, int uWidth, int vHeight, int textureHeight, int textureWidth, ResourceLocation id) {
        innerBlit(matrixStack, x, x + uWidth, y, y + vHeight, blitOffset, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight, id);
    }

    public static void blit(PoseStack matrixStack, int x, int y, int width, int height, float uOffset, float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        innerBlit(matrixStack, x, x + width, y, y + height, 0, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight);
    }

    public static void blit(PoseStack matrixStack, int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight) {
        blit(matrixStack, x, y, width, height, uOffset, vOffset, width, height, textureWidth, textureHeight);
    }

    private static void innerBlit(PoseStack matrixStack, int x1, int x2, int y1, int y2, int blitOffset, int uWidth, int vHeight, float uOffset, float vOffset, int textureWidth, int textureHeight) {
        innerBlit(matrixStack.last().pose(), x1, x2, y1, y2, blitOffset, (uOffset + 0.0F) / (float) textureWidth, (uOffset + (float) uWidth) / (float) textureWidth, (vOffset + 0.0F) / (float) textureHeight, (vOffset + (float) vHeight) / (float) textureHeight);
    }

    private static void innerBlit(PoseStack matrixStack, int x1, int x2, int y1, int y2, int blitOffset, int uWidth, int vHeight, float uOffset, float vOffset, int textureWidth, int textureHeight, ResourceLocation id) {
        innerBlit(matrixStack.last().pose(), x1, x2, y1, y2, blitOffset, (uOffset + 0.0F) / (float) textureWidth, (uOffset + (float) uWidth) / (float) textureWidth, (vOffset + 0.0F) / (float) textureHeight, (vOffset + (float) vHeight) / (float) textureHeight, id);
    }

    public final static int SKY_LIGHT = 15728640;
    public final static int EMMIT_BLOCK_LIGHT = 15728880;
    public static int light = SKY_LIGHT;

    public static int getCombineLight(Level world, BlockPos pos) {
        return LevelRenderer.getLightColor(world, pos);
    }

    public static int[] decodeCombineLight(int light){
        return new int[]{(light >> 20) & 15, (light >> 4) & 15};
    }

    public static int getCombineLight(int skyLight, int blockLight, int selfLight){
        int j = blockLight;
        if (j < selfLight) {
            j = selfLight;
        }

        return skyLight << 20 | j << 4;
    }

    public static int getCombineLight(Level world, BlockPos pos, int selfLight){
        int i = world.getBrightness(LightLayer.SKY, pos);
        int j = world.getBrightness(LightLayer.BLOCK, pos);
        int k = world.getLightEmission(pos);
        if (j < k) {
            j = k;
        }

        if(selfLight > j){
            j = selfLight;
        }

        return i << 20 | j << 4;
    }

    private static void innerBlit(Matrix4f matrix, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, ResourceLocation id) {

        MultiBufferSource.BufferSource renderTypeBuffer = Minecraft.getInstance().renderBuffers().bufferSource();

        VertexConsumer builder = renderTypeBuffer.getBuffer(RenderType.text(id));

        //   public static final VertexFormat POSITION_COLOR_TEX_LIGHTMAP = new VertexFormat(ImmutableList.<VertexFormatElement>builder().add(POSITION_3F).add(COLOR_4UB).add(TEX_2F).add(TEX_2SB).build());
        builder.vertex(matrix, (float) x1, (float) y2, (float) blitOffset).color(255, 255, 255, 255).uv(minU, maxV).uv2(light).endVertex();
        builder.vertex(matrix, (float) x2, (float) y2, (float) blitOffset).color(255, 255, 255, 255).uv(maxU, maxV).uv2(light).endVertex();
        builder.vertex(matrix, (float) x2, (float) y1, (float) blitOffset).color(255, 255, 255, 255).uv(maxU, minV).uv2(light).endVertex();
        builder.vertex(matrix, (float) x1, (float) y1, (float) blitOffset).color(255, 255, 255, 255).uv(minU, minV).uv2(light).endVertex();

        RenderSystem.enableDepthTest();
        renderTypeBuffer.endBatch();
    }

    private static void innerBlit(Matrix4f matrix, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV) {

        MultiBufferSource.BufferSource renderTypeBuffer = Minecraft.getInstance().renderBuffers().bufferSource();


        VertexConsumer builder = renderTypeBuffer.getBuffer(QUAD_TEX);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        builder.vertex(matrix, (float) x1, (float) y2, (float) blitOffset).uv(minU, maxV).endVertex();
        builder.vertex(matrix, (float) x2, (float) y2, (float) blitOffset).uv(maxU, maxV).endVertex();
        builder.vertex(matrix, (float) x2, (float) y1, (float) blitOffset).uv(maxU, minV).endVertex();
        builder.vertex(matrix, (float) x1, (float) y1, (float) blitOffset).uv(minU, minV).endVertex();
        renderTypeBuffer.endBatch();
        RenderSystem.depthMask(true);

    }

    public static void blit2(PoseStack matrixStack, int x, int y, int blitOffset, float uOffset, float vOffset, int uWidth, int vHeight, int textureHeight, int textureWidth, ResourceLocation id) {
        innerBlit2(matrixStack, x, x + uWidth, y, y + vHeight, blitOffset, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight, id);
    }

    public static void blit2(PoseStack matrixStack, int x, int y, int blitOffset, float uOffset, float vOffset, int uWidth, int vHeight, int textureHeight, int textureWidth, int light, ResourceLocation id) {
        innerBlit2(matrixStack, x, x + uWidth, y, y + vHeight, blitOffset, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight, light, id);
    }

    private static void innerBlit2(PoseStack matrixStack, int x1, int x2, int y1, int y2, int blitOffset, int uWidth, int vHeight, float uOffset, float vOffset, int textureWidth, int textureHeight, int light, ResourceLocation id) {
        innerBlit2(matrixStack, x1, x2, y1, y2, blitOffset, (uOffset + 0.0F) / (float) textureWidth, (uOffset + (float) uWidth) / (float) textureWidth, (vOffset + 0.0F) / (float) textureHeight, (vOffset + (float) vHeight) / (float) textureHeight, light, id);

    }

    private static void innerBlit2(PoseStack matrixStack, int x1, int x2, int y1, int y2, int blitOffset, int uWidth, int vHeight, float uOffset, float vOffset, int textureWidth, int textureHeight, ResourceLocation id) {
        innerBlit2(matrixStack, x1, x2, y1, y2, blitOffset, (uOffset + 0.0F) / (float) textureWidth, (uOffset + (float) uWidth) / (float) textureWidth, (vOffset + 0.0F) / (float) textureHeight, (vOffset + (float) vHeight) / (float) textureHeight, id);
    }

    public static void innerBlit2(PoseStack stack, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, ResourceLocation id) {
        innerBlit2(stack, x1, x2, y1, y2, blitOffset,minU, maxU, minV, maxV, RenderHelper.light, id);
    }

    public static void innerBlit2(PoseStack stack, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, int light, ResourceLocation id) {
        innerBlit2(stack, x1, x2, y1, y2, blitOffset,minU, maxU, minV, maxV, 0, 0, -1, light, id);
    }

    public static void innerBlit2(PoseStack stack, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, int nx, int ny, int nz, int light, ResourceLocation id) {
        PoseStack.Pose entry = stack.last();
        innerBlit2(entry.pose(), entry.normal(), x1, x2, y1, y2, blitOffset,minU, maxU, minV, maxV, nx, ny, nz, light, id);
    }

    public static void blit3(VertexConsumer builder, PoseStack matrixStack, int x, int y, int blitOffset, float uOffset, float vOffset, int uWidth, int vHeight, int textureHeight, int textureWidth) {
        innerBlit3(builder, matrixStack, x, x + uWidth, y, y + vHeight, blitOffset, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight);
    }

    public static void blit3(VertexConsumer builder, PoseStack matrixStack, int x, int y, int blitOffset, float uOffset, float vOffset, int uWidth, int vHeight, int textureHeight, int textureWidth, int light) {
        innerBlit3(builder, matrixStack, x, x + uWidth, y, y + vHeight, blitOffset, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight, light);
    }

    private static void innerBlit3(VertexConsumer builder, PoseStack matrixStack, int x1, int x2, int y1, int y2, int blitOffset, int uWidth, int vHeight, float uOffset, float vOffset, int textureWidth, int textureHeight, int light) {
        innerBlit3(builder, matrixStack, x1, x2, y1, y2, blitOffset, (uOffset + 0.0F) / (float) textureWidth, (uOffset + (float) uWidth) / (float) textureWidth, (vOffset + 0.0F) / (float) textureHeight, (vOffset + (float) vHeight) / (float) textureHeight, light);
    }

    private static void innerBlit3(VertexConsumer builder, PoseStack matrixStack, int x1, int x2, int y1, int y2, int blitOffset, int uWidth, int vHeight, float uOffset, float vOffset, int textureWidth, int textureHeight) {
        innerBlit3(builder, matrixStack, x1, x2, y1, y2, blitOffset, (uOffset + 0.0F) / (float) textureWidth, (uOffset + (float) uWidth) / (float) textureWidth, (vOffset + 0.0F) / (float) textureHeight, (vOffset + (float) vHeight) / (float) textureHeight);
    }

    public static void innerBlit3(VertexConsumer builder, PoseStack stack, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV) {
        innerBlit3(builder, stack, x1, x2, y1, y2, blitOffset,minU, maxU, minV, maxV, RenderHelper.light);
    }

    public static void innerBlit3(VertexConsumer builder, PoseStack stack, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, int light) {
        innerBlit3(builder, stack, x1, x2, y1, y2, blitOffset,minU, maxU, minV, maxV, 0, -1, 0, light);
    }

    public static void innerBlit3(VertexConsumer builder, PoseStack stack, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, int nx, int ny, int nz, int light) {
        PoseStack.Pose entry = stack.last();
        innerBlit3(builder, entry.pose(), entry.normal(), x1, x2, y1, y2, blitOffset,minU, maxU, minV, maxV, nx, ny, nz, light);
    }

    private static void innerBlit2(Matrix4f matrix, Matrix3f normalMatrix, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, int nx, int ny, int nz, int light, ResourceLocation id) {

        MultiBufferSource.BufferSource renderTypeBuffer = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer builder = renderTypeBuffer.getBuffer(RenderType.entitySolid(id));

        builder.vertex(matrix, (float) x1, (float) y2, (float) blitOffset).color(255, 255, 255, 255).uv(minU, maxV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normalMatrix, (float)nx, (float)ny, (float)nz).endVertex();
        builder.vertex(matrix, (float) x2, (float) y2, (float) blitOffset).color(255, 255, 255, 255).uv(maxU, maxV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normalMatrix, (float)nx, (float)ny, (float)nz).endVertex();
        builder.vertex(matrix, (float) x2, (float) y1, (float) blitOffset).color(255, 255, 255, 255).uv(maxU, minV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normalMatrix, (float)nx, (float)ny, (float)nz).endVertex();
        builder.vertex(matrix, (float) x1, (float) y1, (float) blitOffset).color(255, 255, 255, 255).uv(minU, minV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normalMatrix, (float)nx, (float)ny, (float)nz).endVertex();
    }

    private static void innerBlit3(VertexConsumer builder, Matrix4f matrix, Matrix3f normalMatrix, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, int nx, int ny, int nz, int light) {

        builder.vertex(matrix, (float) x1, (float) y2, (float) blitOffset).color(255, 255, 255, 255).uv(minU, maxV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normalMatrix, (float)nx, (float)ny, (float)nz).endVertex();
        builder.vertex(matrix, (float) x2, (float) y2, (float) blitOffset).color(255, 255, 255, 255).uv(maxU, maxV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normalMatrix, (float)nx, (float)ny, (float)nz).endVertex();
        builder.vertex(matrix, (float) x2, (float) y1, (float) blitOffset).color(255, 255, 255, 255).uv(maxU, minV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normalMatrix, (float)nx, (float)ny, (float)nz).endVertex();
        builder.vertex(matrix, (float) x1, (float) y1, (float) blitOffset).color(255, 255, 255, 255).uv(minU, minV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normalMatrix, (float)nx, (float)ny, (float)nz).endVertex();
    }


    private void renderPainting(PoseStack stack, VertexConsumer builder, Painting painting, int width, int height, TextureAtlasSprite paintTexture, TextureAtlasSprite backTexture) {
//        VertexConsumer VertexConsumer = bufferIn.getBuffer(RenderType.getEntitySolid(this.getEntityTexture(entityIn)));

        PoseStack.Pose entry = stack.last();
        Matrix4f matrix4f = entry.pose();
        Matrix3f matrix3f = entry.normal();
        float offsetW = (float)(-width) / 2.0F;
        float offsetH = (float)(-height) / 2.0F;
        //back
        float minU = backTexture.getU0();
        float maxU = backTexture.getU1();
        float minV = backTexture.getV0();
        float maxV = backTexture.getV1();
        //top, bottom
        float minU1 = backTexture.getU0();
        float maxU1 = backTexture.getU1();
        float minV1 = backTexture.getV0();
        float onePxV1 = backTexture.getV(1.0D);
        //left, right
        float minU2 = backTexture.getU0();
        float onePxU2 = backTexture.getU(1.0D);
        float minV2 = backTexture.getV0();
        float maxV2 = backTexture.getV1();

        int textureWidth = width / 16;
        int textureHeight = height / 16;
        double d0 = 16.0D / (double)textureWidth;
        double d1 = 16.0D / (double)textureHeight;

        for(int k = 0; k < textureWidth; ++k) {
            for(int l = 0; l < textureHeight; ++l) {
                float maxX = offsetW + (float)((k + 1) * 16);
                float minX = offsetW + (float)(k * 16);
                float maxY = offsetH + (float)((l + 1) * 16);
                float minY = offsetH + (float)(l * 16);
                int x = Mth.floor(painting.getX());
                int y = Mth.floor(painting.getY() + (double)((maxY + minY) / 2.0F / 16.0F));
                int z = Mth.floor(painting.getZ());
                Direction direction = painting.getMotionDirection();
                if (direction == Direction.NORTH) {
                    x = Mth.floor(painting.getX() + (double)((maxX + minX) / 2.0F / 16.0F));
                }

                if (direction == Direction.WEST) {
                    z = Mth.floor(painting.getZ() - (double)((maxX + minX) / 2.0F / 16.0F));
                }

                if (direction == Direction.SOUTH) {
                    x = Mth.floor(painting.getX() - (double)((maxX + minX) / 2.0F / 16.0F));
                }

                if (direction == Direction.EAST) {
                    z = Mth.floor(painting.getZ() + (double)((maxX + minX) / 2.0F / 16.0F));
                }

                int light = LevelRenderer.getLightColor(painting.level, new BlockPos(x, y, z));
                //back and broad around the painting
                float paintMinU = paintTexture.getU(d0 * (double)(textureWidth - k));
                float paintMaxU = paintTexture.getU(d0 * (double)(textureWidth - (k + 1)));
                float paintMinV = paintTexture.getV(d1 * (double)(textureHeight - l));
                float paintMaxV = paintTexture.getV(d1 * (double)(textureHeight - (l + 1)));
                //front
                this.renderVertex(matrix4f, matrix3f, builder, maxX, minY, paintMaxU, paintMinV, -0.5F, 0, 0, -1, light);
                this.renderVertex(matrix4f, matrix3f, builder, minX, minY, paintMinU, paintMinV, -0.5F, 0, 0, -1, light);
                this.renderVertex(matrix4f, matrix3f, builder, minX, maxY, paintMinU, paintMaxV, -0.5F, 0, 0, -1, light);
                this.renderVertex(matrix4f, matrix3f, builder, maxX, maxY, paintMaxU, paintMaxV, -0.5F, 0, 0, -1, light);
                //back
                this.renderVertex(matrix4f, matrix3f, builder, maxX, maxY, minU, minV, 0.5F, 0, 0, 1, light);
                this.renderVertex(matrix4f, matrix3f, builder, minX, maxY, maxU, minV, 0.5F, 0, 0, 1, light);
                this.renderVertex(matrix4f, matrix3f, builder, minX, minY, maxU, maxV, 0.5F, 0, 0, 1, light);
                this.renderVertex(matrix4f, matrix3f, builder, maxX, minY, minU, maxV, 0.5F, 0, 0, 1, light);
                //top
                this.renderVertex(matrix4f, matrix3f, builder, maxX, maxY, minU1, minV1, -0.5F, 0, 1, 0, light);
                this.renderVertex(matrix4f, matrix3f, builder, minX, maxY, maxU1, minV1, -0.5F, 0, 1, 0, light);
                this.renderVertex(matrix4f, matrix3f, builder, minX, maxY, maxU1, onePxV1, 0.5F, 0, 1, 0, light);
                this.renderVertex(matrix4f, matrix3f, builder, maxX, maxY, minU1, onePxV1, 0.5F, 0, 1, 0, light);
                //bottom
                this.renderVertex(matrix4f, matrix3f, builder, maxX, minY, minU1, minV1, 0.5F, 0, -1, 0, light);
                this.renderVertex(matrix4f, matrix3f, builder, minX, minY, maxU1, minV1, 0.5F, 0, -1, 0, light);
                this.renderVertex(matrix4f, matrix3f, builder, minX, minY, maxU1, onePxV1, -0.5F, 0, -1, 0, light);
                this.renderVertex(matrix4f, matrix3f, builder, maxX, minY, minU1, onePxV1, -0.5F, 0, -1, 0, light);
                //right
                this.renderVertex(matrix4f, matrix3f, builder, maxX, maxY, onePxU2, minV2, 0.5F, -1, 0, 0, light);
                this.renderVertex(matrix4f, matrix3f, builder, maxX, minY, onePxU2, maxV2, 0.5F, -1, 0, 0, light);
                this.renderVertex(matrix4f, matrix3f, builder, maxX, minY, minU2, maxV2, -0.5F, -1, 0, 0, light);
                this.renderVertex(matrix4f, matrix3f, builder, maxX, maxY, minU2, minV2, -0.5F, -1, 0, 0, light);
                //left
                this.renderVertex(matrix4f, matrix3f, builder, minX, maxY, onePxU2, minV2, -0.5F, 1, 0, 0, light);
                this.renderVertex(matrix4f, matrix3f, builder, minX, minY, onePxU2, maxV2, -0.5F, 1, 0, 0, light);
                this.renderVertex(matrix4f, matrix3f, builder, minX, minY, minU2, maxV2, 0.5F, 1, 0, 0, light);
                this.renderVertex(matrix4f, matrix3f, builder, minX, maxY, minU2, minV2, 0.5F, 1, 0, 0, light);
            }
        }

    }

    private void renderVertex(Matrix4f matrix4f, Matrix3f matrix3f, VertexConsumer builder, float x, float y, float u, float v, float z, int nx, int ny, int nz, int light) {
        builder.vertex(matrix4f, x, y, z).color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(matrix3f, (float)nx, (float)ny, (float)nz).endVertex();
    }
}
