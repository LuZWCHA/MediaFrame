package top.nowandfuture.mod.imagesign.utils;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.PaintingEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix3f;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL20;

public class RenderHelper {
    public static final RenderType QUAD = RenderType.makeType("quad",
            DefaultVertexFormats.POSITION_COLOR,
            GL20.GL_QUADS, 256,
            RenderType.State.getBuilder().build(false));

    public static final RenderState.TransparencyState TRANSPARENCY_STATE = new RenderState.TransparencyState("translucent_transparency", () -> {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });
    public static final RenderType QUAD_TEX = RenderType.makeType("quad_tex",
            DefaultVertexFormats.POSITION_TEX,
            GL20.GL_QUADS, 255650,
            RenderType.State.getBuilder()
                    .texturing(new RenderState.TexturingState("texture_enable",
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
                    .build(false));


    public static Vector4f getEntityRenderPos(Entity entityIn, double partialTicks) {

        final float renderX = (float) MathHelper.lerp(partialTicks, entityIn.lastTickPosX, entityIn.getPosX());
        float renderY = (float) MathHelper.lerp(partialTicks, entityIn.lastTickPosY, entityIn.getPosY()) + entityIn.getEyeHeight();
        final float renderZ = (float) MathHelper.lerp(partialTicks, entityIn.lastTickPosZ, entityIn.getPosZ());
        final float renderYaw = (float) MathHelper.lerp(partialTicks, entityIn.prevRotationYaw, entityIn.rotationYaw);

        return new Vector4f((float) renderX, (float)renderY, (float) renderZ, renderYaw);
    }

    public static int colorInt(int r, int g, int b, int a) {
        return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | (b & 255);
    }

    public static void blit(MatrixStack matrixStack, int x, int y, int blitOffset, int width, int height, TextureAtlasSprite sprite) {
        innerBlit(matrixStack.getLast().getMatrix(), x, x + width, y, y + height, blitOffset, sprite.getMinU(), sprite.getMaxU(), sprite.getMinV(), sprite.getMaxV());
    }

    public static void blit(MatrixStack matrixStack, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight) {
        blit(matrixStack, x, y, 0, (float) uOffset, (float) vOffset, uWidth, vHeight, 256, 256);
    }

    public static void blit(MatrixStack matrixStack, int x, int y, int blitOffset, float uOffset, float vOffset, int uWidth, int vHeight, int textureHeight, int textureWidth) {
        innerBlit(matrixStack, x, x + uWidth, y, y + vHeight, blitOffset, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight);
    }

    public static void blit1(MatrixStack matrixStack, int x, int y, int blitOffset, float uOffset, float vOffset, int uWidth, int vHeight, int textureHeight, int textureWidth, ResourceLocation id) {
        innerBlit(matrixStack, x, x + uWidth, y, y + vHeight, blitOffset, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight, id);
    }

    public static void blit(MatrixStack matrixStack, int x, int y, int blitOffset, float uOffset, float vOffset, int uWidth, int vHeight, int textureHeight, int textureWidth, ResourceLocation id) {
        innerBlit(matrixStack, x, x + uWidth, y, y + vHeight, blitOffset, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight, id);
    }

    public static void blit(MatrixStack matrixStack, int x, int y, int width, int height, float uOffset, float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        innerBlit(matrixStack, x, x + width, y, y + height, 0, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight);
    }

    public static void blit(MatrixStack matrixStack, int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight) {
        blit(matrixStack, x, y, width, height, uOffset, vOffset, width, height, textureWidth, textureHeight);
    }

    private static void innerBlit(MatrixStack matrixStack, int x1, int x2, int y1, int y2, int blitOffset, int uWidth, int vHeight, float uOffset, float vOffset, int textureWidth, int textureHeight) {
        innerBlit(matrixStack.getLast().getMatrix(), x1, x2, y1, y2, blitOffset, (uOffset + 0.0F) / (float) textureWidth, (uOffset + (float) uWidth) / (float) textureWidth, (vOffset + 0.0F) / (float) textureHeight, (vOffset + (float) vHeight) / (float) textureHeight);
    }

    private static void innerBlit(MatrixStack matrixStack, int x1, int x2, int y1, int y2, int blitOffset, int uWidth, int vHeight, float uOffset, float vOffset, int textureWidth, int textureHeight, ResourceLocation id) {
        innerBlit(matrixStack.getLast().getMatrix(), x1, x2, y1, y2, blitOffset, (uOffset + 0.0F) / (float) textureWidth, (uOffset + (float) uWidth) / (float) textureWidth, (vOffset + 0.0F) / (float) textureHeight, (vOffset + (float) vHeight) / (float) textureHeight, id);
    }

    public final static int SKY_LIGHT = 15728640;
    public final static int EMMIT_BLOCK_LIGHT = 15728880;
    public static int light = SKY_LIGHT;

    public static int getCombineLight(World world, BlockPos pos) {
        return WorldRenderer.getCombinedLight(world, pos);
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

    public static int getCombineLight(World world, BlockPos pos, int selfLight){
        int i = world.getLightFor(LightType.SKY, pos);
        int j = world.getLightFor(LightType.BLOCK, pos);
        int k = world.getLightValue(pos);
        if (j < k) {
            j = k;
        }

        if(selfLight > j){
            j = selfLight;
        }

        return i << 20 | j << 4;
    }

    private static void innerBlit(Matrix4f matrix, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, ResourceLocation id) {

        IRenderTypeBuffer.Impl renderTypeBuffer = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();

        IVertexBuilder builder = renderTypeBuffer.getBuffer(RenderType.getText(id));

        //   public static final VertexFormat POSITION_COLOR_TEX_LIGHTMAP = new VertexFormat(ImmutableList.<VertexFormatElement>builder().add(POSITION_3F).add(COLOR_4UB).add(TEX_2F).add(TEX_2SB).build());
        builder.pos(matrix, (float) x1, (float) y2, (float) blitOffset).color(255, 255, 255, 255).tex(minU, maxV).lightmap(light).endVertex();
        builder.pos(matrix, (float) x2, (float) y2, (float) blitOffset).color(255, 255, 255, 255).tex(maxU, maxV).lightmap(light).endVertex();
        builder.pos(matrix, (float) x2, (float) y1, (float) blitOffset).color(255, 255, 255, 255).tex(maxU, minV).lightmap(light).endVertex();
        builder.pos(matrix, (float) x1, (float) y1, (float) blitOffset).color(255, 255, 255, 255).tex(minU, minV).lightmap(light).endVertex();

        RenderSystem.enableDepthTest();
        renderTypeBuffer.finish();
    }

    private static void innerBlit(Matrix4f matrix, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV) {

        IRenderTypeBuffer.Impl renderTypeBuffer = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();


        IVertexBuilder builder = renderTypeBuffer.getBuffer(QUAD_TEX);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        builder.pos(matrix, (float) x1, (float) y2, (float) blitOffset).tex(minU, maxV).endVertex();
        builder.pos(matrix, (float) x2, (float) y2, (float) blitOffset).tex(maxU, maxV).endVertex();
        builder.pos(matrix, (float) x2, (float) y1, (float) blitOffset).tex(maxU, minV).endVertex();
        builder.pos(matrix, (float) x1, (float) y1, (float) blitOffset).tex(minU, minV).endVertex();
        renderTypeBuffer.finish();
        RenderSystem.depthMask(true);

    }

    public static void blit2(MatrixStack matrixStack, int x, int y, int blitOffset, float uOffset, float vOffset, int uWidth, int vHeight, int textureHeight, int textureWidth, ResourceLocation id) {
        innerBlit2(matrixStack, x, x + uWidth, y, y + vHeight, blitOffset, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight, id);
    }

    public static void blit2(MatrixStack matrixStack, int x, int y, int blitOffset, float uOffset, float vOffset, int uWidth, int vHeight, int textureHeight, int textureWidth, int light, ResourceLocation id) {
        innerBlit2(matrixStack, x, x + uWidth, y, y + vHeight, blitOffset, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight, light, id);
    }

    private static void innerBlit2(MatrixStack matrixStack, int x1, int x2, int y1, int y2, int blitOffset, int uWidth, int vHeight, float uOffset, float vOffset, int textureWidth, int textureHeight, int light, ResourceLocation id) {
        innerBlit2(matrixStack, x1, x2, y1, y2, blitOffset, (uOffset + 0.0F) / (float) textureWidth, (uOffset + (float) uWidth) / (float) textureWidth, (vOffset + 0.0F) / (float) textureHeight, (vOffset + (float) vHeight) / (float) textureHeight, light, id);

    }

    private static void innerBlit2(MatrixStack matrixStack, int x1, int x2, int y1, int y2, int blitOffset, int uWidth, int vHeight, float uOffset, float vOffset, int textureWidth, int textureHeight, ResourceLocation id) {
        innerBlit2(matrixStack, x1, x2, y1, y2, blitOffset, (uOffset + 0.0F) / (float) textureWidth, (uOffset + (float) uWidth) / (float) textureWidth, (vOffset + 0.0F) / (float) textureHeight, (vOffset + (float) vHeight) / (float) textureHeight, id);
    }

    public static void innerBlit2(MatrixStack stack, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, ResourceLocation id) {
        innerBlit2(stack, x1, x2, y1, y2, blitOffset,minU, maxU, minV, maxV, RenderHelper.light, id);
    }

    public static void innerBlit2(MatrixStack stack, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, int light, ResourceLocation id) {
        innerBlit2(stack, x1, x2, y1, y2, blitOffset,minU, maxU, minV, maxV, 0, 0, -1, light, id);
    }

    public static void innerBlit2(MatrixStack stack, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, int nx, int ny, int nz, int light, ResourceLocation id) {
        MatrixStack.Entry entry = stack.getLast();
        innerBlit2(entry.getMatrix(), entry.getNormal(), x1, x2, y1, y2, blitOffset,minU, maxU, minV, maxV, nx, ny, nz, light, id);
    }

    public static void blit3(IVertexBuilder builder, MatrixStack matrixStack, int x, int y, int blitOffset, float uOffset, float vOffset, int uWidth, int vHeight, int textureHeight, int textureWidth) {
        innerBlit3(builder, matrixStack, x, x + uWidth, y, y + vHeight, blitOffset, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight);
    }

    public static void blit3(IVertexBuilder builder, MatrixStack matrixStack, int x, int y, int blitOffset, float uOffset, float vOffset, int uWidth, int vHeight, int textureHeight, int textureWidth, int light) {
        innerBlit3(builder, matrixStack, x, x + uWidth, y, y + vHeight, blitOffset, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight, light);
    }

    private static void innerBlit3(IVertexBuilder builder, MatrixStack matrixStack, int x1, int x2, int y1, int y2, int blitOffset, int uWidth, int vHeight, float uOffset, float vOffset, int textureWidth, int textureHeight, int light) {
        innerBlit3(builder, matrixStack, x1, x2, y1, y2, blitOffset, (uOffset + 0.0F) / (float) textureWidth, (uOffset + (float) uWidth) / (float) textureWidth, (vOffset + 0.0F) / (float) textureHeight, (vOffset + (float) vHeight) / (float) textureHeight, light);
    }

    private static void innerBlit3(IVertexBuilder builder, MatrixStack matrixStack, int x1, int x2, int y1, int y2, int blitOffset, int uWidth, int vHeight, float uOffset, float vOffset, int textureWidth, int textureHeight) {
        innerBlit3(builder, matrixStack, x1, x2, y1, y2, blitOffset, (uOffset + 0.0F) / (float) textureWidth, (uOffset + (float) uWidth) / (float) textureWidth, (vOffset + 0.0F) / (float) textureHeight, (vOffset + (float) vHeight) / (float) textureHeight);
    }

    public static void innerBlit3(IVertexBuilder builder, MatrixStack stack, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV) {
        innerBlit3(builder, stack, x1, x2, y1, y2, blitOffset,minU, maxU, minV, maxV, RenderHelper.light);
    }

    public static void innerBlit3(IVertexBuilder builder, MatrixStack stack, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, int light) {
        innerBlit3(builder, stack, x1, x2, y1, y2, blitOffset,minU, maxU, minV, maxV, 0, -1, 0, light);
    }

    public static void innerBlit3(IVertexBuilder builder, MatrixStack stack, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, int nx, int ny, int nz, int light) {
        MatrixStack.Entry entry = stack.getLast();
        innerBlit3(builder, entry.getMatrix(), entry.getNormal(), x1, x2, y1, y2, blitOffset,minU, maxU, minV, maxV, nx, ny, nz, light);
    }

    private static void innerBlit2(Matrix4f matrix, Matrix3f normalMatrix, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, int nx, int ny, int nz, int light, ResourceLocation id) {

        IRenderTypeBuffer.Impl renderTypeBuffer = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
        IVertexBuilder builder = renderTypeBuffer.getBuffer(RenderType.getEntitySolid(id));

        builder.pos(matrix, (float) x1, (float) y2, (float) blitOffset).color(255, 255, 255, 255).tex(minU, maxV).overlay(OverlayTexture.NO_OVERLAY).lightmap(light).normal(normalMatrix, (float)nx, (float)ny, (float)nz).endVertex();
        builder.pos(matrix, (float) x2, (float) y2, (float) blitOffset).color(255, 255, 255, 255).tex(maxU, maxV).overlay(OverlayTexture.NO_OVERLAY).lightmap(light).normal(normalMatrix, (float)nx, (float)ny, (float)nz).endVertex();
        builder.pos(matrix, (float) x2, (float) y1, (float) blitOffset).color(255, 255, 255, 255).tex(maxU, minV).overlay(OverlayTexture.NO_OVERLAY).lightmap(light).normal(normalMatrix, (float)nx, (float)ny, (float)nz).endVertex();
        builder.pos(matrix, (float) x1, (float) y1, (float) blitOffset).color(255, 255, 255, 255).tex(minU, minV).overlay(OverlayTexture.NO_OVERLAY).lightmap(light).normal(normalMatrix, (float)nx, (float)ny, (float)nz).endVertex();
    }

    private static void innerBlit3(IVertexBuilder builder, Matrix4f matrix, Matrix3f normalMatrix, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, int nx, int ny, int nz, int light) {

        builder.pos(matrix, (float) x1, (float) y2, (float) blitOffset).color(255, 255, 255, 255).tex(minU, maxV).overlay(OverlayTexture.NO_OVERLAY).lightmap(light).normal(normalMatrix, (float)nx, (float)ny, (float)nz).endVertex();
        builder.pos(matrix, (float) x2, (float) y2, (float) blitOffset).color(255, 255, 255, 255).tex(maxU, maxV).overlay(OverlayTexture.NO_OVERLAY).lightmap(light).normal(normalMatrix, (float)nx, (float)ny, (float)nz).endVertex();
        builder.pos(matrix, (float) x2, (float) y1, (float) blitOffset).color(255, 255, 255, 255).tex(maxU, minV).overlay(OverlayTexture.NO_OVERLAY).lightmap(light).normal(normalMatrix, (float)nx, (float)ny, (float)nz).endVertex();
        builder.pos(matrix, (float) x1, (float) y1, (float) blitOffset).color(255, 255, 255, 255).tex(minU, minV).overlay(OverlayTexture.NO_OVERLAY).lightmap(light).normal(normalMatrix, (float)nx, (float)ny, (float)nz).endVertex();
    }


    private void renderPainting(MatrixStack stack, IVertexBuilder builder, PaintingEntity painting, int width, int height, TextureAtlasSprite paintTexture, TextureAtlasSprite backTexture) {
//        IVertexBuilder ivertexbuilder = bufferIn.getBuffer(RenderType.getEntitySolid(this.getEntityTexture(entityIn)));

        MatrixStack.Entry entry = stack.getLast();
        Matrix4f matrix4f = entry.getMatrix();
        Matrix3f matrix3f = entry.getNormal();
        float offsetW = (float)(-width) / 2.0F;
        float offsetH = (float)(-height) / 2.0F;
        //back
        float minU = backTexture.getMinU();
        float maxU = backTexture.getMaxU();
        float minV = backTexture.getMinV();
        float maxV = backTexture.getMaxV();
        //top, bottom
        float minU1 = backTexture.getMinU();
        float maxU1 = backTexture.getMaxU();
        float minV1 = backTexture.getMinV();
        float onePxV1 = backTexture.getInterpolatedV(1.0D);
        //left, right
        float minU2 = backTexture.getMinU();
        float onePxU2 = backTexture.getInterpolatedU(1.0D);
        float minV2 = backTexture.getMinV();
        float maxV2 = backTexture.getMaxV();

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
                int x = MathHelper.floor(painting.getPosX());
                int y = MathHelper.floor(painting.getPosY() + (double)((maxY + minY) / 2.0F / 16.0F));
                int z = MathHelper.floor(painting.getPosZ());
                Direction direction = painting.getHorizontalFacing();
                if (direction == Direction.NORTH) {
                    x = MathHelper.floor(painting.getPosX() + (double)((maxX + minX) / 2.0F / 16.0F));
                }

                if (direction == Direction.WEST) {
                    z = MathHelper.floor(painting.getPosZ() - (double)((maxX + minX) / 2.0F / 16.0F));
                }

                if (direction == Direction.SOUTH) {
                    x = MathHelper.floor(painting.getPosX() - (double)((maxX + minX) / 2.0F / 16.0F));
                }

                if (direction == Direction.EAST) {
                    z = MathHelper.floor(painting.getPosZ() + (double)((maxX + minX) / 2.0F / 16.0F));
                }

                int light = WorldRenderer.getCombinedLight(painting.world, new BlockPos(x, y, z));
                //back and broad around the painting
                float paintMinU = paintTexture.getInterpolatedU(d0 * (double)(textureWidth - k));
                float paintMaxU = paintTexture.getInterpolatedU(d0 * (double)(textureWidth - (k + 1)));
                float paintMinV = paintTexture.getInterpolatedV(d1 * (double)(textureHeight - l));
                float paintMaxV = paintTexture.getInterpolatedV(d1 * (double)(textureHeight - (l + 1)));
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

    private void renderVertex(Matrix4f matrix4f, Matrix3f matrix3f, IVertexBuilder builder, float x, float y, float u, float v, float z, int nx, int ny, int nz, int light) {
        builder.pos(matrix4f, x, y, z).color(255, 255, 255, 255).tex(u, v).overlay(OverlayTexture.NO_OVERLAY).lightmap(light).normal(matrix3f, (float)nx, (float)ny, (float)nz).endVertex();
    }
}
