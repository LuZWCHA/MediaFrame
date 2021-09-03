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
        long posLong = getPos();
        String url = getUrl();
        if (!loadManager.isLoading(url)) {
            //noinspection ResultOfMethodCallIgnored
            fetcher.get(url, getPos(), OpenGLScheduler.renderThread())
                    .doOnSubscribe(disposable1 -> {
                        loadManager.addToLoadingList(posLong, url, disposable1);
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
    public int hashCode() {
        return getUrl().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return getUrl().equals(obj);
    }

    public abstract long getPos();

    public abstract String getUrl();

    public abstract Supplier<BlockPos> getViewerPos();

    public static class SignImageLoadTask extends ImageLoadTask{
        private final long pos;
        private final String url;

        public SignImageLoadTask(long pos, String url) {
            this.pos = pos;
            this.url = url;
        }

        @Override
        public int compareTo(@NotNull ImageLoadTask o) {
            double otherDis = BlockPos.fromLong(o.getPos()).distanceSq(getViewerPos().get());
            double distanceSq = BlockPos.fromLong(getPos()).distanceSq(getViewerPos().get());
            double res = distanceSq - otherDis;
            if (res > 0) return 1;
            else if (res < 0) return -1;
            return 0;
        }

        @Override
        public long getPos() {
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
