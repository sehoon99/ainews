package com.example.ainews.domain.search;

import java.time.LocalDateTime;
import java.util.List;

import com.example.ainews.domain.article.Article;
import com.example.ainews.domain.article.ArticleRepository;
import com.example.ainews.domain.provider.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleIndexServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private ArticleSearchRepository searchRepository;

    @Captor
    private ArgumentCaptor<List<ArticleDocument>> documentsCaptor;

    private ArticleIndexService indexService;

    @BeforeEach
    void setUp() {
        indexService = new ArticleIndexService(articleRepository, searchRepository);
        // init()에서 PostConstruct로 호출되는 로직을 수동 실행
        ReflectionTestUtils.setField(indexService, "lastSyncTime", LocalDateTime.now().minusDays(1));
    }

    @Test
    void incrementalSync_withArticles_shouldBulkIndex() {
        Provider provider = new Provider("테스트언론사");
        Article article = new Article(provider, null, "제목", "본문",
                "https://example.com/1", LocalDateTime.now());
        ReflectionTestUtils.setField(article, "id", 1L);

        SliceImpl<Article> slice = new SliceImpl<>(List.of(article), PageRequest.of(0, 500), false);
        when(articleRepository.findForIndexingSince(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(slice);

        indexService.incrementalSync();

        verify(searchRepository).bulkIndex(documentsCaptor.capture());
        List<ArticleDocument> indexed = documentsCaptor.getValue();
        assertEquals(1, indexed.size());
        assertEquals(1L, indexed.get(0).getId());
        assertEquals("제목", indexed.get(0).getTitle());
    }

    @Test
    void incrementalSync_withNoArticles_shouldNotCallBulkIndex() {
        SliceImpl<Article> emptySlice = new SliceImpl<>(List.of(), PageRequest.of(0, 500), false);
        when(articleRepository.findForIndexingSince(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(emptySlice);

        indexService.incrementalSync();

        verify(searchRepository, never()).bulkIndex(any());
    }

    @Test
    void incrementalSync_withMultiplePages_shouldIndexAll() {
        Provider provider = new Provider("언론사");

        Article article1 = new Article(provider, null, "제목1", "본문1",
                "https://example.com/1", LocalDateTime.now());
        ReflectionTestUtils.setField(article1, "id", 1L);

        Article article2 = new Article(provider, null, "제목2", "본문2",
                "https://example.com/2", LocalDateTime.now());
        ReflectionTestUtils.setField(article2, "id", 2L);

        SliceImpl<Article> firstPage = new SliceImpl<>(List.of(article1), PageRequest.of(0, 500), true);
        SliceImpl<Article> secondPage = new SliceImpl<>(List.of(article2), PageRequest.of(1, 500), false);

        when(articleRepository.findForIndexingSince(any(LocalDateTime.class), eq(PageRequest.of(0, 500))))
                .thenReturn(firstPage);
        when(articleRepository.findForIndexingSince(any(LocalDateTime.class), eq(PageRequest.of(1, 500))))
                .thenReturn(secondPage);

        indexService.incrementalSync();

        verify(searchRepository, times(2)).bulkIndex(any());
    }

    @Test
    void fullReindex_shouldDeleteAndRecreateIndex() {
        SliceImpl<Article> emptySlice = new SliceImpl<>(List.of(), PageRequest.of(0, 500), false);
        when(articleRepository.findForIndexingSince(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(emptySlice);

        indexService.fullReindex();

        var inOrder = inOrder(searchRepository);
        inOrder.verify(searchRepository).deleteIndex();
        inOrder.verify(searchRepository).createIndexIfNotExists();
    }

    @Test
    void fullReindex_shouldIndexAllArticles() {
        Provider provider = new Provider("언론사");
        Article article = new Article(provider, null, "제목", "본문",
                "https://example.com/1", LocalDateTime.now());
        ReflectionTestUtils.setField(article, "id", 1L);

        SliceImpl<Article> slice = new SliceImpl<>(List.of(article), PageRequest.of(0, 500), false);
        when(articleRepository.findForIndexingSince(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(slice);

        indexService.fullReindex();

        verify(searchRepository).deleteIndex();
        verify(searchRepository).createIndexIfNotExists();
        verify(searchRepository).bulkIndex(documentsCaptor.capture());
        assertEquals(1, documentsCaptor.getValue().size());
    }
}
