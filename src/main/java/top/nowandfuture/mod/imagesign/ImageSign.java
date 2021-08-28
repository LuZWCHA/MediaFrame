package top.nowandfuture.mod.imagesign;

import com.mojang.blaze3d.systems.IRenderCall;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.nowandfuture.mod.imagesign.loader.ImageFetcher;
import top.nowandfuture.mod.imagesign.loader.SignImageLoadManager;
import top.nowandfuture.mod.imagesign.net.Proxy;
import top.nowandfuture.mod.imagesign.net.ProxyManager;
import top.nowandfuture.mod.imagesign.setup.ClientProxy;
import top.nowandfuture.mod.imagesign.setup.CommonProxy;
import top.nowandfuture.mod.imagesign.setup.IProxy;
import top.nowandfuture.mod.imagesign.utils.OptiFineHelper;

import java.util.stream.Collectors;

@Mod("imagesign")
public class ImageSign {
    public static ImageSign INSTANCE;
    public static String PROXY_ADDRESS;
    private static Integer PROXY_PORT;
    private static IProxy proxy;
    private static final Logger LOGGER = LogManager.getLogger();


    public ImageSign(){
        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_CONFIG);
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        // Register the doClientStuff method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
        RenderQueue.init(50, 2);
        proxy = DistExecutor.safeRunForDist(() -> ClientProxy::new, () -> CommonProxy::new);
        INSTANCE = this;
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        // some preinit code
        proxy.setup(event);
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        // do something that can only be done on the client
        proxy.doClientStuff(event);
    }

    @SubscribeEvent
    public void onLoad(final ModConfig.Loading configEvent) {
        loadConfig();
    }

    @SubscribeEvent
    public void onReload(final ModConfig.Reloading configEvent) {
        loadConfig();
    }

    public void loadConfig(){
        PROXY_ADDRESS = Config.PROXY_ADDRESS.get();
        PROXY_PORT = Config.PROXY_PORT.get();

        // TODO: 2021/8/24 socks

        ProxyManager.INSTANCE.setProxy(new Proxy(PROXY_ADDRESS, PROXY_PORT, "",0,"",0));
    }
}
