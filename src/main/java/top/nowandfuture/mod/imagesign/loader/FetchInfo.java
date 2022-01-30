package top.nowandfuture.mod.imagesign.loader;

public class FetchInfo implements IEvent{

    public Stage stage;
    public Object object;
    public String message;

    public FetchInfo(Stage stage, Object object, String message) {
        this.stage = stage;
        this.object = object;
        this.message = message;
    }

    public FetchInfo(Stage stage, String message) {
        this.stage = stage;
        this.message = message;
    }
}
