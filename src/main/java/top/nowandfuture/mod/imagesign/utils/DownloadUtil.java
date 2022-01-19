package top.nowandfuture.mod.imagesign.utils;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import top.nowandfuture.mod.imagesign.net.ProxyManager;

import javax.imageio.stream.FileCacheImageInputStream;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;

public class DownloadUtil {

    public static boolean downloadImage(OkHttpClient okHttpClient, String url, File file) throws Exception {

        if(url.startsWith("file://")) return downloadImage(url, file);

        Request request = new Request.Builder().url(url).build();
        Call call = okHttpClient.newCall(request);

        try (Response response = call.execute();
             InputStream is = Objects.requireNonNull(response.body()).byteStream();
             FileOutputStream fileOutputStream = new FileOutputStream(file)){
            if(response.isSuccessful()) {
                byte[] bytes = new byte[2048];
                int count;
                while ((count = is.read(bytes)) != -1) {
                    fileOutputStream.write(bytes, 0, count);
                }
            }else{
                return false;
            }
        }catch (Exception e){
            return false;
        }

        return true;

    }

    public static boolean downloadImage(String url, File file) throws Exception {
        url = url.replaceAll("\\\\", "/");
        URL url2 = URI.create(url).toURL();
        URLConnection connection = url2.openConnection(ProxyManager.INSTANCE.getProxy().getProxyIns());

        try (InputStream is = connection.getInputStream();
             FileOutputStream fileOutputStream = new FileOutputStream(file)){
            byte[] bytes = new byte[2048];
            int count;
            while ((count = is.read(bytes)) != -1) {
                fileOutputStream.write(bytes, 0, count);
            }
        }catch (Exception e){
            return false;
        }

        return true;

    }
}