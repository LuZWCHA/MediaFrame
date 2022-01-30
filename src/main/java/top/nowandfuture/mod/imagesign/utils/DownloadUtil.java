package top.nowandfuture.mod.imagesign.utils;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.http.client.methods.RequestBuilder;
import top.nowandfuture.mod.imagesign.Config;
import top.nowandfuture.mod.imagesign.net.ProxyManager;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;

public class DownloadUtil {

    public interface IDownloadListener {
        void onProgress(long p, long total);

        void onStart(String url);

        void onSuccess(String url);

        void onFailed(Throwable t, String url);
    }

    public static class DownloadListener implements IDownloadListener {

        @Override
        public void onProgress(long p, long total) {

        }

        @Override
        public void onStart(String url) {

        }

        @Override
        public void onSuccess(String url) {

        }

        @Override
        public void onFailed(Throwable t, String url) {

        }
    }

    private static String CUSTOM_USER_AGENT = null;

    public static boolean downloadImage(OkHttpClient okHttpClient, String url, File file, @Nullable IDownloadListener downloadListener) throws Exception {

        if (downloadListener == null) {
            downloadListener = new DownloadListener();
        }

        if (CUSTOM_USER_AGENT == null) {
            CUSTOM_USER_AGENT = Config.HEADER_USER_AGENT.get();
        }

        if (url.startsWith("file://")) return downloadImage(url, file, downloadListener);

        Request.Builder requestBuilder = new Request.Builder().url(url);

        if(!CUSTOM_USER_AGENT.isEmpty()){
            requestBuilder.addHeader("User-Agent", CUSTOM_USER_AGENT);
        }

        Call call = okHttpClient.newCall(requestBuilder.build());

        downloadListener.onStart(url);

        try (Response response = call.execute();
             InputStream is = Objects.requireNonNull(response.body()).byteStream();
             FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            if (response.isSuccessful()) {
                long total = Objects.requireNonNull(response.body()).contentLength();
                byte[] bytes = new byte[2048];
                int p = 0;
                int count;
                while ((count = is.read(bytes)) != -1) {
                    fileOutputStream.write(bytes, 0, count);
                    p += count;
                    downloadListener.onProgress(p, total);
                }
            } else {
                throw new RuntimeException(response.code() + ": " + response.message());
            }
        } catch (Exception e) {
            file.delete();
            downloadListener.onFailed(e, url);
            return false;
        }
        downloadListener.onSuccess(url);
        return true;
    }

    public static boolean downloadImage(String url, File file, @Nullable IDownloadListener downloadListener) throws Exception {

        if (downloadListener == null) {
            downloadListener = new DownloadListener();
        }

        url = url.replaceAll("\\\\", "/");
        URL url2 = URI.create(url).toURL();

        downloadListener.onStart(url);

        URLConnection connection = url2.openConnection(ProxyManager.INSTANCE.getProxy().getProxyIns());

        try (InputStream is = connection.getInputStream();
             FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            byte[] bytes = new byte[2048];
            int p = 0;
            int count;
            long total = connection.getContentLengthLong();

            while ((count = is.read(bytes)) != -1) {
                fileOutputStream.write(bytes, 0, count);
                p += count;
                downloadListener.onProgress(p, total);
            }
        } catch (Exception e) {
            file.delete();
            downloadListener.onFailed(e, url);
            return false;
        }

        downloadListener.onSuccess(url);
        return true;

    }
}