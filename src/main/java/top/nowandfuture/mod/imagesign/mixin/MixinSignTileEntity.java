package top.nowandfuture.mod.imagesign.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.SignTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.extensions.IForgeTileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import top.nowandfuture.mod.imagesign.loader.ISignBlockEntityAccessor;
import top.nowandfuture.mod.imagesign.loader.ImageFetcher;
import top.nowandfuture.mod.imagesign.loader.ImageLoadManager;
import top.nowandfuture.mod.imagesign.loader.Stage;

@Mixin(SignTileEntity.class)
public abstract class MixinSignTileEntity extends TileEntity implements IForgeTileEntity, ISignBlockEntityAccessor {

    @Shadow public abstract ITextComponent getText(int line);
    @Unique private Stage stage = Stage.IDLE;

    public MixinSignTileEntity(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }

    @Override
    public void remove() {
        super.remove();
        if(world != null && world.isRemote()){
            ImageLoadManager.INSTANCE.tryRemoveFromLoadingList(pos.toLong());
            ImageFetcher.INSTANCE.removeByPos(pos.toLong());
        }
    }

    @Override
    public void onLoad() {
        if(world != null && world.isRemote()){

        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if(world != null && world.isRemote()){
            ImageLoadManager.INSTANCE.tryRemoveFromLoadingList(pos.toLong());
            ImageFetcher.INSTANCE.removeByPos(pos.toLong());
        }
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return IForgeTileEntity.super.getRenderBoundingBox();
    }

    @Override
    public Stage getStage() {
        return stage;
    }

    @Override
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @Override
    public double getMaxRenderDistanceSquared() {
        if(stage == Stage.SUCCESS) {
            double distance = Minecraft.getInstance().gameSettings.renderDistanceChunks * 16;
            return distance * distance;
        }else {
            return super.getMaxRenderDistanceSquared();
        }
    }
}
