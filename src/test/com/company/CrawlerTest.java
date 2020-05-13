package com.company;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class CrawlerTest {

    private String htmlTestingValue1;

    private String htmlTestingValue2;

    @BeforeEach
    void setUp() throws MalformedURLException {
        htmlTestingValue1 = "<a href=\"http://goodValue.org\">Some info</a>" +
                "useless stuff here" +
                "<a href=\"http://anotherGoodValue.org\" some useless tags here></a>";
        htmlTestingValue2 = "<div class=\"description\">" +
                "<a href=\"http://openid.net/\">What is OpenID?</a></div>";
    }

    @Test
    void findHttpUrl() {
        List<String> expected = new ArrayList<>();
        expected.add("http://goodValue.org");
        expected.add("http://anotherGoodValue.org");
        assertArrayEquals(expected.toArray(), Crawler.findHttpUrl(htmlTestingValue1).toArray());
        expected.clear();
        expected.add("http://openid.net/");
        assertArrayEquals(expected.toArray(), Crawler.findHttpUrl(htmlTestingValue2).toArray());
    }

}