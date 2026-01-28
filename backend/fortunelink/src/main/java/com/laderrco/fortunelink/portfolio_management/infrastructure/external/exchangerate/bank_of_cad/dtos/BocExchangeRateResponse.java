package com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.bank_of_cad.dtos;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

// note data only given on weekdays, no weekends
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BocExchangeRateResponse {
    private Terms terms;

    private Map<String, SeriesDetail> seriesDetail;

    private List<Observation> observations;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Terms {
        private String url;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SeriesDetail {
        private String label;
        private String description;
        private Dimension dimension;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Dimension {
            private String key;
            private String name;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Observation {
        @JsonProperty("d")
        private String date;
        // the rate name in teh returned json is dynamic
        private Map<String, Rate> rates = new HashMap<>();

        @JsonAnySetter
        public void addRate(String key, Rate value) {
            if (!"d".equals(key)) { // skip the date property
                rates.put(key, value);
            }
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Rate {
            @JsonProperty("v")
            private BigDecimal value;
        }
    }
}
