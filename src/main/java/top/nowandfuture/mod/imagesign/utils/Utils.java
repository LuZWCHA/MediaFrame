package top.nowandfuture.mod.imagesign.utils;

import joptsimple.internal.Strings;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

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

    public static void main(String[] args) {
        String en = md5("https://blog.csdn.net/u011781521/article/details/77932321https://blog.csdn.net/u011781521/article/details/77932321https://blog.csdn.net/u011781521/article/details/77932321https://blog.csdn.net/u011781521/article/details/77932321");
//        String de = decryptAES(en);

        System.out.println(en);
    }
}
