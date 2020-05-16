package com.company;

import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * This class stores crawler info
 * (max depth, unhandled and
 * handled pages). Can be
 * processed by multiple
 * threads.
 *
 * @author Stoyalov Arseny BVT1803
 */
public class URLPool {

    private Set<WebPage> unhandled = new HashSet<>();

    private Set<URL> handled = new HashSet<>();

    private int maxDepth;

    private int waitingThreads;

    public URLPool(URL url, int maxDepth) {
        this.maxDepth = maxDepth;
        unhandled.add(new WebPage(url, 0));
    }

    public synchronized int getWaitingThreads() {
        return waitingThreads;
    }

    public synchronized Set<WebPage> getUnhandled() {
        return unhandled;
    }

    public synchronized Set<URL> getHandled() {
        return handled;
    }

    public synchronized int getMaxDepth() {
        return maxDepth;
    }

    public synchronized void addUnhandledPage(WebPage page) {
        if (waitingThreads > 0) {
            waitingThreads--;
        }
        unhandled.add(page);
        notify();
    }

    public synchronized WebPage getUnhandledPage() {

        try {
            while (unhandled.isEmpty()) {
                waitingThreads++;
                wait();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Iterator<WebPage> i = unhandled.iterator();
        WebPage page = i.next();
        i.remove();

        return page;
    }

    public synchronized void addHandledPage(WebPage page) {
        handled.add(page.getUrl());
    }

}
