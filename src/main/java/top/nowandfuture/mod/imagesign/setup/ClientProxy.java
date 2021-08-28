package top.nowandfuture.mod.imagesign.setup;

import com.mojang.blaze3d.systems.IRenderCall;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import top.nowandfuture.mod.imagesign.RenderQueue;
import top.nowandfuture.mod.imagesign.caches.ImageEntity;
import top.nowandfuture.mod.imagesign.caches.ImageEntityCache;
import top.nowandfuture.mod.imagesign.loader.ImageFetcher;
import top.nowandfuture.mod.imagesign.loader.SignImageLoadManager;
import top.nowandfuture.mod.imagesign.utils.OptiFineHelper;

@OnlyIn(Dist.CLIENT)
public class ClientProxy extends CommonProxy {

    public ClientProxy(){

    }

    @Override
    public void setup(FMLCommonSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        ImageFetcher.INSTANCE.setCacheListener(new ImageEntityCache.CacheChangeListener() {
            @Override
            public void remove(ImageEntity imageEntity, BlockPos... positions) {
                for (BlockPos position : positions) {
                    ResourceLocation location = new ResourceLocation(
                            String.valueOf(position.toLong())
                    );
                    Minecraft.getInstance().getTextureManager().deleteTexture(location);
                }
            }

            @Override
            public void add(ImageEntity imageEntity, BlockPos... positions) {

            }
        });
    }

    @SubscribeEvent
    public void onWorldLastRender(RenderWorldLastEvent renderWorldLastEvent) {
        RenderQueue.doTasks();
        RenderQueue.updateQuerySet();
        RenderQueue.FRAME_COUNT ++;
    }

    private static boolean lastShader;
    private static int lastCountReset = -1;

    public static boolean getShaderLoaded(){
        return lastShader;
    }

    @SubscribeEvent
    public void oClientTick(TickEvent event) {
        // do something when the server starts
        if(event.phase == TickEvent.Phase.START){
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

                        RenderSystem.recordRenderCall(new IRenderCall() {
                            @Override
                            public void execute() {
                                SignImageLoadManager.INSTANCE.clear(entity.world);
                                ImageFetcher.INSTANCE.reload();
                            }
                        });

                    }
                }
            }
        }
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
                    SignImageLoadManager.INSTANCE.clear(world);
                    ImageFetcher.INSTANCE.refresh(blockPos);
                }
            }
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        // do something when the server starts
        if(event.getWorld().isRemote()){
            SignImageLoadManager.INSTANCE.clear(event.getWorld());
            ImageFetcher.INSTANCE.dispose();
            RenderQueue.clearQueue();
        }
    }

    @Override
    public void doClientStuff(FMLClientSetupEvent event) {

    }
}
