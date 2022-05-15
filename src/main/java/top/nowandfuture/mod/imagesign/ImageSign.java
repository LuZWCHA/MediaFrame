package top.nowandfuture.mod.imagesign;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.nowandfuture.mod.imagesign.caches.ImageEntity;
import top.nowandfuture.mod.imagesign.caches.ImageEntityCache;
import top.nowandfuture.mod.imagesign.loader.ImageFetcher;
import top.nowandfuture.mod.imagesign.loader.ImageIOLoader;
import top.nowandfuture.mod.imagesign.net.Proxy;
import top.nowandfuture.mod.imagesign.net.ProxyManager;
import top.nowandfuture.mod.imagesign.setup.ClientProxy;
import top.nowandfuture.mod.imagesign.setup.CommonProxy;
import top.nowandfuture.mod.imagesign.setup.IProxy;
import top.nowandfuture.mod.imagesign.utils.RenderHelper;
import top.nowandfuture.mod.imagesign.utils.Utils;

@Mod("imagesign")
public class ImageSign {
    public static ImageSign INSTANCE;

    private static String PROXY_ADDRESS;
    private static Integer PROXY_PORT;
    private static String TEMP_DIR = "image_temps";
    private static String TEMP_DIR_2 = "image_temps2";

    private static IProxy proxy;
    public static final Logger LOGGER = LogManager.getLogger();


    public ImageSign() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_CONFIG);
        MinecraftForge.EVENT_BUS.register(this);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onLoad);
        RenderQueue.init(50, 2);
        proxy = DistExecutor.safeRunForDist(() -> ClientProxy::new, () -> CommonProxy::new);
        INSTANCE = this;
    }

    private void setup(final FMLCommonSetupEvent event) {
        // some pre-init code
        proxy.setup(event);
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        // do something that can only be done on the client
        proxy.doClientStuff(event);
    }

    @SubscribeEvent
    public void onLoad(final ModConfigEvent.Loading configEvent) {
        loadConfig();
    }

    @SubscribeEvent
    public void onReload(final ModConfigEvent.Reloading configEvent) {
        loadConfig();
    }

    public void loadConfig() {
        PROXY_ADDRESS = Config.PROXY_ADDRESS.get();
        PROXY_PORT = Config.PROXY_PORT.get();
        int CALL_TIMEOUT = Config.CALL_TIMEOUT.get();
        RenderHelper.MIN_AREA = Config.MIN_RENDER_IMAGE_AREA.get();

        ImageIOLoader.setCallTimeOut(CALL_TIMEOUT);

        RenderQueue.setMaxRenderObjCount(Config.MAX_IMAGE_RENDER_COUNT.get());
        // TODO: 2022/1/19 socks proxy next update will added.
        ProxyManager.INSTANCE.setProxy(new Proxy(PROXY_ADDRESS, PROXY_PORT, PROXY_ADDRESS, PROXY_PORT, PROXY_ADDRESS, PROXY_PORT));
        ImageFetcher.CacheSetting cacheSetting = new ImageFetcher.CacheSetting(
                Config.MAX_CACHE_SIZE.get(), Config.MAX_IMAGES_MEMORY.get(), Config.MAX_IMAGE_SIZE.get(),
                Minecraft.getInstance().gameDirectory.getAbsolutePath(), TEMP_DIR, TEMP_DIR_2
        );
        ImageFetcher.INSTANCE.init(cacheSetting);

        ImageFetcher.INSTANCE.setCacheListener(new ImageEntityCache.CacheChangeListener() {
            @Override
            public void remove(ImageEntity imageEntity, long... positions) {
                RenderSystem.recordRenderCall(() -> {
                    if (positions == null || positions.length == 0) {
                        for (int i = 0; i < imageEntity.getOrgImages().size(); i++) {
                            ResourceLocation location = new ResourceLocation(
                                    Utils.urlToByteString(imageEntity.url),
                                    String.valueOf(i)
                            );
                            Minecraft.getInstance().getTextureManager().release(location);
                        }

                    }
                });

            }

            @Override
            public void add(ImageEntity imageEntity, long... positions) {

            }
        });
    }
}
