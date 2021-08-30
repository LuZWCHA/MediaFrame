package top.nowandfuture.mod.imagesign;


import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.util.math.BlockPos;
import top.nowandfuture.mod.imagesign.caches.ImageEntity;
import top.nowandfuture.mod.imagesign.caches.ImageEntityCache;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

public class RenderQueue {

    private static final Queue<Runnable> queue = new LinkedBlockingDeque<>();
    private static int limit = 10;
    private static long exceptWaitTime = 2;
    private static int maxLimit = 20 , minLimit = 1;

    private static final PriorityQueue<ImageEntityCache.ImageWithDistance> distanceQueue = new PriorityQueue<>();
    private static final LongSet posSet = new LongOpenHashSet();
    private static long farthestPos = -1;

    public static long FRAME_COUNT = 0;

    private RenderQueue(){

    }

    public static void init(int maxLimit, long exceptWaitTime){
        init(Math.max(1, maxLimit >> 1), maxLimit, 1, exceptWaitTime);
    }

    public static void init(int limit, int maxLimit, int minLimit, long exceptWaitTime){
        RenderQueue.limit = limit;
        RenderQueue.exceptWaitTime = exceptWaitTime;
        RenderQueue.maxLimit = maxLimit;
        RenderQueue.minLimit = minLimit;
    }

    public static void doTasks(){
        long start = System.currentTimeMillis();
        while (!queue.isEmpty()){
            try {
                queue.poll().run();
            }catch (Exception ignored){

            }
        }

        long t = System.currentTimeMillis() - start;
        if(t > exceptWaitTime && limit > minLimit){
            if(t / exceptWaitTime > 2){
                if(limit > 3) {
                    limit >>>= 1;
                }else{
                    limit -= 2;
                }
            }else{
                limit--;
            }

        }else if(t < exceptWaitTime && limit < maxLimit){
            limit ++;
        }
    }

    public static void runTask(Runnable runnable){
        queue.add(runnable);
    }

    public static void clearQueue(){
        queue.clear();
        distanceQueue.clear();
        posSet.clear();
    }

    public static Runnable poll(){
        return queue.poll();
    }

    public static boolean isQueueEmpty(){
        return queue.isEmpty();
    }

    public static boolean tryAddTask(Runnable runnable){
        if(queue.size() < limit){
            runTask(runnable);
            return true;
        }
        return false;
    }

    public static void addNextFrameRenderObj(ImageEntity entity, BlockPos pos, double distance){
        distanceQueue.add(ImageEntityCache.ImageWithDistance.create(entity, pos, distance));
    }

    private static int maxRenderObjCount = 30;
    public static void updateQuerySet(){
        posSet.clear();
        int i = 0;
        while (!distanceQueue.isEmpty()){
            ImageEntityCache.ImageWithDistance imageWithDistance = distanceQueue.poll();
            if(i ++ < maxRenderObjCount) {
                posSet.add(imageWithDistance.pos);
            }
            if(i == maxRenderObjCount){
                farthestPos = imageWithDistance.pos;
            }
            ImageEntityCache.ImageWithDistance.recycle(imageWithDistance);
        }
    }

    public static boolean isInPosSet(BlockPos pos){
        return posSet.contains(pos.toLong());
    }

    public static boolean isNearer(BlockPos pos, BlockPos viewer){
        return pos.distanceSq(viewer) < viewer.distanceSq(BlockPos.fromLong(farthestPos));
    }

    public static void setMaxRenderObjCount(int maxRenderObjCount) {
        RenderQueue.maxRenderObjCount = maxRenderObjCount;
    }
}
