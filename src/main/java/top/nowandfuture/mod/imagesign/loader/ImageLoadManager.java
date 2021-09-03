package top.nowandfuture.mod.imagesign.loader;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.disposables.Disposable;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import top.nowandfuture.mod.imagesign.RenderQueue;

import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;

//Thread Save
public enum ImageLoadManager {
    INSTANCE;
    private final ConcurrentHashMap<Long, Disposable> loadingMap;
    private final PriorityQueue<ImageLoadTask> toLoadQueue;
    private final LongSet posRecord;

    ImageLoadManager(){
        loadingMap = new ConcurrentHashMap<>();
        toLoadQueue = new PriorityQueue<>();
        posRecord = new LongArraySet();
    }

    public boolean isLoading(Long entityPos){
        return loadingMap.containsKey(entityPos);
    }

    public void addToLoadingList(Long entityPos, Disposable disposable){
        if(!isLoading(entityPos)) {
            loadingMap.put(entityPos, disposable);
        }
    }

    public void removeFromLoadingList(Long entityPos){
        Disposable disposable = loadingMap.remove(entityPos);
        if(disposable != null){
            disposable.dispose();
        }
    }

    public boolean tryRemoveFromLoadingList(Long entityPos){
        if(isLoading(entityPos)){
            removeFromLoadingList(entityPos);
            return true;
        }
        return false;
    }

    public void clear(IWorld world) {
        synchronized (this) {
            loadingMap.forEach((entityPos, disposable) -> {
                if (world != null && world.getTileEntity(BlockPos.fromLong(entityPos)) != null) {
                    disposable.dispose();
                }
            });

            loadingMap.clear();
            posRecord.clear();
            toLoadQueue.clear();
        }
    }

    public synchronized void addToLoad(@NonNull ImageLoadTask task){
        long pos = task.getPos().toLong();
        if(!posRecord.contains(pos) && !isLoading(pos)) {
            toLoadQueue.add(task);
            posRecord.add(pos);
        }
    }

    private static int MAX_LOAD_COUNT = 20;
    public synchronized void runLoadTasks(){
        while (!toLoadQueue.isEmpty()){
            ImageLoadTask loadTask = toLoadQueue.poll();
            BlockPos blockPos = loadTask.getPos();
            posRecord.remove(blockPos.toLong());
            if(!isLoading(blockPos.toLong())
                    && loadingMap.size() < MAX_LOAD_COUNT) {
                loadTask.run();
            }
        }
    }
}
