package com.example.ainews.infra.email;

import com.example.ainews.domain.keywordstat.KeywordChange;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class EmailTemplateBuilder {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");

    public String buildDailyReport(List<KeywordChange> changes, String unsubscribeUrl) {
        List<KeywordChange> newEntries = changes.stream()
                .filter(c -> c.changeType() == KeywordChange.ChangeType.NEW_ENTRY).toList();
        List<KeywordChange> rankUps = changes.stream()
                .filter(c -> c.changeType() == KeywordChange.ChangeType.RANK_UP).toList();
        List<KeywordChange> rankDowns = changes.stream()
                .filter(c -> c.changeType() == KeywordChange.ChangeType.RANK_DOWN).toList();
        List<KeywordChange> dropped = changes.stream()
                .filter(c -> c.changeType() == KeywordChange.ChangeType.DROPPED).toList();

        String today = LocalDate.now().format(DATE_FMT);

        StringBuilder sb = new StringBuilder();
        sb.append("""
                <!DOCTYPE html>
                <html lang="ko">
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;background-color:#0c1222;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;">
                <div style="max-width:600px;margin:0 auto;padding:32px 24px;">
                """);

        // Header
        sb.append("""
                <div style="text-align:center;padding:24px 0;border-bottom:1px solid #1e2d4a;">
                  <h1 style="margin:0;font-size:22px;color:#f4f4f5;">AI News Tracker</h1>
                  <p style="margin:8px 0 0;font-size:14px;color:#a1a1aa;">%s 키워드 변동 리포트</p>
                </div>
                """.formatted(today));

        // New entries
        if (!newEntries.isEmpty()) {
            sb.append(sectionHeader("신규 진입", "#22c55e", newEntries.size()));
            for (KeywordChange c : newEntries) {
                sb.append(keywordRow(c.keyword(),
                        "NEW #" + c.currentRank(),
                        "#22c55e", c.totalCount()));
            }
            sb.append("</div>");
        }

        // Rank up
        if (!rankUps.isEmpty()) {
            sb.append(sectionHeader("순위 상승", "#3b82f6", rankUps.size()));
            for (KeywordChange c : rankUps) {
                sb.append(keywordRow(c.keyword(),
                        "#" + c.currentRank() + " (+" + c.rankDiff() + ")",
                        "#3b82f6", c.totalCount()));
            }
            sb.append("</div>");
        }

        // Rank down
        if (!rankDowns.isEmpty()) {
            sb.append(sectionHeader("순위 하락", "#f59e0b", rankDowns.size()));
            for (KeywordChange c : rankDowns) {
                sb.append(keywordRow(c.keyword(),
                        "#" + c.currentRank() + " (" + c.rankDiff() + ")",
                        "#f59e0b", c.totalCount()));
            }
            sb.append("</div>");
        }

        // Dropped
        if (!dropped.isEmpty()) {
            sb.append(sectionHeader("퇴출", "#ef4444", dropped.size()));
            for (KeywordChange c : dropped) {
                sb.append(keywordRow(c.keyword(),
                        "이전 #" + c.previousRank(),
                        "#ef4444", c.totalCount()));
            }
            sb.append("</div>");
        }

        // Footer
        sb.append("""
                <div style="text-align:center;padding:24px 0;margin-top:24px;border-top:1px solid #1e2d4a;">
                  <p style="margin:0;font-size:12px;color:#64748b;">
                    이 메일은 AI News Tracker 구독 서비스에서 발송되었습니다.<br>
                    <a href="%s" style="color:#64748b;text-decoration:underline;">구독 해제</a>
                  </p>
                </div>
                </div>
                </body>
                </html>
                """.formatted(unsubscribeUrl));

        return sb.toString();
    }

    private String sectionHeader(String title, String color, int count) {
        return """
                <div style="margin-top:24px;padding:16px 20px;background-color:#111827;border:1px solid #1e2d4a;border-radius:12px;">
                  <div style="display:flex;align-items:center;margin-bottom:12px;padding-bottom:10px;border-bottom:1px solid #1e2d4a;">
                    <span style="display:inline-block;width:8px;height:8px;border-radius:50%%;background-color:%s;margin-right:8px;"></span>
                    <span style="font-size:15px;font-weight:700;color:#f4f4f5;">%s</span>
                    <span style="margin-left:auto;font-size:13px;color:#a1a1aa;">%d개</span>
                  </div>
                """.formatted(color, title, count);
    }

    private String keywordRow(String keyword, String badge, String badgeColor, int totalCount) {
        return """
                  <div style="display:flex;align-items:center;padding:6px 0;">
                    <span style="font-size:14px;color:#e4e4e7;flex:1;">%s</span>
                    <span style="font-size:11px;font-weight:600;padding:2px 8px;border-radius:4px;background-color:%s22;color:%s;margin-right:8px;">%s</span>
                    <span style="font-size:12px;color:#a1a1aa;">%d건</span>
                  </div>
                """.formatted(keyword, badgeColor, badgeColor, badge, totalCount);
    }
}