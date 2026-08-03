package com.example.ainews.domain.keywordrank;

import com.example.ainews.domain.keywordrank.dto.KeywordRankResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Component
public class KeywordRankArchiveScheduler {

    private static final Logger log = LoggerFactory.getLogger(KeywordRankArchiveScheduler.class);

    private final KeywordSearchRankService service;
    private final String archivePath;
    private final ObjectMapper objectMapper;

    public KeywordRankArchiveScheduler(
            KeywordSearchRankService service,
            @Value("${ainews.keyword-rank.archive-path:./keyword-rank-archives}") String archivePath) {
        this.service = service;
        this.archivePath = archivePath;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Scheduled(cron = "0 5 0 * * MON", zone = "Asia/Seoul")
    public void archivePreviousWeek() {
        LocalDate previousWeekStart = LocalDate.now(ZoneId.of("Asia/Seoul"))
                .with(TemporalAdjusters.previous(DayOfWeek.MONDAY));

        List<KeywordRankResponse> data = service.getWeekData(previousWeekStart)
                .stream()
                .map(KeywordRankResponse::from)
                .toList();

        if (data.isEmpty()) {
            log.info("No keyword rank data for week starting {}", previousWeekStart);
            return;
        }

        try {
            File dir = new File(archivePath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String filename = String.format("keyword-ranks-%s.json", previousWeekStart);
            File file = new File(dir, filename);
            objectMapper.writeValue(file, data);
            log.info("Archived {} keyword ranks for week {} to {}", data.size(), previousWeekStart, file.getAbsolutePath());
        } catch (Exception e) {
            log.error("Failed to archive keyword ranks for week {}", previousWeekStart, e);
        }
    }
}