package com.example.ainews.domain.keywordstat;

import com.example.ainews.infra.scheduler.ClusterSchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class KeywordStatScheduler {

    private static final Logger log = LoggerFactory.getLogger(KeywordStatScheduler.class);

    private final KeywordStatService keywordStatService;
    private final ClusterSchedulerLock schedulerLock;

    public KeywordStatScheduler(KeywordStatService keywordStatService, ClusterSchedulerLock schedulerLock) {
        this.keywordStatService = keywordStatService;
        this.schedulerLock = schedulerLock;
    }

    @Scheduled(cron = "0 50 8 * * *", zone = "Asia/Seoul")
    public void saveDailySnapshot() {
        schedulerLock.runWithLock("ainews-keyword-stat", this::saveDailySnapshotOnce);
    }

    private void saveDailySnapshotOnce() {
        log.info("일일 키워드 스냅샷 저장 시작");
        try {
            keywordStatService.saveDailyStats();
            keywordStatService.cleanOldStats();
        } catch (Exception e) {
            log.error("일일 키워드 스냅샷 저장 실패", e);
        }
    }
}
