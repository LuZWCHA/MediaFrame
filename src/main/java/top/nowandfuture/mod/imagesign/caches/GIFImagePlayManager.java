package top.nowandfuture.mod.imagesign.caches;

import it.unimi.dsi.fastutil.longs.*;

import java.util.concurrent.atomic.AtomicLong;


//Make the same url sources has different gif start time.
public enum GIFImagePlayManager {
    INSTANCE;

    private final Long2LongMap long2LongMap;
    private final AtomicLong tick;

    GIFImagePlayManager() {
        long2LongMap = new Long2LongOpenHashMap();
        tick = new AtomicLong(0);
    }

    public void setStartTickForPos(long pos, long startTick){
        long2LongMap.put(pos, startTick);
    }

    public void remove(long pos){
        long2LongMap.remove(pos);
    }

    public long getStartTick(long pos){
        return long2LongMap.getOrDefault(pos, 0);
    }

    public boolean contains(long pos){
        return long2LongMap.containsKey(pos);
    }

    public void clear(){
        long2LongMap.clear();
    }

    public long tick(){
        return tick.addAndGet(1);
    }

    public long getTick(){
        return tick.get();
    }
}
