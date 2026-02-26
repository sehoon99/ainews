package com.example.ainews.domain.keywordstat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class KeywordStatService {

    private static final Logger log = LoggerFactory.getLogger(KeywordStatService.class);

    private static final String KEYWORDS_SQL = """
            SELECT keyword, COUNT(*) as total_count,
              SUM(has_ai_image) as ai_count
            FROM (
              SELECT TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(a.keywords, ',', n.n), ',', -1)) as keyword,
                CASE WHEN EXISTS (
                  SELECT 1 FROM image_analyses ia
                  WHERE ia.article_id = a.id AND ia.ai_probability >= 0.7
                ) THEN 1 ELSE 0 END as has_ai_image
              FROM articles a
              CROSS JOIN (SELECT 1 as n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) n
              WHERE a.keywords IS NOT NULL
              AND n.n <= 1 + LENGTH(a.keywords) - LENGTH(REPLACE(a.keywords, ',', ''))
            ) t
            WHERE keyword != ''
            GROUP BY keyword
            ORDER BY total_count DESC
            LIMIT 50
            """;

    private final KeywordStatRepository keywordStatRepository;
    private final JdbcTemplate jdbcTemplate;

    public KeywordStatService(KeywordStatRepository keywordStatRepository, JdbcTemplate jdbcTemplate) {
        this.keywordStatRepository = keywordStatRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void saveDailyStats() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        List<KeywordStat> existing = keywordStatRepository.findByStatDateOrderByRankPositionAsc(today);
        if (!existing.isEmpty()) {
            log.info("오늘({}) 키워드 스냅샷이 이미 존재합니다. 건너뜁니다.", today);
            return;
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(KEYWORDS_SQL);

        List<KeywordStat> stats = new ArrayList<>();
        int rank = 1;
        for (Map<String, Object> row : rows) {
            String keyword = (String) row.get("keyword");
            int totalCount = ((Number) row.get("total_count")).intValue();
            int aiCount = ((Number) row.get("ai_count")).intValue();
            stats.add(new KeywordStat(keyword, totalCount, aiCount, rank, today));
            rank++;
        }

        keywordStatRepository.saveAll(stats);
        log.info("오늘({}) 키워드 스냅샷 {}개 저장 완료", today, stats.size());
    }

    @Transactional
    public void cleanOldStats() {
        LocalDate cutoff = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(30);
        keywordStatRepository.deleteByStatDateBefore(cutoff);
        log.info("{}일 이전 키워드 스냅샷 삭제 완료", cutoff);
    }

    public List<KeywordStat> getStatsByDate(LocalDate date) {
        return keywordStatRepository.findByStatDateOrderByRankPositionAsc(date);
    }

    public List<KeywordChange> calculateChanges() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDate yesterday = today.minusDays(1);

        List<KeywordStat> todayStats = getStatsByDate(today);
        List<KeywordStat> yesterdayStats = getStatsByDate(yesterday);

        Map<String, KeywordStat> yesterdayMap = yesterdayStats.stream()
                .collect(Collectors.toMap(KeywordStat::getKeyword, s -> s));

        Map<String, KeywordStat> todayMap = todayStats.stream()
                .collect(Collectors.toMap(KeywordStat::getKeyword, s -> s));

        List<KeywordChange> changes = new ArrayList<>();

        for (KeywordStat stat : todayStats) {
            KeywordStat prev = yesterdayMap.get(stat.getKeyword());
            if (prev == null) {
                changes.add(new KeywordChange(
                        stat.getKeyword(), KeywordChange.ChangeType.NEW_ENTRY,
                        stat.getRankPosition(), 0, stat.getTotalCount(), stat.getAiCount()));
            } else {
                int diff = prev.getRankPosition() - stat.getRankPosition();
                if (diff > 0) {
                    changes.add(new KeywordChange(
                            stat.getKeyword(), KeywordChange.ChangeType.RANK_UP,
                            stat.getRankPosition(), prev.getRankPosition(),
                            stat.getTotalCount(), stat.getAiCount()));
                } else if (diff < 0) {
                    changes.add(new KeywordChange(
                            stat.getKeyword(), KeywordChange.ChangeType.RANK_DOWN,
                            stat.getRankPosition(), prev.getRankPosition(),
                            stat.getTotalCount(), stat.getAiCount()));
                }
            }
        }

        for (KeywordStat prev : yesterdayStats) {
            if (!todayMap.containsKey(prev.getKeyword())) {
                changes.add(new KeywordChange(
                        prev.getKeyword(), KeywordChange.ChangeType.DROPPED,
                        0, prev.getRankPosition(), prev.getTotalCount(), prev.getAiCount()));
            }
        }

        return changes;
    }
}