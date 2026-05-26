package com.bibliotech.rag;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized RAG configuration bound from application.yml.
 * Pattern: @ConfigurationProperties — avoids @Value scatter.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "bibliotech.rag")
public class RagProperties {
    private boolean enabled = true;
    private int chunkSize = 500;
    private int chunkOverlap = 50;
    private int topK = 3;
    private double minScore = 0.60;
}