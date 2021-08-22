package top.nowandfuture.mod.imagesign;

import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator;

import javax.annotation.Nullable;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

public interface ImageLoader {

    ImageData load(Path path) throws Exception;
    void save(ImageData image, Path path, String format) throws Exception;
    File fetch(String url, File saveFile) throws Exception;

    class ImageInfo{
        private final String imageFormat;
        private long size;
        private final Object pram;

        public ImageInfo(String imageFormat, long size, Object pram) {
            this.imageFormat = imageFormat;
            this.size = size;
            this.pram = pram;
        }

        public String getImageFormat() {
            return imageFormat;
        }

        public long getSize() {
            return size;
        }

        public Object getPram() {
            return pram;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    class ImageData{
        protected BufferedImage[] images;
        private final ImageInfo imageInfo;

        public String getImageFormat(){
            return imageInfo.imageFormat;
        }

        public ImageInfo getImageInfo() {
            return imageInfo;
        }

        public ImageData(String format, @Nullable Object pram, BufferedImage... images){
            this.images = images;
            this.imageInfo = new ImageInfo(format, checkMemory(images), pram);
        }

        public ImageData(String format, @Nullable Object pram, List<BufferedImage> images){
            this(format, pram, (BufferedImage[]) images.toArray());
        }

        private long checkMemory(Object obj){
            long size = ObjectSizeCalculator.getObjectSize(obj);
            return size;
        }

    }
}
