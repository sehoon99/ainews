package com.example.ainews.domain.article.dto;

import java.time.LocalDateTime;

import com.example.ainews.domain.article.Article;

public record ArticleResponse(
        Long id,
        Integer providerId,
        Integer authorId,
        String title,
        String content,
        String sourceUrl,
        LocalDateTime publishedAt,
        LocalDateTime crawledAt
) {

    public static ArticleResponse from(Article article) {
        return new ArticleResponse(
                article.getId(),
                article.getProvider().getId(),
                article.getAuthor() != null ? article.getAuthor().getId() : null,
                article.getTitle(),
                article.getContent(),
                article.getSourceUrl(),
                article.getPublishedAt(),
                article.getCrawledAt()
        );
    }
}
