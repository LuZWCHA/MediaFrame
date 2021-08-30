package top.nowandfuture.mod.imagesign.utils;

import net.minecraft.client.renderer.texture.NativeImage;
import org.lwjgl.BufferUtils;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Optional;

public class ImageUtils {

    public static void saveImage(BufferedImage image, Path savePath){

    }

    public static Optional<BufferedImage> loadImage(Path imagePath){


        return Optional.empty();
    }

    public static BufferedImage convert2RGBA(BufferedImage image) {
        ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);

        ColorModel colorModel = new ComponentColorModel(
                colorSpace, true, false, Transparency.TRANSLUCENT,
                DataBuffer.TYPE_BYTE);

        BufferedImageOp converter = new ColorConvertOp(colorSpace, null);
        BufferedImage newImage =
                converter.createCompatibleDestImage(image, colorModel);
        converter.filter(image, newImage);
        return newImage;
    }

    public static BufferedImage convert2RGB(BufferedImage image) {
        ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);

        ColorModel colorModel = new ComponentColorModel(
                colorSpace, false, false, Transparency.OPAQUE,
                DataBuffer.TYPE_BYTE);

        BufferedImageOp converter = new ColorConvertOp(colorSpace, null);
        BufferedImage newImage =
                converter.createCompatibleDestImage(image, colorModel);
        converter.filter(image, newImage);
        return newImage;
    }

    public BufferedImage convert2Gray(BufferedImage srcImg){
        return new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_sRGB), null).filter(srcImg, null);
    }

    public static BufferedImage scale(BufferedImage image, float scale, int interpolation) {
        if(interpolation < AffineTransformOp.TYPE_NEAREST_NEIGHBOR || interpolation > AffineTransformOp.TYPE_BICUBIC){
            interpolation = AffineTransformOp.TYPE_NEAREST_NEIGHBOR;
        }
        BufferedImageOp converter = new AffineTransformOp(AffineTransform.getScaleInstance(scale, scale), interpolation);
        return converter.filter(image, null);
    }


    public static BufferedImage copyImage(Image img, int imageType, Color backgroundColor) {
        final BufferedImage bufferedImage = new BufferedImage(img.getWidth(null), img.getHeight(null), imageType);
        final Graphics2D bGr = createGraphics(bufferedImage, backgroundColor);
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        return bufferedImage;
    }

    public static Graphics2D createGraphics(BufferedImage image, Color color) {
        final Graphics2D graphics = image.createGraphics();

        if (null != color) {
            graphics.setColor(color);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        }

        return graphics;
    }

    public static NativeImage createMinecraftImage(BufferedImage image) throws IOException {
        NativeImage nativeImage = new NativeImage(image.getWidth(), image.getHeight(), true);
        ByteBuffer buffer = BufferUtils.createByteBuffer(image.getHeight() * image.getWidth() * 4);
        buffer.put(((DataBufferByte) (image.getData().getDataBuffer())).getData());
        return NativeImage.read(NativeImage.PixelFormat.RGBA, buffer);
    }
}
