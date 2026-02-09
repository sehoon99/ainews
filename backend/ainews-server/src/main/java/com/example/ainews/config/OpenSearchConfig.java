package com.example.ainews.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenSearchConfig {

    private final OpenSearchProperties properties;

    public OpenSearchConfig(OpenSearchProperties properties) {
        this.properties = properties;
    }

    @Bean
    public OpenSearchClient openSearchClient() throws Exception {
        HttpHost host = new HttpHost(properties.getScheme(), properties.getHost(), properties.getPort());

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        ApacheHttpClient5TransportBuilder builder = ApacheHttpClient5TransportBuilder
                .builder(host)
                .setMapper(new JacksonJsonpMapper(objectMapper));

        if ("https".equals(properties.getScheme())) {
            builder.setHttpClientConfigCallback(httpClientBuilder -> {
                try {
                    var tlsStrategy = ClientTlsStrategyBuilder.create()
                            .setSslContext(SSLContextBuilder.create()
                                    .loadTrustMaterial(null, (chains, authType) -> true)
                                    .build())
                            .build();
                    var connectionManager = PoolingAsyncClientConnectionManagerBuilder.create()
                            .setTlsStrategy(tlsStrategy)
                            .build();
                    httpClientBuilder.setConnectionManager(connectionManager);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to configure TLS", e);
                }

                if (!properties.isUseAwsSigning()) {
                    var credentialsProvider = new BasicCredentialsProvider();
                    credentialsProvider.setCredentials(
                            new AuthScope(host),
                            new UsernamePasswordCredentials(
                                    properties.getUsername(),
                                    properties.getPassword().toCharArray()));
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                }

                return httpClientBuilder;
            });
        } else if (!properties.isUseAwsSigning()
                && properties.getUsername() != null
                && !properties.getUsername().isEmpty()) {
            builder.setHttpClientConfigCallback(httpClientBuilder -> {
                var credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                        new AuthScope(host),
                        new UsernamePasswordCredentials(
                                properties.getUsername(),
                                properties.getPassword().toCharArray()));
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                return httpClientBuilder;
            });
        }

        return new OpenSearchClient(builder.build());
    }
}
