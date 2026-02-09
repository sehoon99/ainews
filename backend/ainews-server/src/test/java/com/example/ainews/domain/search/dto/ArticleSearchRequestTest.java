package com.example.ainews.domain.search.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArticleSearchRequestTest {

    @Test
    void negativePage_shouldDefaultToZero() {
        ArticleSearchRequest request = new ArticleSearchRequest(
                "AI", null, null, null, null, null, null, -1, 20);

        assertEquals(0, request.page());
    }

    @Test
    void zeroSize_shouldDefaultTo20() {
        ArticleSearchRequest request = new ArticleSearchRequest(
                "AI", null, null, null, null, null, null, 0, 0);

        assertEquals(20, request.size());
    }

    @Test
    void negativeSize_shouldDefaultTo20() {
        ArticleSearchRequest request = new ArticleSearchRequest(
                "AI", null, null, null, null, null, null, 0, -5);

        assertEquals(20, request.size());
    }

    @Test
    void oversizedSize_shouldDefaultTo20() {
        ArticleSearchRequest request = new ArticleSearchRequest(
                "AI", null, null, null, null, null, null, 0, 200);

        assertEquals(20, request.size());
    }

    @Test
    void validParameters_shouldPassThrough() {
        ArticleSearchRequest request = new ArticleSearchRequest(
                "검색어", "naver", "조선일보", null, null, 0.7f, true, 2, 50);

        assertEquals("검색어", request.query());
        assertEquals("naver", request.portal());
        assertEquals("조선일보", request.providerName());
        assertEquals(0.7f, request.minAiProbability());
        assertTrue(request.hasAiImages());
        assertEquals(2, request.page());
        assertEquals(50, request.size());
    }
}
