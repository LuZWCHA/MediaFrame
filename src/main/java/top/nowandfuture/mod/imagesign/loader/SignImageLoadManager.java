package top.nowandfuture.mod.imagesign.loader;

import io.reactivex.rxjava3.disposables.Disposable;
import net.minecraft.tileentity.SignTileEntity;
import net.minecraft.world.IWorld;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;

public enum SignImageLoadManager {
    INSTANCE;
    private final Map<SignTileEntity, Disposable> signTileEntityListMap;

    SignImageLoadManager(){
        signTileEntityListMap = new HashMap<>();
    }

    public boolean isLoading(SignTileEntity entity){
        return signTileEntityListMap.containsKey(entity);
    }

    public void addToLoadingList(SignTileEntity entity, Disposable disposable){
        if(!isLoading(entity)) {
            signTileEntityListMap.put(entity, disposable);
        }
    }

    public void removeFromLoadingList(SignTileEntity entity){
        signTileEntityListMap.remove(entity);
    }

    public boolean tryRemoveFromLoadingList(SignTileEntity entity){
        if(isLoading(entity)){
            removeFromLoadingList(entity);
            return true;
        }
        return false;
    }

    public void clear(IWorld world) {
        signTileEntityListMap.forEach(new BiConsumer<SignTileEntity, Disposable>() {
            @Override
            public void accept(SignTileEntity entity, Disposable disposable) {
                if(world == entity.getWorld())
                    disposable.dispose();
            }
        });

        signTileEntityListMap.clear();
    }

    public void lazyClearRemovedEntities(){
        lazyClearRemovedEntities(-1);
    }

    private static final int K = 5;
    private Iterator<SignTileEntity> last;
    public void lazyClearRemovedEntities(int maxClearCount){
        if(maxClearCount <= 0 || maxClearCount > signTileEntityListMap.size()){
            maxClearCount = K;
        }

        if(last == null || !last.hasNext()){
            last = signTileEntityListMap.keySet()
                    .iterator();
        }

        int i = 0;
        while (last.hasNext() && i < maxClearCount){
            SignTileEntity entity = last.next();
            if(entity.isRemoved()){
                removeFromLoadingList(entity);
            }
            i++;
            if(!last.hasNext()){
                last = signTileEntityListMap.keySet()
                        .iterator();
            }
        }
    }
}
