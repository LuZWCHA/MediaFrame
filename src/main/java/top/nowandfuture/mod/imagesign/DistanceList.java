package top.nowandfuture.mod.imagesign;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

public class DistanceList {
    private Vector3d pos;
    private final Lock lock = new ReentrantLock();

    private long memoryLimit;
    private int size;
    private long curMemory;
    private LRUCache<String, ImageEntity> cacheMap;
    private PriorityQueue<SortedImage> disQueue;

    public DistanceList(int maxSize, long memoryLimit){
        this.size = maxSize;
        this.memoryLimit = memoryLimit;
        this.cacheMap = new LRUCache<>(maxSize);
        this.disQueue = new PriorityQueue<>();
        this.pos = new Vector3d(0, 0, 0);
    }

    public ImageEntity get(String url){
        return cacheMap.get(url);
    }

    private boolean posDirty = false;
    public void updateViewerPos(@NotNull Vector3d pos){
        if(lock.tryLock()) {
            if (!pos.equals(this.pos)) {
                this.pos = pos;
                posDirty = true;
            }
            lock.unlock();
        }
    }

    public double removeFarthestImages(long memorySize, double addedDistance, int a){
        List<SortedImage> sortList = new ArrayList<>();

        cacheMap.forEach(new BiConsumer<String, ImageEntity>() {
            @Override
            public void accept(String s, ImageEntity imageEntity) {
                for (BlockPos po : imageEntity.posList) {
                    SortedImage sortedImage = SortedImage.create(imageEntity, po.distanceSq(new Vector3i(pos.x, pos.y, pos.z)));
                    sortList.add(sortedImage);
                }
            }
        });
        double distance = -1;

        if(lock.tryLock()){
            Collections.sort(sortList);

            int size = 0;
            int rmCount = 0;
            boolean clearFlag = true;

            Map<SortedImage, Integer> removeImages = new HashMap<>();

            for (int i = sortList.size() - 1; i > 0 ; i--) {
                SortedImage sortedImage = sortList.get(i);

                final int count = removeImages.getOrDefault(sortedImage, 0);
                //it is the last image which is nearest to the viewer
                //the image has no-zero memory
                if(sortedImage.imageEntity.posList.size() - count == 1){
                    size += sortedImage.imageEntity.imageInfo.getSize();
                    if(clearFlag && size >= (a + 1) * memorySize){
                        distance = sortedImage.distance;
                        clearFlag = false;
                    }
                }
                removeImages.put(sortedImage, count + 1);

                if(clearFlag)
                    rmCount ++;
                SortedImage.recycle(sortedImage);
            }

            removeImages.forEach(new BiConsumer<SortedImage, Integer>() {
                @Override
                public void accept(SortedImage image, Integer integer) {
                    remove(image.imageEntity.url);
                }
            });

            posDirty = false;
            lock.unlock();
        }

        sortList.clear();
        return distance;
    }

    public synchronized ImageEntity remove(BlockPos pos){
        ImageEntity res = ImageEntity.EMPTY;
        for(Map.Entry<String, ImageEntity> entry: cacheMap.entrySet()){
            if(entry.getValue().posList.contains(pos)){
                res = entry.getValue();
                break;
            }
        }

        if(res != ImageEntity.EMPTY){
            return remove(res.url, pos);
        }

        return null;
    }

    public synchronized ImageEntity remove(String url, BlockPos pos){
        ImageEntity imageEntity = cacheMap.get(url);
        if(imageEntity != null){
            imageEntity.posList.remove(pos);
            if(imageEntity.posList.isEmpty()){
                remove(url);
            }
        }
        return imageEntity;
    }

    public synchronized ImageEntity remove(String url){
        ImageEntity imageEntity = cacheMap.remove(url);
        if(imageEntity != null){
            imageEntity.dispose();
        }
        return imageEntity;
    }

    public boolean contain(String url){
        return cacheMap.containsKey(url);
    }

