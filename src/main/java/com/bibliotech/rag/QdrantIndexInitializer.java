package com.bibliotech.rag;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.PayloadSchemaType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QdrantIndexInitializer {

    private final QdrantClient qdrantClient;

    @Value("${spring.ai.vectorstore.qdrant.collection-name}")
    private String collectionName;

    /**
     * Creates a keyword payload index on the "documentId" field at startup.
     *
     * Why this is needed:
     * Qdrant requires an explicit payload index before a field can be used
     * in filter expressions (e.g. "documentId == '5'").
     * Without it, deleteByDocumentId() throws INVALID_ARGUMENT.
     *
     * This method is idempotent — if the index already exists,
     * Qdrant silently ignores the request. Safe to call on every startup.
     */
    @PostConstruct
    public void createPayloadIndexes() {
        createKeywordIndex("documentId");
    }

    private void createKeywordIndex(String fieldName) {
        try {
            qdrantClient.createPayloadIndexAsync(
                    collectionName,
                    fieldName,
                    PayloadSchemaType.Keyword,
                    null,   // no extra index params needed
                    true,   // wait = true: block until index is confirmed
                    null,   // no write ordering override
                    null    // no timeout override
            ).get();        // .get() makes it synchronous — index ready before app serves requests

            log.info("Qdrant: keyword index on '{}' is ready (collection: {})",
                    fieldName, collectionName);

        } catch (Exception e) {
            // Index already exists → Qdrant returns an error we can safely ignore
            // Any other error is logged but must not prevent app startup
            log.warn("Qdrant index init for '{}': {} — this is normal if index already exists",
                    fieldName, e.getMessage());
        }
    }
}