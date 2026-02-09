package com.example.ainews.domain.analysis.dto;

public record ImageAnalysisRequest(
        String imageUrl,
        String imageHash,
        String apiName,
        Float aiProbability
) {
}
