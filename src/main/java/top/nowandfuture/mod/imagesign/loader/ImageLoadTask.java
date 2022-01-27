package top.nowandfuture.mod.imagesign.loader;

//import net.minecraft.client.Minecraft;
//import net.minecraft.entity.Entity;
//import net.minecraft.util.math.BlockPos;
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
        long posLong = getIdentifier();
        String url = getUrl();
        if (!loadManager.isLoading(url)) {
            //noinspection ResultOfMethodCallIgnored
            fetcher.get(url, getIdentifier(), OpenGLScheduler.renderThread())
                    .doOnSubscribe(disposable1 -> {
                        loadManager.addToLoadingList(posLong, url, disposable1);
                    })
                    .subscribe(
                            imageEntity1 -> {

                            }, throwable -> {
                                loadManager.removeFromLoadingList(posLong);
                                //The url or the image is loaded successful because of the limit by the memory,
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

    public abstract long getIdentifier();

    public abstract String getUrl();

    public abstract Supplier<Long> getViewerPos();
}
