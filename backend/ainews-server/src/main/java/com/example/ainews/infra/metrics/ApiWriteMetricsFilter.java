package com.example.ainews.infra.metrics;

import java.io.IOException;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiWriteMetricsFilter extends OncePerRequestFilter {

    private static final Set<String> WRITE_ENDPOINTS = Set.of(
            "POST /api/subscribers",
            "GET /api/subscribers/unsubscribe",
            "POST /api/keyword-ranks/search",
            "POST /api/articles",
            "PUT /api/articles/{id}",
            "DELETE /api/articles/{id}",
            "POST /api/articles/{articleId}/analyses",
            "DELETE /api/analyses/{id}",
            "POST /api/providers",
            "PUT /api/providers/{id}",
            "DELETE /api/providers/{id}",
            "POST /api/providers/{providerId}/authors",
            "PUT /api/authors/{id}",
            "DELETE /api/authors/{id}");

    private final MeterRegistry meterRegistry;

    public ApiWriteMetricsFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String outcome = "exception";
        try {
            filterChain.doFilter(request, response);
            outcome = outcome(response.getStatus());
        } catch (IOException | ServletException | RuntimeException exception) {
            throw exception;
        } finally {
            String route = matchedRoute(request);
            if (WRITE_ENDPOINTS.contains(request.getMethod() + " " + route)) {
                Counter.builder("ainews.db.write.requests")
                        .description("Database write-intent API requests")
                        .tag("route", route)
                        .tag("method", request.getMethod())
                        .tag("outcome", outcome)
                        .register(meterRegistry)
                        .increment();
            }
        }
    }

    private String matchedRoute(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern instanceof String route ? route : "unmatched";
    }

    private String outcome(int status) {
        if (status >= 500) {
            return "server_error";
        }
        if (status >= 400) {
            return "client_error";
        }
        return "success";
    }
}
