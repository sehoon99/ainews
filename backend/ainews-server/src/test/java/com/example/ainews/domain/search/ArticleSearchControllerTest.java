package com.example.ainews.domain.search;

import java.time.LocalDateTime;
import java.util.List;

import com.example.ainews.config.OpenSearchConfig;
import com.example.ainews.config.OpenSearchProperties;
import com.example.ainews.domain.search.dto.ArticleSearchRequest;
import com.example.ainews.domain.search.dto.ArticleSearchResponse;
import com.example.ainews.domain.search.dto.ArticleSearchResponse.ArticleSearchHit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ArticleSearchController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {OpenSearchConfig.class, OpenSearchProperties.class}
        )
)
class ArticleSearchControllerTest {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ArticleSearchService searchService;

    @MockBean
    private ArticleIndexService indexService;

    @Test
    void searchArticles_withQuery_shouldReturnResults() throws Exception {
        ArticleSearchHit hit = new ArticleSearchHit(
                1L, "AI 뉴스 제목", "기사 내용 스니펫...",
                "https://example.com/1", LocalDateTime.of(2025, 1, 15, 10, 0),
                "naver", "조선일보", "홍길동",
                false, 0.3f, 1, 5.5f);

        ArticleSearchResponse response = new ArticleSearchResponse(
                List.of(hit), 1L, 0, 20, 1);

        when(searchService.search(any(ArticleSearchRequest.class))).thenReturn(response);

        mockMvc.perform(get("/api/search/articles")
                        .param("query", "AI"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHits").value(1))
                .andExpect(jsonPath("$.hits[0].id").value(1))
                .andExpect(jsonPath("$.hits[0].title").value("AI 뉴스 제목"))
                .andExpect(jsonPath("$.hits[0].portal").value("naver"))
                .andExpect(jsonPath("$.hits[0].providerName").value("조선일보"));
    }

    @Test
    void searchArticles_withAllFilters_shouldPassParameters() throws Exception {
        ArticleSearchResponse response = new ArticleSearchResponse(
                List.of(), 0L, 0, 20, 0);

        when(searchService.search(any(ArticleSearchRequest.class))).thenReturn(response);

        mockMvc.perform(get("/api/search/articles")
                        .param("query", "AI")
                        .param("portal", "naver")
                        .param("providerName", "조선일보")
                        .param("minAiProbability", "0.7")
                        .param("hasAiImages", "true")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHits").value(0));

        verify(searchService).search(any(ArticleSearchRequest.class));
    }

    @Test
    void searchArticles_withNoParams_shouldReturnOk() throws Exception {
        ArticleSearchResponse response = new ArticleSearchResponse(
                List.of(), 0L, 0, 20, 0);

        when(searchService.search(any(ArticleSearchRequest.class))).thenReturn(response);

        mockMvc.perform(get("/api/search/articles"))
                .andExpect(status().isOk());
    }

    @Test
    void reindex_shouldCallFullReindex() throws Exception {
        mockMvc.perform(post("/api/search/reindex"))
                .andExpect(status().isOk())
                .andExpect(content().string("Full reindex started"));

        verify(indexService).fullReindex();
    }
}
