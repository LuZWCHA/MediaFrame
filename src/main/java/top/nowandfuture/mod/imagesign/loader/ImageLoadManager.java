package top.nowandfuture.mod.imagesign.loader;

import com.ibm.icu.impl.Pair;
import io.netty.util.internal.ConcurrentSet;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.disposables.Disposable;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

//Thread Safe
public enum ImageLoadManager {
    INSTANCE();
    private final ConcurrentHashMap<Long, Pair<Disposable, String>> loadingMap;
    private final ConcurrentSet<String> loadingUrlsSet;

    private final PriorityQueue<ImageLoadTask> toLoadQueue;
    private final LongSet posRecord;

    ImageLoadManager(){
        this.loadingMap = new ConcurrentHashMap<>();
        this.loadingUrlsSet = new ConcurrentSet<>();
        this.toLoadQueue = new PriorityQueue<>();
        this.posRecord = new LongArraySet();
    }

    public boolean isLoading(String url){
        return loadingUrlsSet.contains(url);
    }

    public boolean isLoading(long pos){
        return loadingMap.containsKey(pos);
    }

    public void addToLoadingList(Long entityPos, String url, Disposable disposable){
        if(!isLoading(url)) {
            loadingMap.put(entityPos, Pair.of(disposable, url));
            loadingUrlsSet.add(url);
        }
    }

    public void removeFromLoadingList(Long entityPos){
        Pair<Disposable, String> pair = loadingMap.remove(entityPos);
        if(pair != null){
            pair.first.dispose();
            loadingUrlsSet.remove(pair.second);
        }

    }

    public boolean tryRemoveFromLoadingList(Long entityPos){
        if(isLoading(entityPos)){
            removeFromLoadingList(entityPos);
            return true;
        }
        return false;
    }

    public void clear(BiConsumer<Long, Pair<Disposable, String>> disposeConsumer) {
        synchronized (this) {
            loadingMap.forEach(disposeConsumer);

            loadingMap.clear();
            posRecord.clear();
            toLoadQueue.clear();
        }
    }

    public synchronized void addToLoad(@NonNull ImageLoadTask task){
        long pos = task.getIdentifier();
        String url = task.getUrl();
        if(!posRecord.contains(pos) && !isLoading(url)) {
            toLoadQueue.add(task);
            posRecord.add(pos);
        }
    }

    private static int MAX_LOAD_COUNT = 20;
    public synchronized void runLoadTasks(){
        while (!toLoadQueue.isEmpty()){
            ImageLoadTask loadTask = toLoadQueue.poll();
            long blockPos = loadTask.getIdentifier();
            posRecord.remove(blockPos);
            if(!isLoading(loadTask.getUrl())
                    && loadingMap.size() < MAX_LOAD_COUNT) {
                loadTask.run();
            }
        }
    }
}
