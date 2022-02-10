package top.nowandfuture.mod.imagesign.mixin;

import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;

public class MCMFConnector implements IMixinConnector {
    @Override
    public void connect() {
        Mixins.addConfiguration("mixins.imagesign.json");
    }
}
