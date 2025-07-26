package com.meeran.newsanalyzerapi.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NewsApiResponse(String status, Integer totalResults, List<Article> articles) {
}