package com.example.ainews.domain.article;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.ainews.domain.article.dto.ArticleRequest;
import com.example.ainews.domain.article.dto.ArticleResponse;

@RestController
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping("/api/articles")
    public ResponseEntity<List<ArticleResponse>> findAll() {
        return ResponseEntity.ok(articleService.findAll());
    }

    @GetMapping("/api/articles/{id}")
    public ResponseEntity<ArticleResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(articleService.findById(id));
    }

    @PostMapping("/api/articles")
    public ResponseEntity<ArticleResponse> create(@RequestBody ArticleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(articleService.create(request));
    }

    @PutMapping("/api/articles/{id}")
    public ResponseEntity<ArticleResponse> update(@PathVariable Long id,
                                                  @RequestBody ArticleRequest request) {
        return ResponseEntity.ok(articleService.update(id, request));
    }

    @DeleteMapping("/api/articles/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        articleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/providers/{providerId}/articles")
    public ResponseEntity<List<ArticleResponse>> findByProviderId(@PathVariable Integer providerId) {
        return ResponseEntity.ok(articleService.findByProviderId(providerId));
    }
}
