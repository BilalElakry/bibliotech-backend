package com.bibliotech.rag;

import com.bibliotech.catalogue.document.DocumentRepository;
import com.bibliotech.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RagAdminController {

    private final RagIngestionService ingestionService;
    private final DocumentRepository documentRepo;

    /**
     * POST /api/rag/ingest/{id}
     * Manually trigger RAG ingestion for one document.
     */
    @PostMapping("/ingest/{id}")
    public ResponseEntity<Map<String, Object>> ingestOne(@PathVariable Long id) {
        documentRepo.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Document introuvable: " + id));

        ingestionService.ingest(id); // @Async

        return ResponseEntity.accepted().body(Map.of(
                "message",    "Ingestion RAG démarrée pour le document " + id,
                "documentId", id,
                "status",     "PROCESSING"
        ));
    }

    /**
     * POST /api/rag/ingest/all
     * Re-ingest every document that has a PDF.
     * Use once after enabling RAG on an existing database.
     */
    @PostMapping("/ingest/all")
    public ResponseEntity<Map<String, Object>> ingestAll() {
        var docs = documentRepo.findAll();
        long count = docs.stream()
                .filter(d -> d.getFichierPdf() != null)
                .peek(d -> ingestionService.ingest(d.getId()))
                .count();

        return ResponseEntity.accepted().body(Map.of(
                "message",        "Ingestion RAG démarrée pour " + count + " documents",
                "totalDocuments", count,
                "status",         "PROCESSING"
        ));
    }
}