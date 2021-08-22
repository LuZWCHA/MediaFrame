package top.nowandfuture.mod.imagesign.utils;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
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

    public BufferedImage toGray(BufferedImage srcImg){
        return new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_sRGB), null).filter(srcImg, null);
    }

    public static BufferedImage scale(BufferedImage image, float scale) {
        BufferedImageOp converter = new RescaleOp(scale, 0, null);
        return converter.filter(image, null);
    }

    /**
     * 将已有Image复制新的一份出来
     *
     * @param img             {@link Image}
     * @param imageType       目标图片类型，{@link BufferedImage}中的常量，例如黑白等
     * @param backgroundColor 背景色，{@code null} 表示默认背景色（黑色或者透明）
     * @return {@link BufferedImage}
     * @see BufferedImage#TYPE_INT_RGB
     * @see BufferedImage#TYPE_INT_ARGB
     * @see BufferedImage#TYPE_INT_ARGB_PRE
     * @see BufferedImage#TYPE_INT_BGR
     * @see BufferedImage#TYPE_3BYTE_BGR
     * @see BufferedImage#TYPE_4BYTE_ABGR
     * @see BufferedImage#TYPE_4BYTE_ABGR_PRE
     * @see BufferedImage#TYPE_BYTE_GRAY
     * @see BufferedImage#TYPE_USHORT_GRAY
     * @see BufferedImage#TYPE_BYTE_BINARY
     * @see BufferedImage#TYPE_BYTE_INDEXED
     * @see BufferedImage#TYPE_USHORT_565_RGB
     * @see BufferedImage#TYPE_USHORT_555_RGB
     * @since 4.5.17
     */
    public static BufferedImage copyImage(Image img, int imageType, Color backgroundColor) {
        final BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), imageType);
        final Graphics2D bGr = createGraphics(bimage, backgroundColor);
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        return bimage;
    }

    /**
     * 创建{@link Graphics2D}
     *
     * @param image {@link BufferedImage}
     * @param color {@link Color}背景颜色以及当前画笔颜色，{@code null}表示不设置背景色
     * @return {@link Graphics2D}
     * @since 4.5.2
     */
    public static Graphics2D createGraphics(BufferedImage image, Color color) {
        final Graphics2D g = image.createGraphics();

        if (null != color) {
            // 填充背景
            g.setColor(color);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
        }

        return g;
    }
}
