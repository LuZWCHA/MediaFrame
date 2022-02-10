package top.nowandfuture.mod.imagesign.loader;

import com.icafe4j.image.gif.GIFFrame;
import io.reactivex.rxjava3.annotations.NonNull;
import org.jetbrains.annotations.NotNull;
import top.nowandfuture.mod.imagesign.caches.GIFParam;
import top.nowandfuture.mod.imagesign.caches.IParam;
import top.nowandfuture.mod.imagesign.net.Proxy;
import top.nowandfuture.mod.imagesign.net.ProxyManager;
import top.nowandfuture.mod.imagesign.net.TrustAll;
import top.nowandfuture.mod.imagesign.utils.DownloadUtil;
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
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ImageIOLoader implements ImageLoader {
    private OkHttpClient client;
    private static int RETRY_COUNT = 3;
    private static long CALL_TIME_OUT = 2000;

    public static void setCallTimeOut(long callTimeOut) {
        CALL_TIME_OUT = callTimeOut;
    }

    public ImageIOLoader() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        client = builder
                .callTimeout(CALL_TIME_OUT, TimeUnit.MILLISECONDS)
                .sslSocketFactory(TrustAll.socketFactory(),
                        new TrustAll.trustManager())
                .proxy(ProxyManager.INSTANCE.getProxy().getProxyIns())
                .build();
    }

    public void setProxy(Proxy proxy) {
        client = client.newBuilder()
                .connectTimeout(CALL_TIME_OUT, TimeUnit.MILLISECONDS)
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
            BufferedImage bufferedImage;
            try (FileInputStream fileInputStream = new FileInputStream(path.toFile())) {
                bufferedImage = ImageIO.read(fileInputStream);
            }
            if (bufferedImage != null) {
                return new ImageData(format.getExtension(), null, bufferedImage);
            }
            throw new RuntimeException("Image cannot be read by ImageIO, " +
                    "the format not supported or the image file is not completed.");
        } else {
            throw new RuntimeException("Unknown format of Image!");
        }
    }

    private ImageData get(ImageReader reader, String format, File file) throws Exception {
        ImageData res = null;
        try (InputStream inputStream = new FileInputStream(file)) {
            reader.read(inputStream);
            List<BufferedImage> bufferedImages = reader.getFrames();
            if (bufferedImages != null) {
                IParam otherInfo = null;
                if (reader instanceof GIFReader) {
                    if (!((GIFReader) reader).getGIFFrames().isEmpty()) {
                        GIFFrame frame = ((GIFReader) reader).getGIFFrame(0);
                        otherInfo = GIFParam.Builder.newBuild(frame.getFrameWidth(), frame.getFrameHeight(), frame.getLeftPosition(), frame.getTopPosition(), frame.getDelay(), frame.getTransparentColor())
                                .setDisposalMethod(frame.getDisposalMethod())
                                .setTransparencyFlag(frame.getTransparencyFlag())
                                .setUserInputFlag(frame.getUserInputFlag())
                                .build();

                    }
                }
                res = new ImageData(format, otherInfo, bufferedImages);
            }
        }
        return res;
    }

    @Override
    public void save(ImageData image, Path path, String format) throws Exception {
        //do nothing
        //we don't need to save the image as a file now, may be useful in the future.
    }

    @Override
    public File fetch(String url, File saveFile,@NotNull DownloadUtil.IDownloadListener downloadListener) throws Exception {

        Files.deleteIfExists(saveFile.toPath());

        Exception last = null;

        File file = null;

        int temp = 0;
        final long mills = 100;
        final long maxWait = 5 * 1000;

        while (temp < RETRY_COUNT) {
            try {

                boolean success = DownloadUtil.downloadImage(client, url, saveFile, downloadListener);
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

        if (temp >= RETRY_COUNT) {
            if (last != null) {
                throw last;
            }
            throw new RuntimeException("Download " + url +
                    " failed! Check the url or refresh the image again.");
        } else {
            return file;
        }
    }

    @Deprecated
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
}
