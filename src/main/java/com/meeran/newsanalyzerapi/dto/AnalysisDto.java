package com.meeran.newsanalyzerapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public final class AnalysisDto {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProposedSolution(String title, String concept, String potentialImpact) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProblemAnalysis(
        String topic,
        String summary,
        String aggregatedProblem,
        String solutionProposal,
        String proposingViewpoint, // Bias from one side
        String opposingViewpoint,  // Bias from the other side
        String historicalPerspective,
        String motivationalProverb
    ) {}
}