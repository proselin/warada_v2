package com.github.warada_v2.crawl;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class CrawlApplication {
    public static void main(String... args) {
        Quarkus.run(args);
    }
}
