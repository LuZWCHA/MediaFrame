package top.nowandfuture.mod.imagesign;

import joptsimple.internal.Strings;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

import java.util.function.Predicate;
import java.util.regex.Pattern;

@Mod.EventBusSubscriber
public class Config {

    public static final String CATEGORY_MAIN = "main";

    public static final ForgeConfigSpec.ConfigValue<String> PROXY_ADDRESS;
    public static final ForgeConfigSpec.ConfigValue<Integer> PROXY_PORT;
    public static final ForgeConfigSpec.ConfigValue<Integer> MAX_CACHE_SIZE;
    public static final ForgeConfigSpec.ConfigValue<Long> MAX_IMAGES_MEMORY;
    public static final ForgeConfigSpec.ConfigValue<Long> MAX_IMAGE_SIZE;
    public static final ForgeConfigSpec.ConfigValue<Integer> MAX_IMAGE_RENDER_COUNT;
    public static final ForgeConfigSpec.ConfigValue<Integer> CALL_TIMEOUT;
    public static final ForgeConfigSpec.ConfigValue<String> HEADER_USER_AGENT;
    public static final ForgeConfigSpec.ConfigValue<Double> MIN_RENDER_IMAGE_AREA;

    public static ForgeConfigSpec SERVER_CONFIG;
    public static ForgeConfigSpec CLIENT_CONFIG;

    private final static String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36";


    static {

        ForgeConfigSpec.Builder SERVER_BUILDER = new ForgeConfigSpec.Builder();
        ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();

        CLIENT_BUILDER.comment("Main settings").push(CATEGORY_MAIN);
        PROXY_ADDRESS = CLIENT_BUILDER.comment("Local-Proxy address.For example: 127.0.0.1 .").define("proxy_address", Strings.EMPTY, new Predicate<Object>() {
            @Override
            public boolean test(Object o) {
                if(o instanceof String){
                    return ((String) o).isEmpty() || isIPv4((String) o) || isIPv6((String) o);
                }
                return false;
            }

            private final Pattern IPV4_REGEX_NORMAL = Pattern.compile(
                    "((2[0-4]\\d|25[0-5]|[01]?\\d\\d?)\\.){3}(2[0-4]\\d|25[0-5]|[01]?\\d\\d?)");

            private final Pattern IPV6_REGEX_NORMAL = Pattern.compile(
                    "((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:)))(%.+)?");

            private boolean isIPv4(String ip){
                return IPV4_REGEX_NORMAL.matcher(ip).matches();
            }

            private boolean isIPv6(String ip){
                return IPV6_REGEX_NORMAL.matcher(ip).matches();
            }
        });
        PROXY_PORT = CLIENT_BUILDER.comment("Local-Proxy port.").defineInRange("proxy_port", 0, 0, 65565);
        MAX_CACHE_SIZE = CLIENT_BUILDER.comment("ImageCache max size.").defineInRange("cache_size", 100, 10, Integer.MAX_VALUE);
        MAX_IMAGES_MEMORY = CLIENT_BUILDER.comment("Images max memory limit (byte).").defineInRange("image_memory_limit", 200L << 20, 1 << 10, Long.MAX_VALUE);
        MAX_IMAGE_SIZE = CLIENT_BUILDER.comment("Image max size (byte).").defineInRange("image_size", 6L << 20, 1 << 10, Long.MAX_VALUE);
        MAX_IMAGE_RENDER_COUNT = CLIENT_BUILDER.comment("Max image count that will be render at one frame.").defineInRange("max_render_count", 50, 1, Integer.MAX_VALUE);
        CALL_TIMEOUT = CLIENT_BUILDER.comment("Request total timeout limit (millisecond). Recommend: more than 1000ms on the internet.").define("call_timeout", 2000);
        HEADER_USER_AGENT = CLIENT_BUILDER.comment("Custom User-Agent for downloading images. " +
                "For example: \"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36\", " +
                "you can also get one from https://fake-useragent.herokuapp.com/browsers/0.1.8, provide by fake-agent")
                .define("custom_user_agent", DEFAULT_USER_AGENT);
        MIN_RENDER_IMAGE_AREA = CLIENT_BUILDER.comment("The minimum area of the image render on the screen, if the area is less than the setting, the image will not render").defineInRange("min_area", 100.0, 1, 1024 * 1024 * 8);

        CLIENT_BUILDER.pop();

        SERVER_CONFIG = SERVER_BUILDER.build();
        CLIENT_CONFIG = CLIENT_BUILDER.build();
    }
}
