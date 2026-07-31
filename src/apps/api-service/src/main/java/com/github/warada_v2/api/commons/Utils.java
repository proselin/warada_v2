package com.github.warada_v2.api.commons;

public class Utils {
    public static String getExtensionFromUrl(String url) {
        String path = url.split("\\?")[0];
        int lastDot = path.lastIndexOf('.');
        if (lastDot > 0) {
            return path.substring(lastDot);
        }
        return ".jpg";
    }
}
