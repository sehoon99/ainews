package com.example.ainews.domain.search.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ArticleSearchResponse(
        List<ArticleSearchHit> hits,
        long totalHits,
        int page,
        int size,
        int totalPages
) {

    public record ArticleSearchHit(
            Long id,
            String title,
            String contentSnippet,
            String sourceUrl,
            LocalDateTime publishedAt,
            String portal,
            String providerName,
            String authorName,
            boolean hasAiImages,
            float maxAiProbability,
            int imageCount,
            float score
    ) {
    }
}
