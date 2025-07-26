package com.meeran.newsanalyzerapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// This annotation prevents an error if the API adds new fields we don't use.
@JsonIgnoreProperties(ignoreUnknown = true)
public record Article(String title, String description, String url) {
}