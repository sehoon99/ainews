package com.example.ainews.domain.keywordrank;

import com.example.ainews.domain.keywordrank.dto.KeywordRankResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class KeywordSearchRankService {

    private final KeywordSearchRankRepository repository;

    public KeywordSearchRankService(KeywordSearchRankRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void recordSearch(String keyword) {
        String normalized = keyword.trim();
        if (normalized.isEmpty()) {
            return;
        }
        LocalDate weekStart = getCurrentWeekStart();
        repository.upsertSearchCount(normalized, weekStart);
    }

    public List<KeywordRankResponse> getTopKeywords() {
        LocalDate weekStart = getCurrentWeekStart();
        return repository.findTop10ByWeekStartOrderBySearchCountDesc(weekStart)
                .stream()
                .map(KeywordRankResponse::from)
                .toList();
    }

    public List<KeywordSearchRank> getWeekData(LocalDate weekStart) {
        return repository.findByWeekStartOrderBySearchCountDesc(weekStart);
    }

    public LocalDate getCurrentWeekStart() {
        return LocalDate.now(ZoneId.of("Asia/Seoul"))
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}