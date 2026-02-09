package com.example.ainews.domain.search;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.ainews.domain.article.Article;
import com.example.ainews.domain.article.ArticleRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class ArticleIndexService {

    private static final Logger log = LoggerFactory.getLogger(ArticleIndexService.class);
    private static final int BATCH_SIZE = 500;

    private final ArticleRepository articleRepository;
    private final ArticleSearchRepository searchRepository;

    private volatile LocalDateTime lastSyncTime;

    public ArticleIndexService(ArticleRepository articleRepository, ArticleSearchRepository searchRepository) {
        this.articleRepository = articleRepository;
        this.searchRepository = searchRepository;
    }

    @PostConstruct
    public void init() {
        searchRepository.createIndexIfNotExists();
        lastSyncTime = LocalDateTime.now().minusDays(1);
        log.info("ArticleIndexService initialized. Index ready.");
    }

    @Scheduled(fixedDelay = 300000)
    public void incrementalSync() {
        log.info("Starting incremental sync since {}", lastSyncTime);
        LocalDateTime syncStart = LocalDateTime.now();

        int totalIndexed = 0;
        boolean hasMore = true;
        int page = 0;

        while (hasMore) {
            Slice<Article> slice = articleRepository.findForIndexingSince(
                    lastSyncTime, PageRequest.of(page, BATCH_SIZE));

            List<ArticleDocument> documents = new ArrayList<>();
            for (Article article : slice.getContent()) {
                documents.add(ArticleDocument.from(article));
            }

            if (!documents.isEmpty()) {
                searchRepository.bulkIndex(documents);
                totalIndexed += documents.size();
            }

            hasMore = slice.hasNext();
            page++;
        }

        lastSyncTime = syncStart;
        log.info("Incremental sync completed. Indexed {} articles", totalIndexed);
    }

    public void fullReindex() {
        log.info("Starting full reindex");

        searchRepository.deleteIndex();
        searchRepository.createIndexIfNotExists();

        int totalIndexed = 0;
        boolean hasMore = true;
        int page = 0;

        while (hasMore) {
            Slice<Article> slice = articleRepository.findForIndexingSince(
                    LocalDateTime.of(1970, 1, 1, 0, 0), PageRequest.of(page, BATCH_SIZE));

            List<ArticleDocument> documents = new ArrayList<>();
            for (Article article : slice.getContent()) {
                documents.add(ArticleDocument.from(article));
            }

            if (!documents.isEmpty()) {
                searchRepository.bulkIndex(documents);
                totalIndexed += documents.size();
            }

            hasMore = slice.hasNext();
            page++;
        }

        lastSyncTime = LocalDateTime.now();
        log.info("Full reindex completed. Indexed {} articles", totalIndexed);
    }
}
