package com.example.ainews.domain.search;

import java.time.LocalDateTime;

import com.example.ainews.domain.search.dto.ArticleSearchRequest;
import com.example.ainews.domain.search.dto.ArticleSearchResponse;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class ArticleSearchController {

    private final ArticleSearchService searchService;
    private final ArticleIndexService indexService;

    public ArticleSearchController(ArticleSearchService searchService, ArticleIndexService indexService) {
        this.searchService = searchService;
        this.indexService = indexService;
    }

    @GetMapping("/articles")
    public ResponseEntity<ArticleSearchResponse> searchArticles(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String portal,
            @RequestParam(required = false) String providerName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime publishedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime publishedTo,
            @RequestParam(required = false) Float minAiProbability,
            @RequestParam(required = false) Boolean hasAiImages,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        ArticleSearchRequest request = new ArticleSearchRequest(
                query, portal, providerName, publishedFrom, publishedTo,
                minAiProbability, hasAiImages, page, size);

        return ResponseEntity.ok(searchService.search(request));
    }

    @PostMapping("/reindex")
    public ResponseEntity<String> reindex() {
        indexService.fullReindex();
        return ResponseEntity.ok("Full reindex started");
    }
}
