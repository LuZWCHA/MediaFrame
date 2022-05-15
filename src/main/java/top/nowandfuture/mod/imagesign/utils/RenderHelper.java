package top.nowandfuture.mod.imagesign.utils;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.StandingSignBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.Texture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.PaintingEntity;
import net.minecraft.tileentity.SignTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix3f;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import top.nowandfuture.mod.imagesign.Config;
import top.nowandfuture.mod.imagesign.caches.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_NEAREST_MIPMAP_NEAREST;
import static org.lwjgl.opengl.GL14.GL_TEXTURE_LOD_BIAS;

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

        return new Vector4f((float) renderX, (float) renderY, (float) renderZ, renderYaw);
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

    public static int[] decodeCombineLight(int light) {
        return new int[]{(light >> 20) & 15, (light >> 4) & 15};
    }

    public static int getCombineLight(int skyLight, int blockLight, int selfLight) {
        int j = blockLight;
        if (j < selfLight) {
            j = selfLight;
        }

        return skyLight << 20 | j << 4;
    }

    public static int getCombineLight(World world, BlockPos pos, int selfLight) {
        int i = world.getLightFor(LightType.SKY, pos);
        int j = world.getLightFor(LightType.BLOCK, pos);
        int k = world.getLightValue(pos);
        if (j < k) {
            j = k;
        }

        if (selfLight > j) {
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
        innerBlit2(stack, x1, x2, y1, y2, blitOffset, minU, maxU, minV, maxV, RenderHelper.light, id);
    }

    public static void innerBlit2(MatrixStack stack, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, int light, ResourceLocation id) {
        innerBlit2(stack, x1, x2, y1, y2, blitOffset, minU, maxU, minV, maxV, 0, 0, -1, light, id);
    }

    public static void innerBlit2(MatrixStack stack, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, int nx, int ny, int nz, int light, ResourceLocation id) {
        MatrixStack.Entry entry = stack.getLast();
        innerBlit2(entry.getMatrix(), entry.getNormal(), x1, x2, y1, y2, blitOffset, minU, maxU, minV, maxV, nx, ny, nz, light, id);
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
        innerBlit3(builder, stack, x1, x2, y1, y2, blitOffset, minU, maxU, minV, maxV, RenderHelper.light);
    }

    public static void innerBlit3(IVertexBuilder builder, MatrixStack stack, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, int light) {
        innerBlit3(builder, stack, x1, x2, y1, y2, blitOffset, minU, maxU, minV, maxV, 0, -1, 0, light);
    }

    public static void innerBlit3(IVertexBuilder builder, MatrixStack stack, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, int nx, int ny, int nz, int light) {
        MatrixStack.Entry entry = stack.getLast();
        innerBlit3(builder, entry.getMatrix(), entry.getNormal(), x1, x2, y1, y2, blitOffset, minU, maxU, minV, maxV, nx, ny, nz, light);
    }

    private static void innerBlit2(Matrix4f matrix, Matrix3f normalMatrix, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, int nx, int ny, int nz, int light, ResourceLocation id) {

        IRenderTypeBuffer.Impl renderTypeBuffer = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
        IVertexBuilder builder = renderTypeBuffer.getBuffer(RenderType.getEntitySolid(id));

        builder.pos(matrix, (float) x1, (float) y2, (float) blitOffset).color(255, 255, 255, 255).tex(minU, maxV).overlay(OverlayTexture.NO_OVERLAY).lightmap(light).normal(normalMatrix, (float) nx, (float) ny, (float) nz).endVertex();
        builder.pos(matrix, (float) x2, (float) y2, (float) blitOffset).color(255, 255, 255, 255).tex(maxU, maxV).overlay(OverlayTexture.NO_OVERLAY).lightmap(light).normal(normalMatrix, (float) nx, (float) ny, (float) nz).endVertex();
        builder.pos(matrix, (float) x2, (float) y1, (float) blitOffset).color(255, 255, 255, 255).tex(maxU, minV).overlay(OverlayTexture.NO_OVERLAY).lightmap(light).normal(normalMatrix, (float) nx, (float) ny, (float) nz).endVertex();
        builder.pos(matrix, (float) x1, (float) y1, (float) blitOffset).color(255, 255, 255, 255).tex(minU, minV).overlay(OverlayTexture.NO_OVERLAY).lightmap(light).normal(normalMatrix, (float) nx, (float) ny, (float) nz).endVertex();
    }

    private static void innerBlit3(IVertexBuilder builder, Matrix4f matrix, Matrix3f normalMatrix, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, int nx, int ny, int nz, int light) {

        builder.pos(matrix, (float) x1, (float) y2, (float) blitOffset).color(255, 255, 255, 255).tex(minU, maxV).overlay(OverlayTexture.NO_OVERLAY).lightmap(light).normal(normalMatrix, (float) nx, (float) ny, (float) nz).endVertex();
        builder.pos(matrix, (float) x2, (float) y2, (float) blitOffset).color(255, 255, 255, 255).tex(maxU, maxV).overlay(OverlayTexture.NO_OVERLAY).lightmap(light).normal(normalMatrix, (float) nx, (float) ny, (float) nz).endVertex();
        builder.pos(matrix, (float) x2, (float) y1, (float) blitOffset).color(255, 255, 255, 255).tex(maxU, minV).overlay(OverlayTexture.NO_OVERLAY).lightmap(light).normal(normalMatrix, (float) nx, (float) ny, (float) nz).endVertex();
        builder.pos(matrix, (float) x1, (float) y1, (float) blitOffset).color(255, 255, 255, 255).tex(minU, minV).overlay(OverlayTexture.NO_OVERLAY).lightmap(light).normal(normalMatrix, (float) nx, (float) ny, (float) nz).endVertex();
    }


    private void renderPainting(MatrixStack stack, IVertexBuilder builder, PaintingEntity painting, int width, int height, TextureAtlasSprite paintTexture, TextureAtlasSprite backTexture) {
//        IVertexBuilder ivertexbuilder = bufferIn.getBuffer(RenderType.getEntitySolid(this.getEntityTexture(entityIn)));

        MatrixStack.Entry entry = stack.getLast();
        Matrix4f matrix4f = entry.getMatrix();
        Matrix3f matrix3f = entry.getNormal();
        float offsetW = (float) (-width) / 2.0F;
        float offsetH = (float) (-height) / 2.0F;
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
        double d0 = 16.0D / (double) textureWidth;
        double d1 = 16.0D / (double) textureHeight;

        for (int k = 0; k < textureWidth; ++k) {
            for (int l = 0; l < textureHeight; ++l) {
                float maxX = offsetW + (float) ((k + 1) * 16);
                float minX = offsetW + (float) (k * 16);
                float maxY = offsetH + (float) ((l + 1) * 16);
                float minY = offsetH + (float) (l * 16);
                int x = MathHelper.floor(painting.getPosX());
                int y = MathHelper.floor(painting.getPosY() + (double) ((maxY + minY) / 2.0F / 16.0F));
                int z = MathHelper.floor(painting.getPosZ());
                Direction direction = painting.getHorizontalFacing();
                if (direction == Direction.NORTH) {
                    x = MathHelper.floor(painting.getPosX() + (double) ((maxX + minX) / 2.0F / 16.0F));
                }

                if (direction == Direction.WEST) {
                    z = MathHelper.floor(painting.getPosZ() - (double) ((maxX + minX) / 2.0F / 16.0F));
                }

                if (direction == Direction.SOUTH) {
                    x = MathHelper.floor(painting.getPosX() - (double) ((maxX + minX) / 2.0F / 16.0F));
                }

                if (direction == Direction.EAST) {
                    z = MathHelper.floor(painting.getPosZ() + (double) ((maxX + minX) / 2.0F / 16.0F));
                }

                int light = WorldRenderer.getCombinedLight(painting.world, new BlockPos(x, y, z));
                //back and broad around the painting
                float paintMinU = paintTexture.getInterpolatedU(d0 * (double) (textureWidth - k));
                float paintMaxU = paintTexture.getInterpolatedU(d0 * (double) (textureWidth - (k + 1)));
                float paintMinV = paintTexture.getInterpolatedV(d1 * (double) (textureHeight - l));
                float paintMaxV = paintTexture.getInterpolatedV(d1 * (double) (textureHeight - (l + 1)));
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
        builder.pos(matrix4f, x, y, z).color(255, 255, 255, 255).tex(u, v).overlay(OverlayTexture.NO_OVERLAY).lightmap(light).normal(matrix3f, (float) nx, (float) ny, (float) nz).endVertex();
    }


    public static void renderImages(ImageEntity imageEntity, SignTileEntity tileEntityIn, double width, double height,
                                    double offsetX, double offsetY, double offsetZ, float partialTicks, MatrixStack matrixStackIn,
                                    IRenderTypeBuffer bufferIn, int combinedLightIn, int combinedOverlayIn, boolean doOffset, boolean lie) {
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

        if (lie) {
            matrixStackIn.translate(0, -0.5, 0);
            matrixStackIn.rotate(Vector3f.XN.rotationDegrees(90));
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

        double area = calculateAreaOf(matrixStackIn.getLast().getMatrix(), p0, p1, p2, p3, partialTicks);

        if (area >= MIN_AREA) {

            IVertexBuilder builder = bufferIn.getBuffer(RenderType.getEntityTranslucent(location));

//        final float oldBias = GL11.glGetTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS);
//        final float oldMagFilter = GL11.glGetTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER);
//        final float oldMinFilter = GL11.glGetTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER);

            GlStateManager.texParameter(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, 0.0f);
            GlStateManager.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            GlStateManager.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);

            RenderHelper.blit3(builder, matrixStackIn,
                    0, 0, 0, 0, 0f,
                    w, h, h, w,
                    combinedLightIn);
        }

        matrixStackIn.pop();

        //restore the bias and filters
        //Bias apply to all textures

//        GlStateManager.texParameter(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, oldBias);
//        GlStateManager.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, oldMagFilter);
//        GlStateManager.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, oldMinFilter);
    }

    public static double MIN_AREA = 100.0;

    public static double calculateAreaOf(Matrix4f matrix, Vector4f point0, Vector4f point1, Vector4f point2, Vector4f point3, float partialTicks) {
        GameRenderer renderer = Minecraft.getInstance().gameRenderer;

        Matrix4f projectionMatrix = renderer.getProjectionMatrix(
                renderer.getActiveRenderInfo(), partialTicks, true);

        double y = Minecraft.getInstance().getMainWindow().getScaledHeight();
        double x = Minecraft.getInstance().getMainWindow().getScaledWidth();

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
        return new Vector3d(xScale * vector4f.getX() / vector4f.getW(), yScale * vector4f.getY() / vector4f.getW(), 0);
    }
}
