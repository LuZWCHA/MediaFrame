package top.nowandfuture.mod.imagesign;


import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.util.math.BlockPos;
import top.nowandfuture.mod.imagesign.caches.ImageEntity;
import top.nowandfuture.mod.imagesign.caches.ImageEntityCache;
import top.nowandfuture.mod.imagesign.caches.Vector3i;

import javax.annotation.Nullable;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * To do many tasks must run at render thread which will also cost much time on CPU.
 */
public class RenderQueue {

    private static final Queue<Runnable> queue = new LinkedBlockingDeque<>();
    private static final Queue<Runnable> waitQueue = new LinkedBlockingQueue<>();
    private static int limit = 5;
    private static long exceptWaitTime = 2;
    private static int maxLimit = 20, minLimit = 1;

    private static final PriorityQueue<ImageEntityCache.ImageWithDistance> distanceQueue = new PriorityQueue<>();
    private static final LongSet nearPosSet = new LongOpenHashSet();
    private static long farthestPos = -1;

    public static long FRAME_COUNT = 0;

    private RenderQueue() {

    }

    public static void init(int maxLimit, long exceptWaitTime) {
        init(Math.max(1, maxLimit >> 1), maxLimit, 1, exceptWaitTime);
    }

    public static void init(int limit, int maxLimit, int minLimit, long exceptWaitTime) {
        RenderQueue.limit = limit;
        RenderQueue.exceptWaitTime = exceptWaitTime;
        RenderQueue.maxLimit = maxLimit;
        RenderQueue.minLimit = minLimit;
    }

    public static void doTasks() {
        long start = System.currentTimeMillis();
        if (queue.size() < limit) {
            while (!waitQueue.isEmpty() && queue.size() <= limit) {
                queue.add(waitQueue.poll());
            }
        }

        while (!queue.isEmpty()) {
            try {
                queue.poll().run();
            } catch (Exception ignored) {

            }
        }

        long t = System.currentTimeMillis() - start;
        if (t > exceptWaitTime && limit > minLimit) {
            if (t / exceptWaitTime > 2) {
                if (limit > 3) {
                    limit >>>= 1;
                } else {
                    limit -= 2;
                }
            } else {
                limit--;
            }

        } else if (t < exceptWaitTime && limit < maxLimit) {
            limit++;
        }
    }

    public static void runTask(Runnable runnable) {
        queue.add(runnable);
    }

    public static void clearQueue() {
        queue.clear();
        distanceQueue.clear();
        waitQueue.clear();
        nearPosSet.clear();
    }

    public static Runnable poll() {
        return queue.poll();
    }

    public static boolean isQueueEmpty() {
        return queue.isEmpty();
    }


    /**
     * @param runnable The task to be add to the render thread.
     * @return Whether the task will be done at the following render time point.
     * If the task is not one that should be executed immediately at the following render time, and the task is a heavy one,
     * the task will be executed at any time after the render time point. In the baddest way, this task may not be done in the
     * future.
     */
    public static boolean tryAddTask(Runnable runnable) {
        if (queue.size() < limit) {
            runTask(runnable);
            return true;
        }
        waitQueue.add(runnable);
        return false;
    }

    public static void addNextFrameRenderObj(@Nullable ImageEntity entity, Vector3i pos, double distance) {
        distanceQueue.add(ImageEntityCache.ImageWithDistance.create(entity, pos, distance));
    }

    private static int maxRenderObjCount = 30;

    public static void updateQuerySet() {
        nearPosSet.clear();
        int i = 0;
        while (!distanceQueue.isEmpty()) {
            ImageEntityCache.ImageWithDistance imageWithDistance = distanceQueue.poll();
            if (i++ < maxRenderObjCount) {
                nearPosSet.add(imageWithDistance.pos);
            }
            if (i == maxRenderObjCount) {
                farthestPos = imageWithDistance.pos;
            }
            ImageEntityCache.ImageWithDistance.recycle(imageWithDistance);
        }
    }

    public static boolean isRenderRange(BlockPos pos) {
        return nearPosSet.contains(pos.toLong());
    }

    public static boolean isNearer(BlockPos pos, BlockPos viewer) {
        return pos.distanceSq(viewer) < viewer.distanceSq(BlockPos.fromLong(farthestPos));
    }

    public static void setMaxRenderObjCount(int maxRenderObjCount) {
        RenderQueue.maxRenderObjCount = maxRenderObjCount;
    }

}
