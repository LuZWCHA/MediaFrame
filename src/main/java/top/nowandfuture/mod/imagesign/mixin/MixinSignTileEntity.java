package top.nowandfuture.mod.imagesign.mixin;


import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import top.nowandfuture.mod.imagesign.loader.ISignBlockEntityAccessor;
import top.nowandfuture.mod.imagesign.loader.ImageFetcher;
import top.nowandfuture.mod.imagesign.loader.ImageLoadManager;
import top.nowandfuture.mod.imagesign.loader.Stage;

@Mixin(SignBlockEntity.class)
public abstract class MixinSignTileEntity extends BlockEntity implements ISignBlockEntityAccessor {

    public MixinSignTileEntity(BlockEntityType<?> pType, BlockPos pWorldPosition, BlockState pBlockState) {
        super(pType, pWorldPosition, pBlockState);
    }

    @Shadow public abstract Component getMessage(int p_155707_, boolean p_155708_);

    @Unique private Stage stage = Stage.IDLE;


    @Override
    public void setRemoved() {
        super.setRemoved();
        if(level != null && level.isClientSide()){
            ImageLoadManager.INSTANCE.tryRemoveFromLoadingList(worldPosition.asLong());
            ImageFetcher.INSTANCE.removeByPos(worldPosition.asLong());
        }
    }

    @Override
    public void onLoad() {
        if(level != null && level.isClientSide()){

        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if(level != null && level.isClientSide()){
            ImageLoadManager.INSTANCE.tryRemoveFromLoadingList(worldPosition.asLong());
            ImageFetcher.INSTANCE.removeByPos(worldPosition.asLong());
        }
    }

    @Override
    public Stage getStage() {
        return stage;
    }

    @Override
    public void setStage(Stage stage) {
        this.stage = stage;
    }
}
