package top.nowandfuture.mod.imagesign.mixin;


import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(SignEditScreen.class)
public abstract class MixinEditSignScreen extends Screen {

    @Shadow
    private TextFieldHelper signField;
    @Final
    @Shadow
    private String[] messages;
    @Shadow
    private int line;
    @Shadow
    @Final
    private SignBlockEntity sign;

    protected MixinEditSignScreen(Component pTitle) {
        super(pTitle);
    }


    @Inject(method = "init",
            at = @At("TAIL"),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    public void inject_init(CallbackInfo callbackInfo) {
        this.signField = new TextFieldHelper(() -> this.messages[this.line], (p_169824_) -> {
            this.messages[this.line] = p_169824_;
            this.sign.setMessage(this.line, new TextComponent(p_169824_));
        }, TextFieldHelper.createClipboardGetter(this.minecraft),
                TextFieldHelper.createClipboardSetter(this.minecraft), (p_169822_) -> true);
    }

}
