package top.nowandfuture.mod.imagesign.mixin;

import net.minecraft.tileentity.SignTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.common.extensions.IForgeTileEntity;
import org.spongepowered.asm.mixin.Mixin;
import top.nowandfuture.mod.imagesign.ImageFetcher;
import top.nowandfuture.mod.imagesign.SignImageLoadManager;

@Mixin(SignTileEntity.class)
public abstract class MixinSignTileEntity extends TileEntity implements IForgeTileEntity {

    public MixinSignTileEntity(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }

    @Override
    public void remove() {
        super.remove();
        if(world != null && world.isRemote()){
            SignImageLoadManager.INSTANCE.tryRemoveFromLoadingList((SignTileEntity) ((Object)this));
            ImageFetcher.INSTANCE.getCache().remove(pos);
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

        }
    }
}
