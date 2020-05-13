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
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class represent a crawler
 * which takes a URL, max depth to go into
 * and starts checking for links of a certain
 * protocol in an html-page, that URL leads to.
 * Then it tries to search for every link it found
 * and if they lead to an html-page it does the same thing
 * to it. Each iteration of searching crawler goes deeper
 * into the web. When the site it finds lays deeper than the max
 * depth (or the same depth as max) crawler stops
 *
 * @author Stoyalov Arseny BVT1803
 */
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

    /**
     * Searches for all websites it can find
     * on html-pages
     *
     * @return List of pages it visited
     */
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

    /**
     * Returns instance of Connection to connect
     * to database with properties from
     * configs.properties file
     *
     * @return Connection instance
     */
    private Connection getConnection() throws IOException, SQLException {

        String configsFileName = "configs.properties";
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(configsFileName)) {
            if (in != null)
                props.load(in);
            else
                throw new FileNotFoundException("Could not find " + configsFileName);
        }
        String user = props.getProperty("db.login");
        String password = props.getProperty("db.password");
        String url = props.getProperty("db.url");
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Updates database record of a url giving
     * it greater number of 'times occurred on
     * other sites' or creates a new record of
     * url if there wasn't any
     *
     * @param initialOccurrences sets number of occurrences for
     *                           a new record (it's 1 for every record except
     *                           the page crawler starts with)
     */
    public void updateDB(String url, int initialOccurrences) {
        String select = "SELECT occurrences FROM urls " +
                "WHERE url = '" + url + "'";
        String update = "UPDATE urls " +
                "SET occurrences = ? " +
                "WHERE url = '" + url + "'";
        String create = "INSERT INTO urls(url, occurrences) VALUES('" + url + "', " + initialOccurrences + ")";
        try (Connection connection = getConnection();
             Statement st = connection.createStatement()) {
            ResultSet res = st.executeQuery(select);
            if (res.next()) {
                int occurrences = res.getInt("occurrences");
                PreparedStatement pst = connection.prepareStatement(update);
                pst.setInt(1, ++occurrences);
                pst.executeUpdate();
                pst.close();
            } else {
                PreparedStatement pst = connection.prepareStatement(create);
                pst.executeUpdate();
                pst.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Searches for URL and if it finds an html-page
     * starts looking for others URLs there. Calls method
     * for updating database with visited pages
     */
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

    /**
     * Find all occurrences of '<a href="LINK"></a>'
     * in a given string and gets all the LINKS from it
     *
     * @return list of string links found
     */
    public static List<String> findHttpUrl(String line) {

        List<String> matches = new ArrayList<>();
        if (line == null) return matches;
        Pattern pattern = Pattern.compile("<a.*?href=\"" + URL_PREFIX + "\\S+?\".*?>.*?</a>");
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
