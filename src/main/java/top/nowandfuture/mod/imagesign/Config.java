package top.nowandfuture.mod.imagesign;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class Config {

    public static final String CATEGORY_MAIN = "main";

    public static ForgeConfigSpec.ConfigValue<String> PROXY_ADDRESS;
    public static ForgeConfigSpec.ConfigValue<Integer> PROXY_PORT;

    public static ForgeConfigSpec SERVER_CONFIG;
    public static ForgeConfigSpec CLIENT_CONFIG;

    private static final ForgeConfigSpec.ConfigValue<Integer> MAX_CACHE_SIZE;

    private static final ForgeConfigSpec.ConfigValue<Long> MAX_IMAGES_MEMORY;

    private static final ForgeConfigSpec.ConfigValue<Long> MAX_IMAGE_SIZE;

    private static final ForgeConfigSpec.ConfigValue<Integer> MAX_IMAGE_RENDER_COUNT;

    static {

        ForgeConfigSpec.Builder SERVER_BUILDER = new ForgeConfigSpec.Builder();
        ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();

        CLIENT_BUILDER.comment("Main settings").push(CATEGORY_MAIN);
        PROXY_ADDRESS       = CLIENT_BUILDER.comment("Proxy address").define("proxy"  ,  "");
        PROXY_PORT          = CLIENT_BUILDER.comment("Proxy port").define("port"  ,  0);
        MAX_CACHE_SIZE      = CLIENT_BUILDER.comment("ImageCache max size").define("cache_size"  ,  100);
        MAX_IMAGES_MEMORY = CLIENT_BUILDER.comment("Images max memory limit").define("limit"  ,  200L << 20);
        MAX_IMAGE_SIZE    = CLIENT_BUILDER.comment("Image max size").define("image_size"  ,  6L << 20);
        MAX_IMAGE_RENDER_COUNT    = CLIENT_BUILDER.comment("Max image count that render at one frame").define("render_count"  ,  100 >> 1);

        CLIENT_BUILDER.pop();

        SERVER_CONFIG = SERVER_BUILDER.build();
        CLIENT_CONFIG = CLIENT_BUILDER.build();
    }
}
