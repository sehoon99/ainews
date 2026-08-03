package com.example.ainews.infra.email;

import com.example.ainews.domain.keywordstat.KeywordChange;
import com.example.ainews.domain.keywordstat.KeywordStatService;
import com.example.ainews.domain.subscriber.Subscriber;
import com.example.ainews.domain.subscriber.SubscriberService;
import com.example.ainews.infra.scheduler.ClusterSchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class DailyMailScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyMailScheduler.class);

    private final KeywordStatService keywordStatService;
    private final SubscriberService subscriberService;
    private final EmailService emailService;
    private final EmailTemplateBuilder templateBuilder;
    private final ClusterSchedulerLock schedulerLock;
    private final String baseUrl;

    public DailyMailScheduler(KeywordStatService keywordStatService,
                              SubscriberService subscriberService,
                              EmailService emailService,
                              EmailTemplateBuilder templateBuilder,
                              ClusterSchedulerLock schedulerLock,
                              @Value("${ainews.ses.base-url}") String baseUrl) {
        this.keywordStatService = keywordStatService;
        this.subscriberService = subscriberService;
        this.emailService = emailService;
        this.templateBuilder = templateBuilder;
        this.schedulerLock = schedulerLock;
        this.baseUrl = baseUrl;
    }

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void sendDailyReport() {
        schedulerLock.runWithLock("ainews-daily-mail", this::sendDailyReportOnce);
    }

    private void sendDailyReportOnce() {
        log.info("일일 키워드 변동 리포트 발송 시작");

        List<KeywordChange> changes = keywordStatService.calculateChanges();
        if (changes.isEmpty()) {
            log.info("키워드 변동이 없어 메일 발송을 건너뜁니다.");
            return;
        }

        List<Subscriber> subscribers = subscriberService.getActiveSubscribers();
        if (subscribers.isEmpty()) {
            log.info("활성 구독자가 없어 메일 발송을 건너뜁니다.");
            return;
        }

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String subject = "[AI News Tracker] " + today + " 키워드 변동 리포트";

        int sent = 0;
        for (Subscriber subscriber : subscribers) {
            String unsubscribeUrl = baseUrl + "/api/subscribers/unsubscribe?token=" + subscriber.getToken();
            String html = templateBuilder.buildDailyReport(changes, unsubscribeUrl);
            emailService.sendHtmlEmail(subscriber.getEmail(), subject, html);
            sent++;
        }

        log.info("일일 키워드 변동 리포트 발송 완료: {}명에게 발송", sent);
    }
}
