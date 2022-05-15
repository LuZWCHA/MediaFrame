package top.nowandfuture.mod.imagesign.utils;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.opengl.GL11;
import top.nowandfuture.mod.imagesign.caches.*;
import top.nowandfuture.mod.imagesign.mixin.IGameRendererAccessor;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_BASE_LEVEL;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_MAX_LEVEL;
import static org.lwjgl.opengl.GL14.GL_TEXTURE_LOD_BIAS;

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

    public static void blit3(VertexConsumer builder, PoseStack matrixStack, int x, int y, int blitOffset, float uOffset, float vOffset, int uWidth, int vHeight, int textureHeight, int textureWidth, float nx, float ny, float nz,int light) {
        innerBlit3(builder, matrixStack, x, x + uWidth, y, y + vHeight, blitOffset, (uOffset + 0.0F) / (float) textureWidth, (uOffset + (float) uWidth) / (float) textureWidth, (vOffset + 0.0F) / (float) textureHeight, (vOffset + (float) vHeight) / (float) textureHeight, (int)nx, (int)ny, (int)nz, light);
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

    public static void renderImages(ImageEntity imageEntity, SignBlockEntity tileEntityIn, double width, double height,
                                    double offsetX, double offsetY, double offsetZ, float partialTicks, PoseStack matrixStackIn,
                                    MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn, boolean doOffset, boolean lie) {
        BlockState blockstate = tileEntityIn.getBlockState();

        if (imageEntity.getOrgImages().isEmpty()) {
            return;
        }

        int index = 0;
        int size = imageEntity.getOrgImages().size();
        if (size > 1) {
            long posLong = tileEntityIn.getBlockPos().asLong();
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
        AbstractTexture texture = manager.getTexture(location);
        manager.register(
                location,
                entity
        );
        if (texture == null) {
            return;
        } else {
            boolean isT = GL11.glIsTexture(texture.getId());

            if (!isT) {
                return;
            }
        }

        int w = entity.getWidth();
        int h = entity.getHeight();
        float wd = w / (float) width;
        float hd = h / (float) height;
        float scale = Math.max(wd, hd);
        matrixStackIn.pushPose();
        matrixStackIn.translate(.5, .5, .5);
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

        if (lie) {
            matrixStackIn.translate(0, -0.5, 0);
            matrixStackIn.mulPose(Vector3f.XN.rotationDegrees(90));
        }

        if (scale == wd) {
            matrixStackIn.translate(0, -(height - h / scale) / 2, 0);
        } else {
            matrixStackIn.translate((width - w / scale) / 2, 0, 0);
        }

        if (doOffset) matrixStackIn.translate(offsetX, lie ? offsetZ : offsetY, lie ? offsetY : offsetZ);
        matrixStackIn.translate(-.5, lie ? .5 : .7725F, doOffset ? .001 : .046666667F);

        matrixStackIn.scale(1 / scale, -1 / scale, 1 / scale);

        Vector4f p0 = new Vector4f(0, 0, 0, 1);
        Vector4f p1 = new Vector4f(0, h, 0, 1);
        Vector4f p2 = new Vector4f(w, h, 0, 1);
        Vector4f p3 = new Vector4f(w, 0, 0, 1);

        double area = calculateAreaOf(matrixStackIn.last().pose(), p0, p1, p2, p3, partialTicks);
        System.out.println(area);
        if(area >= MIN_AREA) {
            VertexConsumer builder = bufferIn.getBuffer(RenderType.entityTranslucent(location));

            final float oldBias = GL11.glGetTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS);
            final int oldMagFilter = GL11.glGetTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER);
            final int oldMinFilter = GL11.glGetTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER);

            GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
            GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 4);
            GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, 2.0f);
            GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);

            //normal: lie 0,0,1 stand 0,-1,0
            RenderHelper.blit3(builder, matrixStackIn,
                    0, 0, 0, 0, 0f,
                    w, h, h, w, 0, lie ? 0: -1, lie ? 1: 0,
                    combinedLightIn);

            //restore the bias and filters
            GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, oldBias);
            GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, oldMagFilter);
            GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, oldMinFilter);
        }

        matrixStackIn.popPose();
    }

    public static double MIN_AREA = 100.0;

    public static double calculateAreaOf(Matrix4f matrix, Vector4f point0, Vector4f point1, Vector4f point2, Vector4f point3, float partialTicks) {
        GameRenderer renderer = Minecraft.getInstance().gameRenderer;

        Matrix4f projectionMatrix = renderer.getProjectionMatrix(
                ((IGameRendererAccessor)renderer).invokeGetFov(renderer.getMainCamera(), partialTicks, true)
        );

        double x = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        double y = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        point0.transform(matrix);
        point1.transform(matrix);
        point2.transform(matrix);
        point3.transform(matrix);

        point0.transform(projectionMatrix);
        point1.transform(projectionMatrix);
        point2.transform(projectionMatrix);
        point3.transform(projectionMatrix);

        Vector3d p0 = vec4ToVec3(point0, x, y);
        Vector3d p1 = vec4ToVec3(point1, x, y);
        Vector3d p2 = vec4ToVec3(point2, x, y);
        Vector3d p3 = vec4ToVec3(point3, x, y);

        double a = p0.distance(p1);
        double b = p1.distance(p2);
        double c = p2.distance(p3);
        double d = p3.distance(p0);

        double z = (a + b + c + d) / 2;

        return 2 * Math.sqrt((z - a) * (z - b) * (z - c) * (z - d));
    }

    private static Vector3d vec4ToVec3(Vector4f vector4f, double xScale, double yScale) {
        return new Vector3d(xScale * vector4f.x() / vector4f.w(), yScale * vector4f.y() / vector4f.w(), 0);
    }
}
