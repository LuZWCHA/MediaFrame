package top.nowandfuture.mod.imagesign.setup;

import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public interface IProxy {

    void setup(final FMLCommonSetupEvent event);
    void doClientStuff(final FMLClientSetupEvent event);

}
