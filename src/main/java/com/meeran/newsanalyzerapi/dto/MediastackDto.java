package com.meeran.newsanalyzerapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

public final class MediastackDto {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Article(String title, String description, String url) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(List<Article> data) {}
}