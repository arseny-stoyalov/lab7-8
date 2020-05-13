package com.company;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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

    private static boolean done;

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
        List<Future<?>> futures = new ArrayList<>();
        List<Runnable> tasks = new ArrayList<>(threadNumber);

        for (int i = 0; i < threadNumber; i++) {
            tasks.add(new CrawlerTask(urlPool, serverAnswerTime));
        }

        Thread timer = new Thread(() -> {

            while (urlPool.getWaitingThreads() != threadNumber)
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            stop();
        });
        timer.start();

        while (!done) {
            Iterator<Runnable> iterator = tasks.iterator();
            int unavailableThreads = futures.size();
            for (int i = 0; i < tasks.size() - unavailableThreads; i++) {
                futures.add(executor.submit(iterator.next()));

            }
            boolean someDone = false;
            while (!someDone && !done)
                someDone = futures.stream().anyMatch(Future::isDone);
            futures.removeIf(Future::isDone);
        }
        executor.shutdownNow();
        Set<URL> res = urlPool.getHandled();
        System.out.println(res);

    }

    private static void stop() {
        done = true;
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
