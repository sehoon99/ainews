package com.example.ainews.domain.search.dto;

import java.time.LocalDateTime;

public record ArticleSearchRequest(
        String query,
        String portal,
        String providerName,
        LocalDateTime publishedFrom,
        LocalDateTime publishedTo,
        Float minAiProbability,
        Boolean hasAiImages,
        int page,
        int size
) {
    public ArticleSearchRequest {
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 20;
    }
}
