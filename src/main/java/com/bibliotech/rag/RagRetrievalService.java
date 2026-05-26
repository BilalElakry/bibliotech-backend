package com.bibliotech.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagRetrievalService {

    private final VectorStore vectorStore;
    private final RagProperties ragProps;

    /**
     * Embeds the user query via nomic-embed-text (Ollama),
     * searches Qdrant for the top-k most similar chunks,
     * and returns a formatted context string for prompt injection.
     *
     * Returns empty string on failure or no results —
     * caller is responsible for fallback behavior.
     */
    public String retrieveContext(String userQuery) {
        if (!ragProps.isEnabled()) return "";

        try {
            // Spring AI 1.0.0-M6: SearchRequest uses builder pattern
            SearchRequest request = SearchRequest.builder()
                    .query(userQuery)
                    .topK(ragProps.getTopK())
                    .similarityThreshold(ragProps.getMinScore())
                    .build();

            List<Document> results = vectorStore.similaritySearch(request);

            if (results == null || results.isEmpty()) {
                log.info("RAG: no chunks above threshold {} for query '{}'",
                        ragProps.getMinScore(), truncate(userQuery, 60));
                return "";
            }

            log.info("RAG: {} chunks retrieved for query '{}'",
                    results.size(), truncate(userQuery, 60));

            return formatContext(results);

        } catch (Exception e) {
            log.error("RAG retrieval error: {}", e.getMessage());
            return ""; // graceful degradation
        }
    }

    /**
     * Formats retrieved chunks into a structured context block.
     * Injected into the LLM system prompt.
     */
    private String formatContext(List<Document> chunks) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Extraits pertinents des documents ===\n\n");

        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            String titre  = (String) chunk.getMetadata().getOrDefault("titre",  "Inconnu");
            String auteur = (String) chunk.getMetadata().getOrDefault("auteur", "Inconnu");
            String annee  = (String) chunk.getMetadata().getOrDefault("annee",  "");

            sb.append(String.format("[Extrait %d] \"%s\" — %s (%s)\n",
                    i + 1, titre, auteur, annee));

            // Spring AI 1.0.0-M6: getText() is the correct method
            sb.append(chunk.getText().trim());
            sb.append("\n\n");
        }

        sb.append("=========================================\n");
        return sb.toString();
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max
                ? s.substring(0, max) + "..." : s;
    }
}