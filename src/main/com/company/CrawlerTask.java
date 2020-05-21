package com.company;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.List;

/**
 * This class represents Runnable task
 * for crawler. It finds gets a URL
 * from URLPool, searches for links
 * there (if it leads to an html page)
 * then updates URLPool instance by
 * adding found URLs as unhandled
 * URLs
 *
 * @author Arseny Stoyalov BVT1803
 */
public class CrawlerTask implements Runnable {

    private final URLPool pool;

    private final int serverAnswerTime;

    public CrawlerTask(URLPool pool, int serverAnswerTime) {
        this.pool = pool;
        this.serverAnswerTime = serverAnswerTime * 1000;
    }

    @Override
    public void run() {
        while (pool.isNotDone()) {
            WebPage pageToSearch = pool.getUnhandledPage();
            searchForUrls(pageToSearch);
            pool.addHandledPage(pageToSearch);
        }
    }

    /**
     * Searches for given page if its depth
     * is lesser than max depth of URLPool
     * instance. Adds all URLs (found at
     * this page)
     */
    private void searchForUrls(WebPage page) {

        if (page.getDepth() >= pool.getMaxDepth() - 1) return;

        try (Socket socket = new Socket(InetAddress.getByName(page.getUrl().getHost()), Crawler.PORT)) {
            socket.setSoTimeout(serverAnswerTime);

            URLConnection urlConnection = page.getUrl().openConnection();
            if (urlConnection.getContentType() != null && !urlConnection.getContentType().contains("text/html"))
                return;

            BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
            String pageFile = page.getUrl().getFile().equals("") ? "/" : page.getUrl().getFile();
            String request = "GET " + pageFile + " HTTP/1.1\n" +
                    "Host: " + page.getUrl().getHost() + "\n" +
                    "\n";
            for (byte c : request.getBytes()) {
                out.write(c);
            }
            out.flush();

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String line;
            while ((line = in.readLine()) != null) {
                List<String> foundUrls = Crawler.findHttpUrl(line);
                for (String foundUrl : foundUrls) {
                    WebPage newPage = new WebPage(new URL(foundUrl), page.getDepth() + 1);
                    pool.addUnhandledPage(newPage);
                    System.out.println(newPage);
                }
            }
        } catch (IOException e) {
            if (e instanceof SocketTimeoutException)
                System.out.println("Stopped waiting for server to answer due to entered time limit");
            else
                System.out.println("Got troubles reading html page " + page.getUrl().toString() + " " + e.toString());
        }


    }

}