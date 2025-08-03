package com.meeran.newsanalyzerapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

public final class NewsDataDto {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Article(String title, String description, String link) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(String status, int totalResults, List<Article> results) {}
}