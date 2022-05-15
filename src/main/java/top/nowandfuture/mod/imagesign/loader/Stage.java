package top.nowandfuture.mod.imagesign.loader;

public enum Stage {
    DOWNLOADING("downloading", 0), //from disk or network
    CACHING("caching", 1), //cache the image into disk
    LOADING("loading", 2), //load from memory
    UPLOADING("uploading", 3), //upload the image to gpu
    FAILED("failed", 4), //failed at any stage
    SUCCESS("success", 5),
    IDLE("idle", -1);

    String str;
    int progress;
    private static int total = 5;

    Stage(String name, int progress){
        this.str = name;
        this.progress = progress;
    }

    public static int totalStageNum(){
        return total;
    }

    public String getStr() {
        return str;
    }

    public int getProgress() {
        return progress;
    }

    @Override
    public String toString() {
        return "Stage{" +
                "str='" + str + '\'' +
                ", progress=" + progress +
                '}';
    }
}