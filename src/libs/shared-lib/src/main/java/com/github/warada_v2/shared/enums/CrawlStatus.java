package com.github.warada_v2.shared.enums;

public enum CrawlStatus {
    INIT("0000"),
    STARTED("0001"),
    FINISHED("9999");

    private final String code;

    CrawlStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static CrawlStatus fromCode(String code) {
        for (CrawlStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        throw new IllegalArgumentException("Unknown CrawlStatus code: " + code);
    }
}
