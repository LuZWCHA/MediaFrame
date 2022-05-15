package top.nowandfuture.mod.imagesign.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface IGameRendererAccessor {
    @Invoker("getFov")
    double invokeGetFov(Camera pActiveRenderInfo, float pPartialTicks, boolean pUseFOVSetting);

}
