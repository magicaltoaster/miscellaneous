package com.fingy.scrape.security;

public class TorNetworkProxyBasedRefreshingScrapeDetectionOverride extends TorNetworkProxyBasedScrapeDetectionOverride {

    private final Thread proxyRefreshingThread = new ProxyRefreshingThread();

    @Override
    public void initializeContext() {
        super.initializeContext();
        proxyRefreshingThread.start();
    }

    @Override
    public void destroyContext() {
        proxyRefreshingThread.interrupt();
        super.destroyContext();
    }

    private class ProxyRefreshingThread extends Thread {
        private static final int DEFAULT_TIMEOUT_MILLIS = 180000;

        public ProxyRefreshingThread() {
            setDaemon(true);
        }

        @Override
        public void run() {
            try {
                doRun();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        public void doRun() throws InterruptedException {
            while (!interrupted()) {
                sleep(DEFAULT_TIMEOUT_MILLIS);
                setUpProxy();
            }
        }
    }

}
