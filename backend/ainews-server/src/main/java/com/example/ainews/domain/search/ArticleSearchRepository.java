package com.example.ainews.domain.search;

import java.util.ArrayList;
import java.util.List;

import com.example.ainews.config.OpenSearchProperties;
import com.example.ainews.domain.search.dto.ArticleSearchRequest;
import com.example.ainews.domain.search.dto.ArticleSearchResponse;
import com.example.ainews.domain.search.dto.ArticleSearchResponse.ArticleSearchHit;
import com.example.ainews.global.exception.SearchException;

import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Highlight;
import org.opensearch.client.opensearch.core.search.HighlightField;
import org.opensearch.client.opensearch.core.search.Hit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ArticleSearchRepository {

    private static final Logger log = LoggerFactory.getLogger(ArticleSearchRepository.class);

    private final OpenSearchClient client;
    private final OpenSearchProperties properties;

    public ArticleSearchRepository(OpenSearchClient client, OpenSearchProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss||yyyy-MM-dd'T'HH:mm:ss.SSS||epoch_millis";

    public void createIndexIfNotExists() {
        try {
            boolean exists = client.indices().exists(e -> e.index(properties.getIndexName())).value();
            if (!exists) {
                client.indices().create(c -> c
                        .index(properties.getIndexName())
                        .settings(s -> s
                                .analysis(a -> a
                                        .analyzer("nori_analyzer", an -> an
                                                .custom(ca -> ca
                                                        .tokenizer("nori_tokenizer")
                                                        .filter("nori_readingform", "lowercase")))))
                        .mappings(m -> m
                                .properties("id", p -> p.long_(l -> l))
                                .properties("title", p -> p.text(t -> t.analyzer("nori_analyzer")))
                                .properties("content", p -> p.text(t -> t.analyzer("nori_analyzer")))
                                .properties("sourceUrl", p -> p.keyword(k -> k))
                                .properties("publishedAt", p -> p.date(d -> d.format(DATE_FORMAT)))
                                .properties("crawledAt", p -> p.date(d -> d.format(DATE_FORMAT)))
                                .properties("portal", p -> p.keyword(k -> k))
                                .properties("providerName", p -> p.keyword(k -> k))
                                .properties("authorName", p -> p.keyword(k -> k))
                                .properties("hasAiImages", p -> p.boolean_(b -> b))
                                .properties("maxAiProbability", p -> p.float_(f -> f))
                                .properties("imageCount", p -> p.integer(i -> i))));
                log.info("Created OpenSearch index: {}", properties.getIndexName());
            }
        } catch (Exception e) {
            throw new SearchException("Failed to create index: " + e.getMessage(), e);
        }
    }

    public void index(ArticleDocument document) {
        try {
            client.index(i -> i
                    .index(properties.getIndexName())
                    .id(String.valueOf(document.getId()))
                    .document(document));
        } catch (Exception e) {
            throw new SearchException("Failed to index document: " + document.getId(), e);
        }
    }

    public void bulkIndex(List<ArticleDocument> documents) {
        if (documents.isEmpty()) {
            return;
        }

        try {
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            for (ArticleDocument doc : documents) {
                bulkBuilder.operations(op -> op
                        .index(idx -> idx
                                .index(properties.getIndexName())
                                .id(String.valueOf(doc.getId()))
                                .document(doc)));
            }

            BulkResponse response = client.bulk(bulkBuilder.build());
            if (response.errors()) {
                log.warn("Bulk indexing had errors. First error: {}",
                        response.items().stream()
                                .filter(item -> item.error() != null)
                                .findFirst()
                                .map(item -> item.error().reason())
                                .orElse("unknown"));
            }
            log.info("Bulk indexed {} documents", documents.size());
        } catch (Exception e) {
            throw new SearchException("Failed to bulk index documents", e);
        }
    }

    public void delete(Long articleId) {
        try {
            client.delete(d -> d
                    .index(properties.getIndexName())
                    .id(String.valueOf(articleId)));
        } catch (Exception e) {
            throw new SearchException("Failed to delete document: " + articleId, e);
        }
    }

    public void deleteIndex() {
        try {
            boolean exists = client.indices().exists(e -> e.index(properties.getIndexName())).value();
            if (exists) {
                client.indices().delete(d -> d.index(properties.getIndexName()));
                log.info("Deleted OpenSearch index: {}", properties.getIndexName());
            }
        } catch (Exception e) {
            throw new SearchException("Failed to delete index", e);
        }
    }

    public ArticleSearchResponse search(ArticleSearchRequest request) {
        try {
            BoolQuery.Builder boolQuery = new BoolQuery.Builder();

            // Full-text search on title and content
            if (request.query() != null && !request.query().isBlank()) {
                boolQuery.must(Query.of(q -> q
                        .multiMatch(mm -> mm
                                .query(request.query())
                                .fields("title^3", "content")
                                .analyzer("nori_analyzer"))));
            }

            // Portal filter
            if (request.portal() != null && !request.portal().isBlank()) {
                boolQuery.filter(Query.of(q -> q
                        .term(t -> t
                                .field("portal")
                                .value(FieldValue.of(request.portal())))));
            }

            // Provider filter
            if (request.providerName() != null && !request.providerName().isBlank()) {
                boolQuery.filter(Query.of(q -> q
                        .term(t -> t
                                .field("providerName")
                                .value(FieldValue.of(request.providerName())))));
            }

            // Date range filter
            if (request.publishedFrom() != null || request.publishedTo() != null) {
                boolQuery.filter(Query.of(q -> q
                        .range(r -> {
                            r.field("publishedAt");
                            if (request.publishedFrom() != null) {
                                r.gte(JsonData.of(request.publishedFrom().toString()));
                            }
                            if (request.publishedTo() != null) {
                                r.lte(JsonData.of(request.publishedTo().toString()));
                            }
                            return r;
                        })));
            }

            // AI probability filter
            if (request.minAiProbability() != null) {
                boolQuery.filter(Query.of(q -> q
                        .range(r -> r
                                .field("maxAiProbability")
                                .gte(JsonData.of(request.minAiProbability())))));
            }

            // Has AI images filter
            if (request.hasAiImages() != null) {
                boolQuery.filter(Query.of(q -> q
                        .term(t -> t
                                .field("hasAiImages")
                                .value(FieldValue.of(request.hasAiImages())))));
            }

            // Highlight configuration
            Highlight highlight = Highlight.of(h -> h
                    .fields("title", HighlightField.of(hf -> hf.numberOfFragments(1).fragmentSize(200)))
                    .fields("content", HighlightField.of(hf -> hf.numberOfFragments(1).fragmentSize(200))));

            int from = request.page() * request.size();

            SearchResponse<ArticleDocument> response = client.search(s -> s
                    .index(properties.getIndexName())
                    .query(Query.of(q -> q.bool(boolQuery.build())))
                    .highlight(highlight)
                    .from(from)
                    .size(request.size()), ArticleDocument.class);

            long totalHits = response.hits().total() != null ? response.hits().total().value() : 0;
            int totalPages = (int) Math.ceil((double) totalHits / request.size());

            List<ArticleSearchHit> hits = new ArrayList<>();
            for (Hit<ArticleDocument> hit : response.hits().hits()) {
                ArticleDocument doc = hit.source();
                if (doc == null) continue;

                String titleHighlight = getHighlight(hit, "title", doc.getTitle());
                String contentSnippet = getHighlight(hit, "content",
                        doc.getContent() != null && doc.getContent().length() > 200
                                ? doc.getContent().substring(0, 200)
                                : doc.getContent());

                hits.add(new ArticleSearchHit(
                        doc.getId(),
                        titleHighlight,
                        contentSnippet,
                        doc.getSourceUrl(),
                        doc.getPublishedAt(),
                        doc.getPortal(),
                        doc.getProviderName(),
                        doc.getAuthorName(),
                        doc.isHasAiImages(),
                        doc.getMaxAiProbability(),
                        doc.getImageCount(),
                        hit.score() != null ? hit.score().floatValue() : 0f
                ));
            }

            return new ArticleSearchResponse(hits, totalHits, request.page(), request.size(), totalPages);
        } catch (Exception e) {
            throw new SearchException("Failed to search articles: " + e.getMessage(), e);
        }
    }

    private String getHighlight(Hit<ArticleDocument> hit, String field, String fallback) {
        if (hit.highlight() != null && hit.highlight().containsKey(field)) {
            List<String> fragments = hit.highlight().get(field);
            if (fragments != null && !fragments.isEmpty()) {
                return fragments.get(0);
            }
        }
        return fallback;
    }
}
