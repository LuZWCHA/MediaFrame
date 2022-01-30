package top.nowandfuture.mod.imagesign.loader;

import java.util.LinkedHashMap;
import java.util.Map;

public class LoadStageLangKeyMap {

    public static Map<Stage, String> langKeyMap;
    static {
        langKeyMap = new LinkedHashMap<>();
        langKeyMap.put(Stage.IDLE, "imagesign.stage.idle.name");
        langKeyMap.put(Stage.UPLOADING, "imagesign.stage.uploading.name");
        langKeyMap.put(Stage.FAILED, "imagesign.stage.failed.name");
        langKeyMap.put(Stage.CACHING, "imagesign.stage.caching.name");
        langKeyMap.put(Stage.DOWNLOADING, "imagesign.stage.downloading.name");
        langKeyMap.put(Stage.LOADING, "imagesign.stage.loading.name");
    }

    public static String key(Stage stage){
        return langKeyMap.getOrDefault(stage, "imagesign.stage.idle.name");
    }
}
