package top.nowandfuture.mod.imagesign.mixin;

import net.minecraft.client.gui.fonts.TextInputUtil;
import net.minecraft.client.gui.screen.EditSignScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.tileentity.SignTileEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(EditSignScreen.class)
public abstract class MixinEditSignScreen extends Screen {

    @Shadow
    private TextInputUtil textInputUtil;
    @Final
    @Shadow
    private String[] field_238846_r_;
    @Shadow
    private int editLine;
    @Shadow
    @Final
    private SignTileEntity tileSign;

    protected MixinEditSignScreen(ITextComponent titleIn) {
        super(titleIn);
    }

    @Inject(method = "init",
            at = @At("TAIL"),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    public void inject_init(CallbackInfo callbackInfo) {
        this.textInputUtil = new TextInputUtil(
                () -> this.field_238846_r_[this.editLine],
                (string) -> {
                    this.field_238846_r_[this.editLine] = string;
                    this.tileSign.setText(this.editLine, new StringTextComponent(string));
                },
                TextInputUtil.getClipboardTextSupplier(this.minecraft),
                TextInputUtil.getClipboardTextSetter(this.minecraft),
                (p_238848_1_) -> true);
    }

}
