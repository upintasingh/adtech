package com.adtech.insight.dto;

public enum MetricType {
    CLICKS("clicks", "CLICKS"),
    IMPRESSIONS("impressions", "IMPRESSIONS"),
    CLICK_TO_BASKET("clickToBasket", "CLICK_TO_BASKET");

    private final String attr;
    private final String column;

    MetricType(String attr, String column) {
        this.attr = attr;
        this.column = column;
    }
    public String attr() {
        return attr;
    }

    public String column() {
        return column;
    }
}
