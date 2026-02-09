package com.example.ainews.domain.search;

import java.time.LocalDateTime;
import java.util.List;

import com.example.ainews.domain.analysis.ImageAnalysis;
import com.example.ainews.domain.article.Article;
import com.example.ainews.domain.author.Author;
import com.example.ainews.domain.provider.Provider;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class ArticleDocumentTest {

    @Test
    void from_shouldMapBasicFields() {
        Provider provider = new Provider("조선일보");
        Author author = new Author(provider, "홍길동", "hong@example.com");
        LocalDateTime publishedAt = LocalDateTime.of(2025, 1, 15, 10, 0);

        Article article = new Article(provider, author, "AI 기사 제목", "기사 본문 내용",
                "https://example.com/1", publishedAt);
        ReflectionTestUtils.setField(article, "id", 1L);
        article.setPortal("naver");

        ArticleDocument doc = ArticleDocument.from(article);

        assertEquals(1L, doc.getId());
        assertEquals("AI 기사 제목", doc.getTitle());
        assertEquals("기사 본문 내용", doc.getContent());
        assertEquals("https://example.com/1", doc.getSourceUrl());
        assertEquals(publishedAt, doc.getPublishedAt());
        assertNotNull(doc.getCrawledAt());
        assertEquals("naver", doc.getPortal());
        assertEquals("조선일보", doc.getProviderName());
        assertEquals("홍길동", doc.getAuthorName());
    }

    @Test
    void from_withNullAuthor_shouldSetAuthorNameNull() {
        Provider provider = new Provider("중앙일보");
        Article article = new Article(provider, null, "제목", "본문",
                "https://example.com/2", LocalDateTime.now());
        ReflectionTestUtils.setField(article, "id", 2L);

        ArticleDocument doc = ArticleDocument.from(article);

        assertEquals("중앙일보", doc.getProviderName());
        assertNull(doc.getAuthorName());
    }

    @Test
    void from_withNoImages_shouldSetDefaultValues() {
        Provider provider = new Provider("한겨레");
        Article article = new Article(provider, null, "제목", "본문",
                "https://example.com/3", LocalDateTime.now());
        ReflectionTestUtils.setField(article, "id", 3L);

        ArticleDocument doc = ArticleDocument.from(article);

        assertEquals(0, doc.getImageCount());
        assertEquals(0f, doc.getMaxAiProbability());
        assertFalse(doc.isHasAiImages());
    }

    @Test
    void from_withHighAiProbability_shouldDetectAiImages() {
        Provider provider = new Provider("동아일보");
        Article article = new Article(provider, null, "제목", "본문",
                "https://example.com/4", LocalDateTime.now());
        ReflectionTestUtils.setField(article, "id", 4L);

        ImageAnalysis analysis1 = new ImageAnalysis(article, "https://img/1.jpg", "hash1", "sightengine", 0.3f);
        ImageAnalysis analysis2 = new ImageAnalysis(article, "https://img/2.jpg", "hash2", "sightengine", 0.85f);
        ReflectionTestUtils.setField(article, "imageAnalyses", List.of(analysis1, analysis2));

        ArticleDocument doc = ArticleDocument.from(article);

        assertEquals(2, doc.getImageCount());
        assertEquals(0.85f, doc.getMaxAiProbability(), 0.001f);
        assertTrue(doc.isHasAiImages());
    }

    @Test
    void from_withLowAiProbability_shouldNotDetectAiImages() {
        Provider provider = new Provider("경향신문");
        Article article = new Article(provider, null, "제목", "본문",
                "https://example.com/5", LocalDateTime.now());
        ReflectionTestUtils.setField(article, "id", 5L);

        ImageAnalysis analysis = new ImageAnalysis(article, "https://img/1.jpg", "hash1", "sightengine", 0.3f);
        ReflectionTestUtils.setField(article, "imageAnalyses", List.of(analysis));

        ArticleDocument doc = ArticleDocument.from(article);

        assertEquals(1, doc.getImageCount());
        assertEquals(0.3f, doc.getMaxAiProbability(), 0.001f);
        assertFalse(doc.isHasAiImages());
    }

    @Test
    void from_withNullAiProbability_shouldHandleGracefully() {
        Provider provider = new Provider("매일경제");
        Article article = new Article(provider, null, "제목", "본문",
                "https://example.com/6", LocalDateTime.now());
        ReflectionTestUtils.setField(article, "id", 6L);

        ImageAnalysis analysis = new ImageAnalysis(article, "https://img/1.jpg", "hash1", "sightengine", null);
        ReflectionTestUtils.setField(article, "imageAnalyses", List.of(analysis));

        ArticleDocument doc = ArticleDocument.from(article);

        assertEquals(1, doc.getImageCount());
        assertEquals(0f, doc.getMaxAiProbability());
        assertFalse(doc.isHasAiImages());
    }
}
