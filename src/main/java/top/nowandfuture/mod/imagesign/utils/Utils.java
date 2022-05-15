package top.nowandfuture.mod.imagesign.utils;

import joptsimple.internal.Strings;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Utils {
    public static String md5(String data) {
        return DigestUtils.md5Hex(data);
    }

    public static String urlEncode(String url) {
        try {
            return URLEncoder.encode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return Strings.EMPTY;
        }
    }

    public static String urlDecode(String url) {
        try {
            return URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return Strings.EMPTY;
        }
    }

    public static String urlToByteString(String url){
        StringBuilder stringBuilder = new StringBuilder();
        for (byte c : url.getBytes(StandardCharsets.UTF_8)) {
            stringBuilder.append(c);
        }
        return stringBuilder.toString();
    }


}
