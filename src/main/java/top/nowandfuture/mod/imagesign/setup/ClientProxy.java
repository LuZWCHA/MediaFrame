package top.nowandfuture.mod.imagesign.setup;

import com.mojang.blaze3d.platform.ClipboardManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelLastEvent;
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
import top.nowandfuture.mod.imagesign.caches.Vector3d;
import top.nowandfuture.mod.imagesign.loader.FetchInfo;
import top.nowandfuture.mod.imagesign.loader.ISignBlockEntityAccessor;
import top.nowandfuture.mod.imagesign.loader.ImageFetcher;
import top.nowandfuture.mod.imagesign.loader.ImageLoadManager;
import top.nowandfuture.mod.imagesign.utils.OptiFineHelper;


@OnlyIn(Dist.CLIENT)
public class ClientProxy extends CommonProxy {

    public ClientProxy() {
    }

    @Override
    public void setup(FMLCommonSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(this);

    }

    @SubscribeEvent
    public void onWorldLastRender(RenderLevelLastEvent renderWorldLastEvent) {
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
            Entity entity = Minecraft.getInstance().getCameraEntity();
            if (entity != null) {
                ImageFetcher.INSTANCE.onTick(new Vector3d(entity.getX(), entity.getY(), entity.getZ()));
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

    private static final ClipboardManager CLIPBOARD_HELPER
            = new ClipboardManager();

    @SubscribeEvent
    public void onClickOnBlock(PlayerInteractEvent.RightClickBlock rightClickBlock) {
        Level world = rightClickBlock.getWorld();
        BlockPos blockPos = rightClickBlock.getPos();
        boolean refresh = rightClickBlock.getItemStack().isEmpty();
        boolean shiftDown = Screen.hasShiftDown();
        ImageFetcher fetcher = ImageFetcher.INSTANCE;
        if (world.isClientSide() && world.getBlockState(blockPos).getBlock() instanceof SignBlock) {
            boolean hasTe = world.getBlockState(blockPos).hasBlockEntity();
            if (hasTe) {
                if (shiftDown) {
                    BlockEntity entity = world.getBlockEntity(blockPos);
                    if (entity instanceof SignBlockEntity) {
                        String header = ((SignBlockEntity) entity).getMessage(0, false).getContents();
                        String url = ((SignBlockEntity) entity).getMessage(1,false).getContents();
                        boolean normalImage = "[Image]".equals(header);
                        boolean thuImage = "[ImageT]".equals(header);
                        if (normalImage || thuImage && !url.isEmpty()) {
                            CLIPBOARD_HELPER.setClipboard(Minecraft.getInstance().getWindow().getWindow(), url);
                            LocalPlayer player = Minecraft.getInstance().player;
                            if (player != null) {
                                player.sendMessage(Component.nullToEmpty(
                                        String.format("Copied url: %s", url)),
                                        player.getUUID()
                                );
                            }
                            ImageSign.LOGGER.info("Copied url: {} into the clipboard.", url);
                        }
                    }
                } else {
                    if (refresh) {
                        fetcher.refreshSmooth(blockPos.asLong());
                    } else {
                        ImageLoadManager.INSTANCE.clear(
                                (entityPos, disposableStringPair) -> {
                                    if (world.getBlockEntity(BlockPos.of(entityPos)) != null) {
                                        disposableStringPair.first.dispose();
                                    }
                                }
                        );
                        fetcher.refresh(blockPos.asLong());
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isClientSide()) {
            ImageLoadManager.INSTANCE.clear(
                    (entityPos, disposableStringPair) -> {
                        if (event.getWorld().getBlockEntity(BlockPos.of(entityPos)) != null) {
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
            if (iEvent instanceof FetchInfo info) {
                if (info.object instanceof Long && Minecraft.getInstance().level != null) {
                    BlockPos pos = BlockPos.of((Long) info.object);
                    BlockEntity tileEntity = Minecraft.getInstance().level.getBlockEntity(pos);

                    if (tileEntity instanceof ISignBlockEntityAccessor) {
                        ISignBlockEntityAccessor entityAccessor = (ISignBlockEntityAccessor) (tileEntity);
                        entityAccessor.setStage(info.stage);
                    }
                }
            }
        });
    }
}
