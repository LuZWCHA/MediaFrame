package top.nowandfuture.mod.imagesign.loader;

import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator;
import top.nowandfuture.mod.imagesign.caches.IParam;

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

        public BufferedImage[] getImages() {
            return images;
        }

        public ImageInfo getImageInfo() {
            return imageInfo;
        }

        public ImageData(String format, @Nullable IParam obj, BufferedImage... images){
            this.images = images;
            this.imageInfo = new ImageInfo(format, checkMemory(images), obj);
        }

        public ImageData(String format, @Nullable IParam obj, List<BufferedImage> images){
            this(format, obj, images.toArray(new BufferedImage[0]));
        }

        private long checkMemory(Object obj){
            return ObjectSizeCalculator.getObjectSize(obj);
        }

    }
}
