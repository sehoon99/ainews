package com.example.ainews.domain.author.dto;

import com.example.ainews.domain.author.Author;

public record AuthorResponse(Integer id, Integer providerId, String name, String email, Integer articleCount) {

    public static AuthorResponse from(Author author) {
        return new AuthorResponse(
                author.getId(),
                author.getProvider().getId(),
                author.getName(),
                author.getEmail(),
                author.getArticleCount()
        );
    }
}
