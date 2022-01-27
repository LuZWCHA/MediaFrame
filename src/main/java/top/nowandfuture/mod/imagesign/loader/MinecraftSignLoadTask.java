package top.nowandfuture.mod.imagesign.loader;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.NotNull;
import top.nowandfuture.mod.imagesign.caches.Vector3i;

import java.util.function.Supplier;

public class MinecraftSignLoadTask extends ImageLoadTask {

    private final long id;
    private final String url;

    public MinecraftSignLoadTask(long id, String url) {
        this.id = id;
        this.url = url;
    }

    @Override
    public int compareTo(@NotNull ImageLoadTask o) {
        double otherDis = Vector3i.fromLong(o.getIdentifier()).distanceSq(Vector3i.fromLong(getViewerPos().get()));
        double distanceSq = Vector3i.fromLong(getIdentifier()).distanceSq(Vector3i.fromLong(getViewerPos().get()));
        double res = distanceSq - otherDis;
        if (res > 0) return 1;
        else if (res < 0) return -1;
        return 0;
    }

    @Override
    public long getIdentifier() {
        return id;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public Supplier<Long> getViewerPos() {
        return () -> {
            Entity entity = Minecraft.getInstance().getRenderViewEntity();
            if (entity != null) return entity.getPosition().toLong();
            return 0L;
        };
    }

}
