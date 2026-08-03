package com.example.ainews.domain.keywordrank;

import com.example.ainews.domain.keywordrank.dto.KeywordRankResponse;
import com.example.ainews.domain.keywordrank.dto.KeywordSearchRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/keyword-ranks")
public class KeywordSearchRankController {

    private final KeywordSearchRankService service;

    public KeywordSearchRankController(KeywordSearchRankService service) {
        this.service = service;
    }

    @PostMapping("/search")
    public ResponseEntity<Void> recordSearch(@RequestBody KeywordSearchRequest request) {
        service.recordSearch(request.keyword());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/top")
    public ResponseEntity<List<KeywordRankResponse>> getTopKeywords() {
        return ResponseEntity.ok(service.getTopKeywords());
    }
}