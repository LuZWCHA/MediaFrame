package top.nowandfuture.mod.imagesign.caches;

import io.netty.util.collection.LongObjectHashMap;
import io.netty.util.collection.LongObjectMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.SoftReference;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public class ImageEntityCache {
    private Vector3d viewerPos;

    private long memoryLimit;
    private int size;
    private long singleImageMaxSize;
    private long curMemory;
    private LRUCache<String, ImageEntity> cacheMap;
    private Map<Long, ImageEntity> posQuickMap;

    private CacheChangeListener cacheChangeListener;

    public interface CacheChangeListener {
        void remove(ImageEntity imageEntity, BlockPos... positions);

        void add(ImageEntity imageEntity, BlockPos... positions);
    }

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

    public synchronized void removeFromQuickQueryMap(BlockPos pos) {
        posQuickMap.remove(pos.toLong());
    }

    public synchronized void reset(int maxSize, long singleImageMaxSize, long memoryLimit) {
        if (maxSize == this.size && singleImageMaxSize == this.singleImageMaxSize && memoryLimit == this.memoryLimit) {
            return;
        }

        dispose();

        this.size = maxSize;
        this.memoryLimit = memoryLimit;
        this.cacheMap = new LRUCache<>(maxSize);
        //the quick map may bigger than cache map because one image may has more the one sign tile entities
        this.posQuickMap = new LRUCache<>(maxSize);
        this.singleImageMaxSize = singleImageMaxSize;
    }

    public synchronized ImageEntity get(String url) {
        return cacheMap.get(url);
    }

    private AtomicBoolean posDirty = new AtomicBoolean(true);

    public void updateViewerPos(@NotNull Vector3d pos) {
        if (!pos.equals(this.viewerPos)) {
            this.viewerPos = pos;
            this.posDirty.set(true);
        }
    }

    //This method is slow! to remove the farthest points is linear time complexity ( O(n) )
    //If every query to do so, for a N near to the map size n, at one frame the total query time is nearly O(n^2)
    @Deprecated
    public double removeFarthestEntities(long memorySize, double addedDistance, int a) {
        LinkedList<ImageWithDistance> sortList = new LinkedList<>();
        BlockPos bb = new BlockPos(viewerPos);

        synchronized (this) {
            cacheMap.forEach((s, imageEntity) -> {
                for (BlockPos po : imageEntity.posList) {
                    ImageWithDistance imageWithDistance = ImageWithDistance.create(imageEntity, po, po.distanceSq(bb));
                    sortList.add(imageWithDistance);
                }
            });
        }
        double distance = -1;

        if (!sortList.isEmpty()) {
            Collections.sort(sortList);

            long size = getLeftMemory();
            boolean clearFlag = true;

            final Map<ImageEntity, Integer> imageCounter = new HashMap<>();

            for (int i = sortList.size() - 1; i >= 0; i--) {
                ImageWithDistance imageWithDistance = sortList.get(i);

                final int count = imageCounter.getOrDefault(imageWithDistance.imageEntity, 0);
                //It is the last image which is nearest to the viewer(one image may link to many positions)
                //the image has no-zero size
                if (imageWithDistance.imageEntity.posList.size() - count == 1) {
                    size += imageWithDistance.imageEntity.imageInfo.getSize();
                    if (addedDistance + .01 < imageWithDistance.distance && size > (a + 1) * memorySize * 1.1) {
                        distance = addedDistance;
                        clearFlag = false;
                        imageCounter.put(imageWithDistance.imageEntity, count + 1);
                        break;
                    }
                }

                imageCounter.put(imageWithDistance.imageEntity, count + 1);

                ImageWithDistance.recycle(imageWithDistance);
            }

            if (clearFlag && !sortList.isEmpty()) {
                distance = sortList.get(0).distance;
            } else if (!clearFlag) {
                imageCounter.forEach((ImageEntity imageEntity, Integer integer) -> {
                    //Try to remove the url but may failed because this method not thread-safe after the map sort operation
                    removeImage(imageEntity.url);
                });
                distance = addedDistance;
            }

            posDirty.set(false);
        }

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

    public synchronized ImageEntity removeEntityByPos(BlockPos pos) {
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
        final ImageEntity imageEntity = cacheMap.get(url);
        if (imageEntity != null) {
            posQuickMap.remove(pos.toLong());
            imageEntity.posList.remove(pos);
            removeEvent(imageEntity, pos);
            if (imageEntity.posList.isEmpty()) {
                removeImage(url);
            }

        }
        return imageEntity;
    }

    public synchronized ImageEntity removeImage(String url) {
        final ImageEntity imageEntity = cacheMap.remove(url);
        if (imageEntity != null) {
            removeAllPosFromQuickQueryMap(imageEntity.posList);
            curMemory -= imageEntity.imageInfo.getSize();
            //The posList may be empty
            if (!imageEntity.posList.isEmpty()) {
                removeEvent(imageEntity, imageEntity.posList.toArray(new BlockPos[0]));
            }
            imageEntity.dispose();
        }
        return imageEntity;
    }

    private void removeEvent(ImageEntity imageEntity, BlockPos... positions) {
        if (cacheChangeListener != null) {
            cacheChangeListener.remove(imageEntity, positions);
        }
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

    private void removeAllPosFromQuickQueryMap(List<BlockPos> posList) {
        for (BlockPos blockPos : posList) {
            posQuickMap.remove(blockPos.toLong());
        }
    }

    public long getSingleImageMaxSize() {
        return singleImageMaxSize;
    }

    public synchronized ImageEntity add(@NotNull ImageEntity entity) {
        long imageSize = entity.imageInfo.getSize();

        //Merge the position:
        if (cacheMap.containsKey(entity.url)) {
            final ImageEntity imageEntity = cacheMap.get(entity.url);
            boolean hasAdded = imageEntity.posList.contains(entity.getFirstPos());
            if (!hasAdded) {
                imageEntity.posList.add(entity.getFirstPos());
                posQuickMap.put(entity.getFirstPos().toLong(), imageEntity);
            }

            return imageEntity;
        }

        if (curMemory + imageSize <= memoryLimit) {
            curMemory += imageSize;
            final boolean removeEldest = cacheMap.size() == size;
            cacheMap.put(entity.url, entity);
            posQuickMap.put(entity.getFirstPos().toLong(), entity);

            addEvent(entity, entity.posList.toArray(new BlockPos[0]));

            //Dispose the eldest that be removed by map itself.
            if (removeEldest) {
                Map.Entry<String, ImageEntity> entry = cacheMap.getEldest();
                if (entry != null && !cacheMap.containsKey(entry.getKey())) {
                    ImageEntity imageEntity = entry.getValue();
                    //remove the eldest image if out of the capacity
                    removeAllPosFromQuickQueryMap(entity.posList);

                    removeEvent(imageEntity, imageEntity.posList.toArray(new BlockPos[0]));
                    entry.getValue().dispose();
                }
            }

            posDirty.set(false);
            return entity;
        } else {
//            Reach the limit.
//            Remove the farthest.
            delayAdd(entity);
        }
        return ImageEntity.EMPTY;
    }

    //If the cache is full, we will add the image to the cache by the distance to viewer.
    //The images may be clean up by GC, so the sort result will not be so stable.
    //The images may load and unload alternatively.
    private final List<SoftReference<ImageWithDistance>> waitAddList = new LinkedList<>();

    private void delayAdd(ImageEntity imageEntity) {
        synchronized (waitAddList) {
            BlockPos blockPos = imageEntity.getFirstPos();
            double distance = blockPos.distanceSq(new BlockPos(viewerPos));
            waitAddList.add(new SoftReference<>(ImageWithDistance.create(imageEntity, blockPos, distance)));
        }
    }

    public void tryProcessWaitQueue() {
        if (!waitAddList.isEmpty()) {
            List<ImageWithDistance> imageWithDistanceList = new LinkedList<>();
            BlockPos viewer = new BlockPos(viewerPos);
            //Try to sort the image entity by distance.
            synchronized (waitAddList) {
                for (SoftReference<ImageWithDistance> softReference : waitAddList) {
                    ImageWithDistance image = softReference.get();
                    if (image != null) {
                        imageWithDistanceList.add(image);
                    }
                }

                waitAddList.clear();
            }

            for (ImageEntity entity : cacheMap.values()) {
                if (entity != null) {
                    for (BlockPos blockPos : entity.posList) {
                        imageWithDistanceList.add(ImageWithDistance.create(entity, blockPos, blockPos.distanceSq(viewer)));
                    }
                }
            }

            Collections.sort(imageWithDistanceList);

            long cm = 0;
            LongObjectMap<ImageEntity> add = new LongObjectHashMap<>();

            Set<String> recordUrls = new HashSet<>();
            for (ImageWithDistance entity : imageWithDistanceList) {
                ImageEntity imageEntity = entity.imageEntity;
                long size = imageEntity.imageInfo.getSize();

                if (recordUrls.contains(imageEntity.url)) {
                    size = 0;
                }

                if (cm <= memoryLimit - size) {
                    cm += size;
                    add.put(entity.pos, imageEntity);
                    recordUrls.add(imageEntity.url);
                }

                ImageWithDistance.recycle(entity);
            }

            List<String> remove = new LinkedList<>();

            for (Map.Entry<String, ImageEntity> stringImageEntityEntry : cacheMap.entrySet()) {
                String url = stringImageEntityEntry.getKey();
                if(!recordUrls.contains(url)) {
                    remove.add(url);
                }
            }

            for (String s : remove) {
                removeImage(s);
            }

            for (Map.Entry<Long, ImageEntity> longImageEntityEntry : add.entrySet()) {
                ImageEntity imageEntity = longImageEntityEntry.getValue();
                if (!cacheMap.containsKey(imageEntity.url)) {
                    add(imageEntity);
                }
            }
        }

    }

    private void addEvent(ImageEntity entity, BlockPos... positions) {
        if (cacheChangeListener != null) {
            cacheChangeListener.add(entity, positions);
        }
    }

    public synchronized void dispose() {
        cacheMap.forEach((s, imageEntity) -> {
            imageEntity.dispose();
            removeEvent(imageEntity, imageEntity.posList.toArray(new BlockPos[0]));
        });
        posQuickMap.clear();
        cacheMap.clear();
        curMemory = 0;
        ImageWithDistance.close();
        posDirty.set(true);
    }

    public void setCacheChangeListener(CacheChangeListener cacheChangeListener) {
        this.cacheChangeListener = cacheChangeListener;
    }

    private static abstract class ObjectPool<T extends ObjectPool.IDispose> {
        private final Queue<T> objects;

        public ObjectPool() {
            objects = new LinkedBlockingQueue<>();
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

    public static class ImageWithDistance implements Comparable<ImageWithDistance>, ObjectPool.IDispose {
        public static final ThreadLocal<SortedImagePool> POOL = ThreadLocal.withInitial(SortedImagePool::new);

        public ImageEntity imageEntity;
        public long pos;
        public double distance;

        public ImageWithDistance() {

        }

        private ImageWithDistance(ImageEntity imageEntity, double distance, BlockPos pos) {
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

            ImageWithDistance that = (ImageWithDistance) o;

            return pos == that.pos;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(pos);
        }

        @Override
        public int compareTo(@NotNull ImageWithDistance o) {
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

        public static ImageWithDistance create() {
            return POOL.get().get();
        }

        public static ImageWithDistance create(ImageEntity imageEntity, BlockPos pos, double distance) {
            ImageWithDistance imageWithDistance = POOL.get().get();
            imageWithDistance.set(imageEntity, pos, distance);
            return imageWithDistance;
        }

        public static void recycle(ImageWithDistance image) {
            POOL.get().recycle(image);
        }

        public static void close() {
            POOL.get().close();
        }

        public static class SortedImagePool extends ObjectPool<ImageWithDistance> {

            @Override
            public ImageWithDistance createNew() {
                return new ImageWithDistance();
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
