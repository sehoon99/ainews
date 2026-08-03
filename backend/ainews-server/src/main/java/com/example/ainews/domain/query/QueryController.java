package com.example.ainews.domain.query;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QueryController {

    private static final String LAST_ARTICLE_SQL =
            "SELECT CONVERT_TZ(created_at, '+00:00', '+09:00') AS created_at "
                    + "FROM articles ORDER BY created_at DESC LIMIT 1";
    private static final String TODAY_AI_COUNT_SQL =
            "SELECT COUNT(*) as cnt FROM image_analyses WHERE ai_probability >= 0.7 "
                    + "AND DATE(CONVERT_TZ(created_at, '+00:00', '+09:00')) = CURDATE()";
    private static final String KEYWORD_SUMMARY_SQL = """
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
    private static final String PORTAL_SEARCH_SQL =
            "SELECT portal, COUNT(*) as cnt FROM articles WHERE keywords LIKE ? GROUP BY portal";
    private static final Pattern PORTAL_SEARCH_REQUEST = Pattern.compile(
            "^SELECT portal, COUNT\\(\\*\\) as cnt FROM articles WHERE keywords LIKE '%(.{1,100})%' GROUP BY portal$",
            Pattern.CASE_INSENSITIVE);

    private final JdbcTemplate jdbcTemplate;

    public QueryController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/api/query")
    public ResponseEntity<Map<String, Object>> executeQuery(@RequestBody QueryRequest request) {
        String sql = request.sql().trim();
        long start = System.currentTimeMillis();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sql", sql);

        try {
            List<Map<String, Object>> rows = executeAllowedQuery(sql);
            if (rows == null) {
                result.put("error", "Only dashboard aggregate queries are allowed");
                result.put("executionTime", System.currentTimeMillis() - start);
                return ResponseEntity.badRequest().body(result);
            }

            List<String> columns = rows.isEmpty()
                    ? List.of()
                    : new ArrayList<>(rows.get(0).keySet());
            result.put("columns", columns);
            result.put("rows", rows);
            result.put("rowCount", rows.size());

            result.put("executionTime", System.currentTimeMillis() - start);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("executionTime", System.currentTimeMillis() - start);
            return ResponseEntity.badRequest().body(result);
        }
    }

    private List<Map<String, Object>> executeAllowedQuery(String sql) {
        String normalized = sql.replaceAll("\\s+", " ").trim();
        if (normalized.equals(normalize(LAST_ARTICLE_SQL))) {
            return jdbcTemplate.queryForList(LAST_ARTICLE_SQL);
        }
        if (normalized.equals(normalize(TODAY_AI_COUNT_SQL))) {
            return jdbcTemplate.queryForList(TODAY_AI_COUNT_SQL);
        }
        if (normalized.equals(normalize(KEYWORD_SUMMARY_SQL))) {
            return jdbcTemplate.queryForList(KEYWORD_SUMMARY_SQL);
        }

        Matcher portalSearch = PORTAL_SEARCH_REQUEST.matcher(normalized);
        if (portalSearch.matches()) {
            return jdbcTemplate.queryForList(PORTAL_SEARCH_SQL, "%" + portalSearch.group(1) + "%");
        }
        return null;
    }

    private String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
