package com.example.ainews.domain.keywordstat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class KeywordStatScheduler {

    private static final Logger log = LoggerFactory.getLogger(KeywordStatScheduler.class);

    private final KeywordStatService keywordStatService;

    public KeywordStatScheduler(KeywordStatService keywordStatService) {
        this.keywordStatService = keywordStatService;
    }

    @Scheduled(cron = "0 50 8 * * *", zone = "Asia/Seoul")
    public void saveDailySnapshot() {
        log.info("일일 키워드 스냅샷 저장 시작");
        try {
            keywordStatService.saveDailyStats();
            keywordStatService.cleanOldStats();
        } catch (Exception e) {
            log.error("일일 키워드 스냅샷 저장 실패", e);
        }
    }
}