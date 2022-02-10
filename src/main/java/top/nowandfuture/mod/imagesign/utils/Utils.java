package top.nowandfuture.mod.imagesign.utils;

import org.apache.commons.codec.digest.DigestUtils;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Utils {
    public static String md5(String data) {
        return DigestUtils.md5Hex(data);
    }

    public static String urlEncode(String url) {
        return URLEncoder.encode(url, StandardCharsets.UTF_8);
    }

    public static String urlDecode(String url) {
        return URLDecoder.decode(url, StandardCharsets.UTF_8);
    }

    public static String urlToByteString(String url){
        StringBuilder stringBuilder = new StringBuilder();
        for (byte c : url.getBytes(StandardCharsets.UTF_8)) {
            stringBuilder.append(c);
        }
        return stringBuilder.toString();
    }

}
