package top.nowandfuture.mod.imagesign.net;

import org.apache.logging.log4j.util.Strings;

import java.net.InetSocketAddress;

/**
 * The Proxy class is a wrapper for network proxy in jdk.
 */
public class Proxy {
    private String httpProxy;
    private int httpPort;

    private String httpsProxy;
    private int httpsPort;

    private String socksProxy;
    private int socksPort;


    public Proxy(String httpProxy,String httpsProxy, String socksProxy) {
        this.httpProxy = httpProxy;
        this.httpPort = 0;
        this.httpsProxy = httpsProxy;
        this.httpsPort = 0;
        this.socksProxy = socksProxy;
        this.socksPort = 0;
    }

    public Proxy(String httpProxy, int httpPort, String httpsProxy, int httpsPort, String socksProxy, int socksPort) {
        this.httpProxy = httpProxy;
        this.httpPort = httpPort;
        this.httpsProxy = httpsProxy;
        this.httpsPort = httpsPort;
        this.socksProxy = socksProxy;
        this.socksPort = socksPort;
    }

    public java.net.Proxy getProxyIns(){
        if(!Strings.isEmpty(socksProxy)){
            return new java.net.Proxy(java.net.Proxy.Type.SOCKS, new InetSocketAddress(socksProxy, socksPort));
        }else if(!Strings.isEmpty(httpsProxy)){
            return new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(httpsProxy, httpsPort));
        }else if(!Strings.isEmpty(httpProxy)){
            return new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(httpProxy, httpPort));
        }else{
            return java.net.Proxy.NO_PROXY;
        }
    }

    public String getHttpsProxy() {
        return httpsProxy;
    }

    public void setHttpsProxy(String httpsProxy) {
        this.httpsProxy = httpsProxy;
    }

    public String getHttpProxy() {
        return httpProxy;
    }

    public void setHttpProxy(String httpProxy) {
        this.httpProxy = httpProxy;
    }

    public String getSocksProxy() {
        return socksProxy;
    }

    public void setSocksProxy(String socksProxy) {
        this.socksProxy = socksProxy;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public void setHttpsPort(int httpsPort) {
        this.httpsPort = httpsPort;
    }

    public void setSocksPort(int socksPort) {
        this.socksPort = socksPort;
    }
}
