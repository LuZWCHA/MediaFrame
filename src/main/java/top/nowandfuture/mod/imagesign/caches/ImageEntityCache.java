package top.nowandfuture.mod.imagesign.caches;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import org.jetbrains.annotations.NotNull;
import top.nowandfuture.mod.imagesign.RenderQueue;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

public class ImageEntityCache {
    private Vector3d viewerPos;

    private final long memoryLimit;
    private final int size;
    private final long singleImageMaxSize;
    private long curMemory;
    private final LRUCache<String, ImageEntity> cacheMap;
    private final Map<Long, ImageEntity> posQuickMap;


    //reserve 10 MB memory
//    private static byte[] memoryReserve = new byte[20 * 1024 * 1024];

    public ImageEntityCache(int maxSize, long singleImageMaxSize, long memoryLimit) {
        this.size = maxSize;
        this.memoryLimit = memoryLimit;
        this.cacheMap = new LRUCache<>(maxSize);
        this.viewerPos = new Vector3d(0, 0, 0);
        //the quick map may bigger than cache map because one image may has more the one sign tile entities
        this.posQuickMap = new LRUCache<>(maxSize);
        this.singleImageMaxSize = singleImageMaxSize;
    }



    public ImageEntity get(String url) {
        return cacheMap.get(url);
    }

    private AtomicBoolean posDirty = new AtomicBoolean(true);

    public void updateViewerPos(@NotNull Vector3d pos) {
        if (!pos.equals(this.viewerPos)) {
            this.viewerPos = pos;
            this.posDirty.set(true);
        }
    }


    public double removeFarthestEntities(long memorySize, double addedDistance, int a) {
        List<SortedImage> sortList = new ArrayList<>();
        BlockPos bb = new BlockPos(viewerPos);
        cacheMap.forEach(new BiConsumer<String, ImageEntity>() {
            @Override
            public void accept(String s, ImageEntity imageEntity) {
                for (BlockPos po : imageEntity.posList) {
                    SortedImage sortedImage = SortedImage.create(imageEntity, po, po.distanceSq(bb));
                    sortList.add(sortedImage);
                }
            }
        });
        double distance = -1;

        if (!sortList.isEmpty()) {
            Collections.sort(sortList);

            int size = 0;
            boolean clearFlag = true;

            Map<ImageEntity, Integer> imageCounter = new HashMap<>();

            for (int i = sortList.size() - 1; i >= 0; i--) {
                SortedImage sortedImage = sortList.get(i);

                final int count = imageCounter.getOrDefault(sortedImage.imageEntity, 0);
                //It is the last image which is nearest to the viewer(one image may link to many positions)
                //the image has no-zero size
                if (clearFlag && sortedImage.imageEntity.posList.size() - count == 1) {
                    size += sortedImage.imageEntity.imageInfo.getSize();
                    if (addedDistance < sortedImage.distance && size >= (a + 1) * memorySize) {
                        distance = addedDistance;
                        clearFlag = false;
                    }
                }

                if(clearFlag) {
                    imageCounter.put(sortedImage.imageEntity, count + 1);
                }
                SortedImage.recycle(sortedImage);
            }

            if (clearFlag && !sortList.isEmpty()) {
                distance = sortList.get(0).distance;
            }else if(!clearFlag) {
                imageCounter.forEach((ImageEntity imageEntity, Integer integer) -> {
                    removeImage(imageEntity.url);
                });
            }

            posDirty.set(false);
        }


        sortList.clear();
        return distance;
    }

    public synchronized ImageEntity findByPos(BlockPos pos) {
        final long posLong = pos.toLong();
        if (posQuickMap.containsKey(posLong)) {
            return posQuickMap.get(posLong);
        } else {
            ImageEntity res = ImageEntity.EMPTY;
            for (Map.Entry<String, ImageEntity> entry : cacheMap.entrySet()) {
                if (entry.getValue().posList.contains(pos)) {
                    res = entry.getValue();
                    break;
                }
            }
            if (res != ImageEntity.EMPTY) {
                posQuickMap.put(pos.toLong(), res);
            }
            return res;
        }
    }

    public synchronized ImageEntity removeByBos(BlockPos pos) {
        final long posLong = pos.toLong();
        if (posQuickMap.containsKey(posLong)) {
            ImageEntity entity = posQuickMap.get(posLong);
            removeEntity(entity.url, pos);
        } else {
            ImageEntity res = ImageEntity.EMPTY;
            for (Map.Entry<String, ImageEntity> entry : cacheMap.entrySet()) {
                if (entry.getValue().posList.contains(pos)) {
                    res = entry.getValue();
                    break;
                }
            }

            if (res != ImageEntity.EMPTY) {
                return removeEntity(res.url, pos);
            }
        }

        return null;
    }

    public synchronized ImageEntity removeEntity(String url, BlockPos pos) {
        ImageEntity imageEntity = cacheMap.get(url);
        if (imageEntity != null) {
            imageEntity.posList.remove(pos);
            if (imageEntity.posList.isEmpty()) {
                removeImage(url);
            }
            posQuickMap.remove(pos.toLong());

        }
        return imageEntity;
    }

    public synchronized ImageEntity removeImage(String url) {
        ImageEntity imageEntity = cacheMap.remove(url);
        if (imageEntity != null) {
            removeAllPos(imageEntity.posList);
            curMemory -= imageEntity.imageInfo.getSize();

            imageEntity.dispose();
        }
        return imageEntity;
    }

    public synchronized boolean contain(String url) {
        return cacheMap.containsKey(url);
    }

