package top.nowandfuture.mod.imagesign.net;

import joptsimple.internal.Strings;

public enum ProxyManager {
    INSTANCE;
    private Proxy proxy;

    ProxyManager(){
        proxy = new Proxy(Strings.EMPTY, Strings.EMPTY, Strings.EMPTY);
    }

    public synchronized void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    public synchronized Proxy getProxy() {
        return proxy;
    }

}
