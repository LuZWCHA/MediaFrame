package top.nowandfuture.mod.imagesign.utils;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.imageio.stream.FileCacheImageInputStream;
import java.io.*;
import java.util.Objects;

public class OkHttpUtil {

    public static boolean downloadImage(OkHttpClient okHttpClient, String url, File file) throws Exception {

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
}