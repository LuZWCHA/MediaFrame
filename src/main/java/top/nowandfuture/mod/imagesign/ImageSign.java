package top.nowandfuture.mod.imagesign;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.resources.IResourceManagerReloadListener;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLLoader;
import top.nowandfuture.mod.imagesign.caches.ImageEntity;
import top.nowandfuture.mod.imagesign.loader.ImageFetcher;
import top.nowandfuture.mod.imagesign.loader.SignImageLoadManager;
import top.nowandfuture.mod.imagesign.net.Proxy;
import top.nowandfuture.mod.imagesign.net.ProxyManager;
import top.nowandfuture.mod.imagesign.utils.OptiFineHelper;
import top.nowandfuture.mod.imagesign.utils.RenderHelper;

@Mod("imagesign")
public class ImageSign {
    public static ImageSign INSTANCE;
    public static String PROXY_ADDRESS;
    private static Integer PROXY_PORT;

    public ImageSign(){
        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_CONFIG);

        INSTANCE = this;
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

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        // do something when the server starts
        if(event.getWorld().isRemote()){
            SignImageLoadManager.INSTANCE.clear(event.getWorld());
            ImageFetcher.INSTANCE.dispose();
        }
    }

    private static boolean lastShader;
    private static int lastCountReset = -1;

    public static boolean getShaderLoaded(){
        return lastShader;
    }

    @SubscribeEvent
    public void onClickOnBlock(PlayerInteractEvent.RightClickBlock rightClickBlock){
        World world = rightClickBlock.getWorld();
        BlockPos blockPos = rightClickBlock.getPos();
        boolean refresh = rightClickBlock.getItemStack().isEmpty();
        if(world.isRemote() && world.getBlockState(blockPos).getBlock() instanceof AbstractSignBlock){
            boolean hasTe = world.getBlockState(blockPos).hasTileEntity();
            if(hasTe){
                if(refresh)
                    ImageFetcher.INSTANCE.reRender(blockPos);
                else{
//                    SignImageLoadManager.INSTANCE.clear(world);
                    ImageFetcher.INSTANCE.refresh(blockPos);
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void oClientTick(TickEvent event) {
        // do something when the server starts
        if(event.phase == TickEvent.Phase.START && event.side == LogicalSide.CLIENT){
            Entity entity = Minecraft.getInstance().getRenderViewEntity();
            if(entity != null) {
                ImageFetcher.INSTANCE.onTick(entity.getPositionVec());
                if(OptiFineHelper.isLoaded()){
                    boolean cur = OptiFineHelper.isShaderLoaded();
                    if(cur != lastShader){
                        lastShader = cur;
                    }

                    int count = OptiFineHelper.getResetDisplayListsCount();

                    if(count >= 0 && lastCountReset != count){
                        lastCountReset = count;
                        SignImageLoadManager.INSTANCE.clear(entity.world);
                        ImageFetcher.INSTANCE.dispose();
                    }
                }
            }
        }
    }
}
