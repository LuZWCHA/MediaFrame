package top.nowandfuture.mod.imagesign.loader;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import top.nowandfuture.mod.imagesign.schedulers.OpenGLScheduler;

import java.util.function.Supplier;

public abstract class ImageLoadTask implements Runnable, Comparable<ImageLoadTask>{

    @Override
    public void run() {
        load();
    }

    protected void load(){
        ImageLoadManager loadManager = ImageLoadManager.INSTANCE;
        ImageFetcher fetcher = ImageFetcher.INSTANCE;
        long posLong = getPos().toLong();
        String url = getUrl();
        if (!loadManager.isLoading(posLong)) {
            //noinspection ResultOfMethodCallIgnored
            fetcher.get(url, getPos(), OpenGLScheduler.renderThread())
                    .doOnSubscribe(disposable1 -> {
                        loadManager.addToLoadingList(posLong, disposable1);
                    })
                    .subscribe(
                            imageEntity1 -> {

                            }, throwable -> {
                                loadManager.removeFromLoadingList(posLong);
                                //Thr url or the image is load successful because of the limit by the memory,
                                //We may cache them next time.
                                if(!(throwable instanceof OutOfMemoryError)) {
                                    fetcher.addToBlackList(url);
                                }else{
                                    // TODO: 2021/8/28 expand the cache or resize the image
                                }

                            }, () -> {
                                loadManager.removeFromLoadingList(posLong);
                            });
        }
    }

    @Override
    public int compareTo(@NotNull ImageLoadTask o) {
        double otherDis = o.getPos().distanceSq(getViewerPos().get());
        double distanceSq = getPos().distanceSq(getViewerPos().get());
        double res = distanceSq - otherDis;
        if (res > 0) return 1;
        else if (res < 0) return -1;
        return 0;
    }

    @Override
    public int hashCode() {
        return getPos().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return getPos().equals(obj);
    }

    public abstract BlockPos getPos();

    public abstract String getUrl();

    public abstract Supplier<BlockPos> getViewerPos();

    public static class SignImageLoadTask extends ImageLoadTask{
        private final BlockPos pos;
        private final String url;

        public SignImageLoadTask(BlockPos pos, String url) {
            this.pos = pos;
            this.url = url;
        }

        @Override
        public BlockPos getPos() {
            return pos;
        }

        @Override
        public String getUrl() {
            return url;
        }

        @Override
        public Supplier<BlockPos> getViewerPos() {
            return () -> {
                Entity entity = Minecraft.getInstance().getRenderViewEntity();
                if(entity != null) return entity.getPosition();
                return BlockPos.ZERO;
            };
        }
    }
}
