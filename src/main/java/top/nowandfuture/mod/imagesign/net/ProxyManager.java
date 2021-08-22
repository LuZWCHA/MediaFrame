package top.nowandfuture.mod.imagesign.net;

import joptsimple.internal.Strings;

public enum ProxyManager {
    INSTANCE;
    private Proxy proxy;

    ProxyManager(){
//        proxy = new Proxy(Strings.EMPTY, Strings.EMPTY, Strings.EMPTY);
        proxy = new Proxy("127.0.0.1", Strings.EMPTY, Strings.EMPTY);
        proxy.setHttpPort(7890);
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    public Proxy getProxy() {
        return proxy;
    }

}
