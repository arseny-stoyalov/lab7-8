package com.company;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * This is an entry point class which
 * reads a URL, maximum depth,
 * threads number, waiting time limit values
 * from console, then prints out a list
 * of sites the crawler with this parameters
 * visited. Starts several threads to
 * complete this task
 *
 * @author Stoyalov Arseny BVT1803
 */
public class Main {

    //http://e-m-b.org/
    public static void main(String[] args) throws IOException {

        //---------------Getting all parameters from console--------------------
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        final URL url = getUrlParameterFromConsole(in);
        final int maxDepth = getNumericParameterFromConsole(in, "max depth");
        final int threadNumber = getNumericParameterFromConsole(in, "number of threads");
        final int serverAnswerTime = getNumericParameterFromConsole(in, "server answer time limit(sec)");
        in.close();
        //------------------------Got all the parameters-------------------------------

        URLPool urlPool = new URLPool(url, maxDepth);

        ExecutorService executor = Executors.newFixedThreadPool(threadNumber);

        CrawlerTask task = new CrawlerTask(urlPool, serverAnswerTime);

        Thread timer = new Thread(() -> {

            while (urlPool.getWaitingThreads() != threadNumber)
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            urlPool.setDone(true);
        });
        timer.start();

        for (int i = 0; i < threadNumber; i++)
            executor.submit(task);

        while (urlPool.isNotDone())
            Thread.yield();

        executor.shutdownNow();

        Set<WebPage> res = urlPool.getHandled();
        System.out.println(res.size() + " : " + res);

    }

    private static int getNumericParameterFromConsole(BufferedReader in, String value) throws IOException {

        System.out.println("Enter " + value);
        int num = -1;
        while (num <= 0) {
            try {
                num = Integer.parseInt(in.readLine());
                if (num <= 0) throw new NumberFormatException("Invalid " + value + " value");
            } catch (NumberFormatException e) {
                System.out.println("Please enter valid " + value);
            }
        }

        return num;
    }

    private static URL getUrlParameterFromConsole(BufferedReader in) throws IOException {

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

        return url;
    }

}
