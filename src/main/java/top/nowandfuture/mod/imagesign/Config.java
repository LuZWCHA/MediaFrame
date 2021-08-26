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

    static {

        ForgeConfigSpec.Builder SERVER_BUILDER = new ForgeConfigSpec.Builder();
        ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();

        CLIENT_BUILDER.comment("Main settings").push(CATEGORY_MAIN);
        PROXY_ADDRESS           = CLIENT_BUILDER.comment("Proxy address").define("proxy"  ,  "");
        PROXY_PORT           = CLIENT_BUILDER.comment("Proxy port").define("port"  ,  0);
        CLIENT_BUILDER.pop();

        SERVER_CONFIG = SERVER_BUILDER.build();
        CLIENT_CONFIG = CLIENT_BUILDER.build();
    }
}
