package com.example.ainews.domain.article;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ainews.domain.article.dto.ArticleRequest;
import com.example.ainews.domain.article.dto.ArticleResponse;
import com.example.ainews.domain.author.Author;
import com.example.ainews.domain.author.AuthorRepository;
import com.example.ainews.domain.provider.Provider;
import com.example.ainews.domain.provider.ProviderRepository;
import com.example.ainews.global.exception.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final ProviderRepository providerRepository;
    private final AuthorRepository authorRepository;

    public ArticleService(ArticleRepository articleRepository,
                          ProviderRepository providerRepository,
                          AuthorRepository authorRepository) {
        this.articleRepository = articleRepository;
        this.providerRepository = providerRepository;
        this.authorRepository = authorRepository;
    }

    public List<ArticleResponse> findAll() {
        return articleRepository.findAll().stream()
                .map(ArticleResponse::from)
                .toList();
    }

    public List<ArticleResponse> findByProviderId(Integer providerId) {
        return articleRepository.findByProviderId(providerId).stream()
                .map(ArticleResponse::from)
                .toList();
    }

    public ArticleResponse findById(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found: " + id));
        return ArticleResponse.from(article);
    }

    @Transactional
    public ArticleResponse create(ArticleRequest request) {
        Provider provider = providerRepository.findById(request.providerId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found: " + request.providerId()));

        Author author = null;
        if (request.authorId() != null) {
            author = authorRepository.findById(request.authorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Author not found: " + request.authorId()));
        }

        Article article = new Article(provider, author, request.title(), request.content(),
                request.sourceUrl(), request.publishedAt());
        return ArticleResponse.from(articleRepository.save(article));
    }

    @Transactional
    public ArticleResponse update(Long id, ArticleRequest request) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found: " + id));

        Provider provider = providerRepository.findById(request.providerId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found: " + request.providerId()));

        Author author = null;
        if (request.authorId() != null) {
            author = authorRepository.findById(request.authorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Author not found: " + request.authorId()));
        }

        article.setProvider(provider);
        article.setAuthor(author);
        article.setTitle(request.title());
        article.setContent(request.content());
        article.setSourceUrl(request.sourceUrl());
        article.setPublishedAt(request.publishedAt());
        return ArticleResponse.from(articleRepository.save(article));
    }

    @Transactional
    public void delete(Long id) {
        if (!articleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Article not found: " + id);
        }
        articleRepository.deleteById(id);
    }
}
