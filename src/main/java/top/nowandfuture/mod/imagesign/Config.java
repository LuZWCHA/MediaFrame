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
    public static final ForgeConfigSpec.ConfigValue<Long> CALL_TIMEOUT;

    public static ForgeConfigSpec SERVER_CONFIG;
    public static ForgeConfigSpec CLIENT_CONFIG;


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
        CALL_TIMEOUT = CLIENT_BUILDER.comment("Request total timeout limit (millisecond). Recommend: more than 1000ms on the internet.").define("call_timeout", 2000L);

        CLIENT_BUILDER.pop();

        SERVER_CONFIG = SERVER_BUILDER.build();
        CLIENT_CONFIG = CLIENT_BUILDER.build();
    }
}
