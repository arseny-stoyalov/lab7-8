package com.company;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Crawler {

    private int maxDepth;

    private List<WebPage> handled;

    private List<WebPage> unhandled;

    public static final String URL_PREFIX = "http://";

    public static final int PORT = 80;

    public Crawler(URL startingURL, int maxDepth) {
        this.maxDepth = maxDepth;
        this.handled = new LinkedList<>();
        this.unhandled = new LinkedList<>();
        unhandled.add(new WebPage(startingURL, 0));
    }

    public List<WebPage> getSites() {

        while (!unhandled.isEmpty()) {
            WebPage page = unhandled.remove(0);
            handled.add(page);
            System.out.println(handled.size());
            searchForUrls(page);
        }
        return handled;
    }

    private void searchForUrls(WebPage page) {

        System.out.println("Checking... " + page.toString());

        if (page.getDepth() >= maxDepth - 1) return;
        try (Socket socket = new Socket(InetAddress.getByName(page.getUrl().getHost()), PORT)) {
            socket.setSoTimeout(10000);

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
                List<String> foundUrls = findHttpUrl(line);
                for (String foundUrl : foundUrls) {
                    WebPage newPage = new WebPage(new URL(foundUrl), page.getDepth() + 1);
                    if (!handled.contains(newPage) && !unhandled.contains(newPage))
                        System.out.println(unhandled.add(newPage));
                    System.out.println(newPage);
                }
            }
        } catch (IOException e) {
            System.out.println("Got troubles reading html page " + page.toString() + " " + e.toString());
        }
    }

    public List<String> findHttpUrl(String line) {

        List<String> matches = new ArrayList<>();
        if (line == null) return matches;
        Pattern pattern = Pattern.compile("<a.*?href=\"" + URL_PREFIX + ".+?\".*?>.*?</a>");
        Matcher matcher = pattern.matcher(line);
        while (matcher.find()) {
            matches.add(matcher.group());
        }
        for (int i = 0; i < matches.size(); i++) {
            Matcher matcherUrl = Pattern.compile("href=\"" + URL_PREFIX + ".+?\"").matcher(matches.get(i));
            if (matcherUrl.find()) {
                String resInQuotes = matcherUrl.group().substring(5);
                matches.set(i, resInQuotes.substring(1, resInQuotes.length() - 1));
            }
        }
        return matches;
    }

}
