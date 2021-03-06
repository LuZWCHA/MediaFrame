package top.nowandfuture.mod.imagesign.setup;

import com.ibm.icu.impl.Pair;
import com.mojang.blaze3d.systems.IRenderCall;
import com.mojang.blaze3d.systems.RenderSystem;
import io.reactivex.rxjava3.disposables.Disposable;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.client.ClipboardHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.SignTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import top.nowandfuture.mod.imagesign.ImageSign;
import top.nowandfuture.mod.imagesign.RenderQueue;
import top.nowandfuture.mod.imagesign.caches.GIFImagePlayManager;
import top.nowandfuture.mod.imagesign.loader.*;
import top.nowandfuture.mod.imagesign.caches.Vector3d;
import top.nowandfuture.mod.imagesign.utils.OptiFineHelper;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class ClientProxy extends CommonProxy {

    public ClientProxy() {
    }

    @Override
    public void setup(FMLCommonSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(this);

    }

    @SubscribeEvent
    public void onWorldLastRender(RenderWorldLastEvent renderWorldLastEvent) {
        RenderQueue.doTasks();
        RenderQueue.updateQuerySet();
        RenderQueue.FRAME_COUNT++;

        ImageLoadManager.INSTANCE.runLoadTasks();
        ImageFetcher.INSTANCE.getCache().tryProcessWaitQueue();
    }

    private static boolean lastShader;
    private static int lastCountReset = -1;

    public static boolean isShaderLoaded() {
        return lastShader;
    }

    @SubscribeEvent
    public void oClientTick(TickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            Entity entity = Minecraft.getInstance().getRenderViewEntity();
            if (entity != null) {
                ImageFetcher.INSTANCE.onTick(new Vector3d(entity.getPosX(), entity.getPosY(), entity.getPosZ()));
                GIFImagePlayManager.INSTANCE.tick();
                if (OptiFineHelper.isLoaded()) {
                    boolean cur = OptiFineHelper.isShaderLoaded();
                    if (cur != lastShader) {
                        lastShader = cur;
                    }

                    int count = OptiFineHelper.getResetDisplayListsCount();

                    if (count >= 0 && lastCountReset != count) {
                        lastCountReset = count;

                        // Update the image when shader first loaded or reloaded, but it has no effect by testing now.
                        //I don't sure that if the shader will break the image render in the future, so the codes will be
                        // always here.
                        RenderSystem.recordRenderCall(() -> {
//                                ImageLoadManager.INSTANCE.clear(entity.world);
//                                ImageFetcher.INSTANCE.reload();
                        });

                    }
                }
            }
        }
    }

    private static final ClipboardHelper CLIPBOARD_HELPER
            = new ClipboardHelper();

    @SubscribeEvent
    public void onClickOnBlock(PlayerInteractEvent.RightClickBlock rightClickBlock) {
        World world = rightClickBlock.getWorld();
        BlockPos blockPos = rightClickBlock.getPos();
        boolean refresh = rightClickBlock.getItemStack().isEmpty();
        boolean shiftDown = Screen.hasShiftDown();
        ImageFetcher fetcher = ImageFetcher.INSTANCE;
        if (world.isRemote() && world.getBlockState(blockPos).getBlock() instanceof AbstractSignBlock) {
            boolean hasTe = world.getBlockState(blockPos).hasTileEntity();
            if (hasTe) {
                if (shiftDown) {
                    TileEntity entity = world.getTileEntity(blockPos);
                    if (entity instanceof SignTileEntity) {
                        String header = ((SignTileEntity) entity).getText(0).getUnformattedComponentText();
                        String url = ((SignTileEntity) entity).getText(1).getUnformattedComponentText();
                        boolean normalImage = "[Image]".equals(header);
                        boolean thuImage = "[ImageT]".equals(header);
                        if (normalImage || thuImage && !url.isEmpty()) {
                            CLIPBOARD_HELPER.setClipboardString(Minecraft.getInstance().getMainWindow().getHandle(), url);
                            PlayerEntity player = Minecraft.getInstance().player;
                            if (player != null) {
                                player.sendMessage(ITextComponent.getTextComponentOrEmpty(
                                        String.format("Copied url: %s", url)),
                                        player.getUniqueID()
                                );
                            }
                            ImageSign.LOGGER.info("Copied url: {} into the clipboard.", url);
                        }
                    }
                } else {
                    if (refresh) {
                        fetcher.refreshSmooth(blockPos.toLong());
                    } else {
                        ImageLoadManager.INSTANCE.clear(
                                (entityPos, disposableStringPair) -> {
                                    if (world.getTileEntity(BlockPos.fromLong(entityPos)) != null) {
                                        disposableStringPair.first.dispose();
                                    }
                                }
                        );
                        fetcher.refresh(blockPos.toLong());
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote()) {
            ImageLoadManager.INSTANCE.clear(
                    (entityPos, disposableStringPair) -> {
                        if (event.getWorld().getTileEntity(BlockPos.fromLong(entityPos)) != null) {
                            disposableStringPair.first.dispose();
                        }
                    });
            ImageFetcher.INSTANCE.dispose();
            RenderQueue.clearQueue();
            GIFImagePlayManager.INSTANCE.clear();
        }
    }

    @Override
    public void doClientStuff(FMLClientSetupEvent event) {
        ImageFetcher.INSTANCE.addListener(iEvent -> {
            if (iEvent instanceof FetchInfo) {
                FetchInfo info = (FetchInfo) iEvent;
                if (info.object instanceof Long) {
                    BlockPos pos = BlockPos.fromLong((Long) info.object);
                    TileEntity tileEntity = event.getMinecraftSupplier().get().world.getTileEntity(pos);

                    if (tileEntity instanceof ISignBlockEntityAccessor) {
                        ISignBlockEntityAccessor entityAccessor = (ISignBlockEntityAccessor) (tileEntity);
                        entityAccessor.setStage(info.stage);
                    }
                }
            }
        });
    }
}
