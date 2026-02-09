package com.example.ainews.domain.analysis.dto;

import java.time.LocalDateTime;

import com.example.ainews.domain.analysis.ImageAnalysis;

public record ImageAnalysisResponse(
        Long id,
        Long articleId,
        String imageUrl,
        String imageHash,
        String apiName,
        Float aiProbability,
        LocalDateTime analyzedAt
) {

    public static ImageAnalysisResponse from(ImageAnalysis analysis) {
        return new ImageAnalysisResponse(
                analysis.getId(),
                analysis.getArticle().getId(),
                analysis.getImageUrl(),
                analysis.getImageHash(),
                analysis.getApiName(),
                analysis.getAiProbability(),
                analysis.getAnalyzedAt()
        );
    }
}