    public synchronized void clearGLSource() {
        for (Map.Entry<String, ImageEntity> stringImageEntityEntry : cacheMap.entrySet()) {
            stringImageEntityEntry.getValue().disposeGLSource();
        }
    }

    public synchronized void markUpdate() {
        for (Map.Entry<String, ImageEntity> stringImageEntityEntry : cacheMap.entrySet()) {
            stringImageEntityEntry.getValue().markUpdate();
        }
    }

    public int size() {
        return cacheMap.size();
    }

    private void removeAllPos(List<BlockPos> posList) {
        for (BlockPos blockPos : posList) {
            posQuickMap.remove(blockPos.toLong());
        }
    }

    private int reAddCount = 0;
    private long frameId = -1;
    private double lastDistance = -1;

    public synchronized ImageEntity add(@NotNull ImageEntity entity) {
        long imageSize = entity.imageInfo.getSize();
        if(imageSize > singleImageMaxSize){
            return ImageEntity.EMPTY;
        }

        //merge the position and use the new image data
        if (cacheMap.containsKey(entity.url)) {
            ImageEntity old = cacheMap.remove(entity.url);
            old.dispose();
            //update positions
            removeAllPos(old.posList);

            curMemory -= imageSize;
        }

        if (curMemory + imageSize <= memoryLimit) {
            curMemory += imageSize;
            boolean removeEldest = cacheMap.size() == size;
            cacheMap.put(entity.url, entity);
            posQuickMap.put(entity.getFirstPos().toLong(), entity);

            //remove eldest
            if (removeEldest) {
                Map.Entry<String, ImageEntity> entry = cacheMap.getEldest();
                if (entry != null && !cacheMap.containsKey(entry.getKey())) {
                    //remove the eldest image if out of the capacity
                    entry.getValue().dispose();
                    removeAllPos(entity.posList);
                }
            }

            posDirty.set(false);
            reAddCount = 0;
            return entity;
        } else {
            if (!RenderQueue.isNearer(entity.getFirstPos(), new BlockPos(viewerPos))) {
                //If it is not nearer than last frame's rendered images, throw out of memory error, don't add to the cache.
                return ImageEntity.EMPTY;
            }

            //To get a memory size of imageSize
            double ed = entity.getFirstPos().distanceSq(viewerPos.x, viewerPos.y, viewerPos.z, true);
            double distance = -1;
            if (frameId == RenderQueue.FRAME_COUNT - 1 && ed < lastDistance || lastDistance == -1) {
                distance = removeFarthestEntities(imageSize, ed, reAddCount);
            }

            if (distance > 0 && reAddCount < 2) {
                reAddCount++;
                lastDistance = distance;
                frameId = RenderQueue.FRAME_COUNT;
                return add(entity);
            }
        }

        reAddCount = 0;
        return ImageEntity.EMPTY;
    }

    public synchronized void dispose() {
        cacheMap.forEach(new BiConsumer<String, ImageEntity>() {
            @Override
            public void accept(String s, ImageEntity imageEntity) {
                imageEntity.dispose();
                for (BlockPos blockPos : imageEntity.posList) {
                    ResourceLocation location = new ResourceLocation(
                            String.valueOf(blockPos.toLong())
                    );
                    Minecraft.getInstance().getTextureManager().deleteTexture(location);
                }
            }
        });
        posQuickMap.clear();
        cacheMap.clear();
        curMemory = 0;
        posDirty.set(true);
    }

private static abstract class ObjectPool<T extends ObjectPool.IDispose> {
    private final Queue<T> objects;

    public ObjectPool() {
        objects = new LinkedList<>();
    }

    public T get() {
        if (objects.isEmpty()) {
            objects.add(createNew());
        }

        return objects.poll();
    }

    public void recycle(T t) {
        if (!objects.contains(t)) {
            t.dispose();
            objects.add(t);
        }
    }

    public void close() {
        for (T object : objects) {
            object.dispose();
        }

        objects.clear();
    }

    public abstract T createNew();

    interface IDispose {
        void dispose();
    }
}

public static class SortedImage implements Comparable<SortedImage>, ObjectPool.IDispose {
    public static final SortedImagePool POOL = new SortedImagePool();

    public ImageEntity imageEntity;
    public long pos;
    public double distance;

    public SortedImage() {

    }

    private SortedImage(ImageEntity imageEntity, double distance, BlockPos pos) {
        this.imageEntity = imageEntity;
        this.distance = distance;
        this.pos = pos.toLong();
    }

    public void set(ImageEntity imageEntity, BlockPos pos, double distance) {
        this.imageEntity = imageEntity;
        this.pos = pos.toLong();
        this.distance = distance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SortedImage that = (SortedImage) o;

        return imageEntity.equals(that.imageEntity);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public int compareTo(@NotNull SortedImage o) {
        double res = this.distance - o.distance;
        if (res > 0) return 1;
        else if (res < 0) return -1;
        return 0;
    }

    @Override
    public void dispose() {
        imageEntity = null;
        distance = -1;
    }

    public static SortedImage create() {
        return POOL.get();
    }

    public static SortedImage create(ImageEntity imageEntity, BlockPos pos, double distance) {
        SortedImage sortedImage = POOL.get();
        sortedImage.set(imageEntity, pos, distance);
        return sortedImage;
    }

    public static void recycle(SortedImage image) {
        POOL.recycle(image);
    }

    public static class SortedImagePool extends ObjectPool<SortedImage> {

        @Override
        public SortedImage createNew() {
            return new SortedImage();
        }
    }

}

    public long getCurMemory() {
        return curMemory;
    }

    public long getLeftMemory() {
        return memoryLimit - curMemory;
    }

    private double getMemoryPercent() {
        return getLeftMemory() / (double) memoryLimit;
    }
}
