package top.nowandfuture.mod.imagesign.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.SignTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.extensions.IForgeTileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import top.nowandfuture.mod.imagesign.loader.ImageFetcher;
import top.nowandfuture.mod.imagesign.loader.SignImageLoadManager;

@Mixin(SignTileEntity.class)
public abstract class MixinSignTileEntity extends TileEntity implements IForgeTileEntity {

    @Shadow public abstract ITextComponent getText(int line);

    public MixinSignTileEntity(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }

    @Override
    public void remove() {
        super.remove();
        if(world != null && world.isRemote()){
            SignImageLoadManager.INSTANCE.tryRemoveFromLoadingList((SignTileEntity) ((Object)this));
            ImageFetcher.INSTANCE.getCache().removeByBos(pos);
            String url = getText(1).getString();
            if(ImageFetcher.INSTANCE.isInBlackList(url)){
                ImageFetcher.INSTANCE.removeFromBlackList(url);
            }
            ResourceLocation location = new ResourceLocation(
                    String.valueOf(pos.toLong())
            );
            Minecraft.getInstance().getTextureManager().deleteTexture(location);
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

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return IForgeTileEntity.super.getRenderBoundingBox();
    }
}
