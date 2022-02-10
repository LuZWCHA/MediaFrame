package top.nowandfuture.mod.imagesign.caches;


import java.util.LinkedHashMap;
import java.util.Map;

//Get the eldest removed entry...
class LRUCache<K,V> extends LinkedHashMap<K,V>{
    private final int capacity;

    public LRUCache(int capacity) {
        super(capacity, 0.75F, true);
        this.capacity = capacity;
    }

    public Map.Entry<K, V> getEldest() {
        return eldest;
    }

    private Map.Entry<K,V> eldest;
    @Override
    protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
        boolean outer = size() > capacity;
        if(outer && put){
            this.eldest = eldest;
        }else{
            this.eldest = null;
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
