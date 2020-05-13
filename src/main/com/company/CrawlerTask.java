package com.company;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.List;

public class CrawlerTask implements Runnable {

    private URLPool pool;

    private int serverAnswerTime;

    public CrawlerTask(URLPool pool, int serverAnswerTime) {
        this.pool = pool;
        this.serverAnswerTime = serverAnswerTime;
    }

    @Override
    public void run() {
        WebPage pageToSearch = pool.getUnhandledPage();
        System.out.println("Checking... " + pageToSearch);
        searchForUrls(pageToSearch);
        pool.addHandledPage(pageToSearch);
        System.out.println("Done with " + pageToSearch);
    }

    private void searchForUrls(WebPage page) {

        StringBuilder content = new StringBuilder();
        String line;
        if (page.getDepth() >= pool.getMaxDepth() - 1 || pool.getHandled().contains(page.getUrl())) {
            return;
        }

        try (Socket socket = new Socket(InetAddress.getByName(page.getUrl().getHost()), Crawler.PORT)) {
            socket.setSoTimeout(serverAnswerTime * 1000);

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

            while ((line = in.readLine()) != null) {
                content.append(line);
                content.append(System.lineSeparator());
            }
        } catch (IOException e) {
            if (e instanceof SocketTimeoutException)
                System.out.println("Stopped waiting for server to answer due to entered time limit");
            else
                System.out.println("Got troubles reading html page " + page.getUrl().toString() + " " + e.toString());
        }

        List<String> foundUrls = Crawler.findHttpUrl(content.toString());
        for (String foundUrl : foundUrls) {
            WebPage newPage = null;
            try {
                newPage = new WebPage(new URL(foundUrl), page.getDepth() + 1);
            } catch (MalformedURLException e) {
                System.out.println("Got problems parsing link " + foundUrl);
            }
            pool.addUnhandledPage(newPage);
        }

    }

}