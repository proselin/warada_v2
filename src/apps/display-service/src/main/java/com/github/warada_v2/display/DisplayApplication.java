package com.github.warada_v2.display;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class DisplayApplication {
    public static void main(String... args) {
        Quarkus.run(args);
    }
}
