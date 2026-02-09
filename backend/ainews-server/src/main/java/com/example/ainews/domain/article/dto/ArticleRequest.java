package com.example.ainews.domain.article.dto;

import java.time.LocalDateTime;

public record ArticleRequest(
        Integer providerId,
        Integer authorId,
        String title,
        String content,
        String sourceUrl,
        LocalDateTime publishedAt
) {
}
