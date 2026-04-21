package com.github.warada_v2.commons.enums;

public enum CrawlStatus {
    INIT("0000", "Initialize and can crawl"),
    STARTED("0001", "Started crawl and lock for no one can update"),
    FINISHED("9999", "Finished crawl");

    private final String code;
    private final String description;

    CrawlStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static CrawlStatus fromCode(String code) {
        for (CrawlStatus status : CrawlStatus.values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown code " + code);
    }
}
