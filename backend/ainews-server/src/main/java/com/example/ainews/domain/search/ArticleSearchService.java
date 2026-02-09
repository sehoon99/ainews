package com.example.ainews.domain.search;

import com.example.ainews.domain.search.dto.ArticleSearchRequest;
import com.example.ainews.domain.search.dto.ArticleSearchResponse;

import org.springframework.stereotype.Service;

@Service
public class ArticleSearchService {

    private final ArticleSearchRepository searchRepository;

    public ArticleSearchService(ArticleSearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    public ArticleSearchResponse search(ArticleSearchRequest request) {
        return searchRepository.search(request);
    }
}
