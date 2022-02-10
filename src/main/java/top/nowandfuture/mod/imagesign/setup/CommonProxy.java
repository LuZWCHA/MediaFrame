package top.nowandfuture.mod.imagesign.setup;

import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class CommonProxy implements IProxy {
    @Override
    public void setup(FMLCommonSetupEvent event) {

    }

    @Override
    public void doClientStuff(FMLClientSetupEvent event) {
        throw new RuntimeException("Server should not invoke the method!");
    }
}
