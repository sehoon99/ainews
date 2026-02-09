package com.example.ainews.domain.analysis;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.ainews.domain.analysis.dto.ImageAnalysisRequest;
import com.example.ainews.domain.analysis.dto.ImageAnalysisResponse;

@RestController
public class ImageAnalysisController {

    private final ImageAnalysisService imageAnalysisService;

    public ImageAnalysisController(ImageAnalysisService imageAnalysisService) {
        this.imageAnalysisService = imageAnalysisService;
    }

    @GetMapping("/api/articles/{articleId}/analyses")
    public ResponseEntity<List<ImageAnalysisResponse>> findByArticleId(@PathVariable Long articleId) {
        return ResponseEntity.ok(imageAnalysisService.findByArticleId(articleId));
    }

    @PostMapping("/api/articles/{articleId}/analyses")
    public ResponseEntity<ImageAnalysisResponse> create(@PathVariable Long articleId,
                                                        @RequestBody ImageAnalysisRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(imageAnalysisService.create(articleId, request));
    }

    @GetMapping("/api/analyses/{id}")
    public ResponseEntity<ImageAnalysisResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(imageAnalysisService.findById(id));
    }

    @DeleteMapping("/api/analyses/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        imageAnalysisService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
