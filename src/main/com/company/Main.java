package com.company;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

/**
 * This is an entry point class which
 * reads a URL and maximum depth values
 * from console, then prints out a list
 * of sites the crawler with this parameters
 * visited.
 *
 * @author Stoyalov Arseny BVT1803
 */
public class Main {

    //testing values
    //http://scratchpads.eu/explore/sites-list
    //http://e-m-b.org/
    public static void main(String[] args) throws IOException {

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Please enter a url to start scanning from");
        URL url = null;
        while (url == null) {
            try {
                url = new URL(in.readLine());
                if (!url.getProtocol().equals("http")) {
                    url = null;
                    throw new MalformedURLException("Wrong protocol");
                }

            } catch (MalformedURLException e) {
                System.out.println("Please enter a valid URL using http protocol");
            }
        }

        System.out.println("Now enter the depth of scanning");
        int maxDepth = -1;
        while (maxDepth <= 0) {
            try {
                maxDepth = Integer.parseInt(in.readLine());
                if (maxDepth <= 0) throw new NumberFormatException("Invalid depth value");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid value of max depth");
            }
        }
        in.close();

        Crawler crawler = new Crawler(url, maxDepth);

        Set<WebPage> result = crawler.getSites();

        System.out.println(result.size() + " : " + result.toString());
    }

}
