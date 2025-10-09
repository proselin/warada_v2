package com.github.warada_v2.commons;

public class Utils {
    public static String getExtensionFromUrl(String url) {
        // Try to get extension from URL
        String path = url.split("\\?")[0]; // Remove query params
        int lastDot = path.lastIndexOf('.');
        if (lastDot > 0) {
            return path.substring(lastDot);
        }
        return ".jpg"; // Default
    }
}
