package com.example.ainews.domain.analysis;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ainews.domain.analysis.dto.ImageAnalysisRequest;
import com.example.ainews.domain.analysis.dto.ImageAnalysisResponse;
import com.example.ainews.domain.article.Article;
import com.example.ainews.domain.article.ArticleRepository;
import com.example.ainews.global.exception.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class ImageAnalysisService {

    private final ImageAnalysisRepository imageAnalysisRepository;
    private final ArticleRepository articleRepository;

    public ImageAnalysisService(ImageAnalysisRepository imageAnalysisRepository,
                                ArticleRepository articleRepository) {
        this.imageAnalysisRepository = imageAnalysisRepository;
        this.articleRepository = articleRepository;
    }

    public List<ImageAnalysisResponse> findByArticleId(Long articleId) {
        return imageAnalysisRepository.findByArticleId(articleId).stream()
                .map(ImageAnalysisResponse::from)
                .toList();
    }

    public ImageAnalysisResponse findById(Long id) {
        ImageAnalysis analysis = imageAnalysisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ImageAnalysis not found: " + id));
        return ImageAnalysisResponse.from(analysis);
    }

    @Transactional
    public ImageAnalysisResponse create(Long articleId, ImageAnalysisRequest request) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found: " + articleId));

        ImageAnalysis analysis = new ImageAnalysis(article, request.imageUrl(), request.imageHash(),
                request.apiName(), request.aiProbability());
        return ImageAnalysisResponse.from(imageAnalysisRepository.save(analysis));
    }

    @Transactional
    public void delete(Long id) {
        if (!imageAnalysisRepository.existsById(id)) {
            throw new ResourceNotFoundException("ImageAnalysis not found: " + id);
        }
        imageAnalysisRepository.deleteById(id);
    }
}
