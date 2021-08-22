package top.nowandfuture.mod.imagesign;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.Objects;

@Mod("imagesign")
public class ImageSign {
    public static ImageSign INSTANCE;

    public ImageSign(){
        MinecraftForge.EVENT_BUS.register(this);

        INSTANCE = this;
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        // do something when the server starts
        if(event.getWorld().isRemote()){
            SignImageLoadManager.INSTANCE.clear(event.getWorld());
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
            }
        }
    }
}
