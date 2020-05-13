package com.company;

import java.net.URL;
import java.util.Objects;

/**
 * Entity that contains a URL and its depth.
 * Represents page that crawler needs to visit
 *
 * @author Stoyalov Arseny BVT1803
 */
public class WebPage {

    private URL url;

    private int depth;

    public WebPage(URL url, int depth) {
        this.url = url;
        this.depth = depth;
    }

    public URL getUrl() {
        return url;
    }

    public int getDepth() {
        return depth;
    }

    @Override
    public String toString() {
        return "URL: " + url.toString() + " depth: " + depth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebPage page = (WebPage) o;
        return url.toString().equals(page.url.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(url.toString());
    }

}
