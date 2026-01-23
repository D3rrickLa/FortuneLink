package com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.bank_of_cad;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class BocUrlBuilder {

    private final String baseUrl;
    private final StringBuilder path = new StringBuilder();
    private final Map<String, String> queryParams = new LinkedHashMap<>();

    public BocUrlBuilder(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public BocUrlBuilder observations(String... seriesNames) {
        String joined = String.join(",", seriesNames);
        path.append("/observations/").append(joined);
        return this;
    }

    public BocUrlBuilder format(String format) {
        path.append("/").append(format);
        return this;
    }

    public BocUrlBuilder recent(int count) {
        queryParams.put("recent", String.valueOf(count));
        return this;
    }

    public BocUrlBuilder startDate(LocalDate date) {
        queryParams.put("start_date", date.toString());
        return this;
    }

    public BocUrlBuilder endDate(LocalDate date) {
        queryParams.put("end_date", date.toString());
        return this;
    }

    public String build() {
        String query = queryParams.isEmpty()
                ? ""
                : "?" + queryParams.entrySet()
                    .stream()
                    .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                    .collect(Collectors.joining("&"));

        return baseUrl + path + query;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}