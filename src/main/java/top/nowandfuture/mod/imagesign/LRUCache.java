package top.nowandfuture.mod.imagesign;


import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
class LRUCache<K,V> extends LinkedHashMap<K,V>{
    private int capacity;

    public LRUCache(int capacity) {
        super(capacity, 0.75F, true);
        this.capacity = capacity;
    }

    public Map.Entry<K, V> getEldest() {
        return eldest;
    }

    Map.Entry<K,V> eldest;
    @Override
    protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
        boolean outer = size() > capacity;
        if(outer && put){
            this.eldest = eldest;
        }else{
            eldest = null;
        }
        return outer;
    }

    volatile boolean put = false;
    @Override
    public V put(K key, V value) {
        put = true;
        V res = super.put(key, value);
        put = false;
        return res;
    }

    @Override
    public void clear() {
        eldest = null;
        super.clear();
    }
}
