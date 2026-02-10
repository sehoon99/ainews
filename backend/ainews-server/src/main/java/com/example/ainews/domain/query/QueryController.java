package com.example.ainews.domain.query;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QueryController {

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
            if (isSelectQuery(sql)) {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
                List<String> columns = rows.isEmpty()
                        ? List.of()
                        : new ArrayList<>(rows.get(0).keySet());

                result.put("columns", columns);
                result.put("rows", rows);
                result.put("rowCount", rows.size());
            } else {
                int affected = jdbcTemplate.update(sql);
                result.put("columns", List.of());
                result.put("rows", List.of());
                result.put("rowCount", affected);
                result.put("message", affected + " row(s) affected");
            }

            result.put("executionTime", System.currentTimeMillis() - start);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("executionTime", System.currentTimeMillis() - start);
            return ResponseEntity.badRequest().body(result);
        }
    }

    private boolean isSelectQuery(String sql) {
        String upper = sql.toUpperCase();
        return upper.startsWith("SELECT") || upper.startsWith("SHOW") || upper.startsWith("DESCRIBE") || upper.startsWith("EXPLAIN");
    }
}