package com.meeran.newsanalyzerapi.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Records for creating the Gemini API Request
public final class GeminiDto {
    public record Part(String text) {}
    public record Content(List<Part> parts) {}
    public record GenerationConfig(@JsonProperty("response_mime_type") String responseMimeType) {}
    public record GeminiRequest(List<Content> contents, @JsonProperty("generation_config") GenerationConfig generationConfig) {}

    // Records for parsing the Gemini API Response
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeminiResponse(List<Candidate> candidates) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Candidate(Content content) {}
}