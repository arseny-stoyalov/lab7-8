package com.company;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.sql.*;
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

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/crawler_urls";

    private static final String DB_USER = "crawler";

    private static final String DB_PASSWORD = "onlydoomiseternal";

    public Crawler(URL startingURL, int maxDepth) {
        this.maxDepth = maxDepth;
        this.handled = new LinkedList<>();
        this.unhandled = new LinkedList<>();
        unhandled.add(new WebPage(startingURL, 0));
    }

    public List<WebPage> getSites() {

        updateDB(unhandled.get(0).getUrl().toString(), -1);
        while (!unhandled.isEmpty()) {
            WebPage page = unhandled.remove(0);
            handled.add(page);
            System.out.println(handled.size());
            searchForUrls(page);
        }
        return handled;
    }

    public void updateDB(String url, int initialOccurrences) {
        String select = "SELECT occurrences FROM urls " +
                "WHERE url = '" +  url + "'";
        String update = "UPDATE urls " +
                "SET occurrences = ? " +
                "WHERE url = '" + url + "'";
        String create = "INSERT INTO urls(url, occurrences) VALUES('" + url + "', " + initialOccurrences + ")";
        try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement st = c.createStatement()) {
            ResultSet res = st.executeQuery(select);
            if (res.next()) {
                int occurrences = res.getInt("occurrences");
                PreparedStatement pst = c.prepareStatement(update);
                pst.setInt(1, ++occurrences);
                pst.executeUpdate();
                pst.close();
            } else {
                PreparedStatement pst = c.prepareStatement(create);
                pst.executeUpdate();
                pst.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void searchForUrls(WebPage page) {

        System.out.println("Checking... " + page.toString());

        if (page.getDepth() >= maxDepth - 1) return;
        try (Socket socket = new Socket(InetAddress.getByName(page.getUrl().getHost()), PORT)) {
            socket.setSoTimeout(10000);

            updateDB(page.getUrl().toString(), 1);
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
                        unhandled.add(newPage);
                    else updateDB(newPage.getUrl().toString(), 1);
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