    private int reAddCount = 0;
    public synchronized ImageEntity add(@NotNull ImageEntity entity){
        long imageSize = entity.imageInfo.getSize();
        //merge the position and use the new image data
        if(cacheMap.containsKey(entity.url)) {
            ImageEntity old = cacheMap.remove(entity.url);
            old.dispose();
            //update positions
            if(!cacheMap.get(entity.url).posList.contains(entity.getFirstPos())) {
                entity.posList.addAll(old.posList);
            }
            curMemory -= imageSize;
        }

        if(curMemory + imageSize <= memoryLimit) {
            curMemory += imageSize;
            boolean removeEldest = cacheMap.size() == size;
            cacheMap.put(entity.url, entity);

            //remove eldest
            if(removeEldest){
                Map.Entry<String, ImageEntity> entry = cacheMap.getEldest();
                if(entry != null && !cacheMap.containsKey(entry.getKey())){
                    //remove the eldest image if out of the capacity
                    entry.getValue().dispose();
                }
            }

            posDirty = true;
            reAddCount = 0;
            return entity;
        }else{
            //to get a memory size of imageSize
            double distance = removeFarthestImages(imageSize,
                    entity.getFirstPos().distanceSq(pos.x, pos.y, pos.z, true), reAddCount);

            if(distance > 0 && reAddCount < 2) {
                reAddCount++;
                return add(entity);
            }
        }

        reAddCount = 0;
        return ImageEntity.EMPTY;
    }

    public synchronized void dispose(){
        cacheMap.forEach(new BiConsumer<String, ImageEntity>() {
            @Override
            public void accept(String s, ImageEntity imageEntity) {
                imageEntity.dispose();
            }
        });
        cacheMap.clear();
    }

    private static abstract class ObjectPool<T extends ObjectPool.IDispose>{
        private final Queue<T> objects;
        public ObjectPool(){
            objects = new LinkedList<>();
        }

        public T get(){
            if(objects.isEmpty()){
                objects.add(createNew());
            }

            return objects.poll();
        }

        public void recycle(T t){
            t.dispose();
            if(!objects.contains(t)) {
                objects.add(t);
            }
        }

        public void close(){
            for (T object : objects) {
                object.dispose();
            }

            objects.clear();
        }

        public abstract T createNew();

        interface IDispose{
            void dispose();
        }
    }

    public static class SortedImage implements Comparable<SortedImage>, ObjectPool.IDispose {
        public static final SortedImagePool POOL = new SortedImagePool();

        ImageEntity imageEntity;
        double distance;

        public SortedImage(){

        }

        private SortedImage(ImageEntity imageEntity, double distance) {
            this.imageEntity = imageEntity;
            this.distance = distance;
        }

        public void set(ImageEntity imageEntity, double distance){
            this.imageEntity = imageEntity;
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
            return imageEntity.hashCode();
        }

        @Override
        public int compareTo(@NotNull SortedImage o) {
            double res = this.distance - o.distance;
            if(res > 0) return 1;
            else if(res < 0) return -1;
            return 0;
        }

        @Override
        public void dispose() {
            imageEntity = null;
            distance = -1;
        }

        public static SortedImage create(){
            return POOL.get();
        }

        public static SortedImage create(ImageEntity imageEntity, double distance){
            SortedImage sortedImage = POOL.get();
            sortedImage.set(imageEntity, distance);
            return sortedImage;
        }

        public static void recycle(SortedImage image){
            POOL.recycle(image);
        }

        public static class SortedImagePool extends ObjectPool<SortedImage>{

            @Override
            public SortedImage createNew() {
                return new SortedImage();
            }
        }
    }

    public long getCurMemory(){
        return curMemory;
    }

    public long getLeftMemory(){
        return memoryLimit - curMemory;
    }

    private double getMemoryPercent(){
        return getLeftMemory() / (double)memoryLimit;
    }
}
