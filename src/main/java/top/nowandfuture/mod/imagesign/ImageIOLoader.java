package top.nowandfuture.mod.imagesign;

import top.nowandfuture.mod.imagesign.net.Proxy;
import top.nowandfuture.mod.imagesign.net.ProxyManager;
import top.nowandfuture.mod.imagesign.net.TrustAll;
import top.nowandfuture.mod.imagesign.utils.OkHttpUtil;
import com.icafe4j.image.ImageIO;
import com.icafe4j.image.ImageType;
import com.icafe4j.image.reader.GIFReader;
import com.icafe4j.image.reader.ImageReader;
import com.icafe4j.image.util.IMGUtils;
import com.icafe4j.io.PeekHeadInputStream;
import joptsimple.internal.Strings;
import okhttp3.OkHttpClient;

import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ImageIOLoader implements ImageLoader {
    private OkHttpClient client;
    private int retry = 3;


    public ImageIOLoader() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        client = builder
                .connectTimeout(500, TimeUnit.MILLISECONDS)
                .sslSocketFactory(TrustAll.socketFactory(),
                        new TrustAll.trustManager())
                .proxy(ProxyManager.INSTANCE.getProxy().getProxyIns())
                .build();
    }

    public void setProxy(Proxy proxy){
        client = client.newBuilder()
                .connectTimeout(500, TimeUnit.MILLISECONDS)
                .sslSocketFactory(TrustAll.socketFactory(),
                        new TrustAll.trustManager())
                .proxy(proxy.getProxyIns())
                .build();
    }


    @Override
    public ImageData load(Path path) throws Exception {
        ImageType format = guessImageType(path);

        if (format.equals(ImageType.GIF)) {
            ImageReader reader = ImageIO.getReader(ImageType.GIF);
            return get(reader, format.getExtension(), path.toFile());
        } else if (!format.equals(ImageType.UNKNOWN)) {
            try {
                BufferedImage bufferedImage = ImageIO.read(path.toFile());
                if (bufferedImage != null) {
                    return new ImageData(format.getExtension(), null, bufferedImage);
                }
            }catch (Exception ignored){

            }
            throw new RuntimeException("Image cannot be read by ImageIO, " +
                    "the format not supported or the image file is not completed.");
        }else {
            throw new RuntimeException("Unknown format of Image!");
        }
    }

    private ImageData get(ImageReader reader, String format, File file) throws Exception {
        try (InputStream inputStream = new FileInputStream(file)) {
            reader.read(inputStream);
            List<BufferedImage> bufferedImages = reader.getFrames();
            if (bufferedImages != null) {
                Object otherInfo = null;
                if (reader instanceof GIFReader) {
                    otherInfo = ((GIFReader) reader).getGIFFrames();
                }
                return new ImageData(format, otherInfo, bufferedImages);
            }
        }
        return null;
    }

    @Override
    public void save(ImageData image, Path path, String format) throws Exception {
        // TODO: 2021/8/19
    }

    @Override
    public File fetch(String url, File saveFile) throws Exception {
//        URL url2 = URI.create(url).toURL();
//        url2.openConnection(ProxyManager.INSTANCE.getProxy().getProxyIns());

        Files.deleteIfExists(saveFile.toPath());

        Exception last = null;

        File file = null;

        int temp = 0;
        long mills = 100;
        long maxWait = 5 * 1000;

        while (temp < retry) {
            try {
                boolean success = OkHttpUtil.downloadImage(client, url, saveFile);
                if (success) {
                    file = saveFile;
                    break;
                }
                temp++;
                //noinspection BusyWait
                Thread.sleep(Math.min(mills * (1L << temp), maxWait));
            } catch (Exception e2) {
                last = e2;
            }
        }

        if (temp >= retry) {
            if (last != null) {
                throw last;
            }
            throw new RuntimeException("Download "+ url +
                    " failed! Check the url or refresh the image again.");
        } else {
            return file;
        }
    }

    public String getImageType(Object obj) {
        try (ImageInputStream inputStream = javax.imageio.ImageIO.createImageInputStream(obj)) {
            Iterator<javax.imageio.ImageReader> imageReaderIterator = javax.imageio.ImageIO.getImageReaders(inputStream);
            if (imageReaderIterator.hasNext()) {
                javax.imageio.ImageReader reader = imageReaderIterator.next();
                return reader.getFormatName();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Strings.EMPTY;
    }

    public ImageType guessImageType(Path path) throws IOException {
        return IMGUtils.guessImageType(new PeekHeadInputStream(new FileInputStream(path.toFile()), 4));
    }

    public static void main(String[] args) {

    }
}
