package com.bibliotech.rag;

import com.bibliotech.catalogue.document.Document;
import com.bibliotech.catalogue.document.DocumentRepository;
import com.bibliotech.exception.ResourceNotFoundException;
import com.bibliotech.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagIngestionService {

    private final VectorStore vectorStore;
    private final StorageService storageService;
    private final DocumentRepository documentRepo;
    private final RagProperties ragProps;

    /**
     * Entry point — called @Async from DocumentService after PDF upload.
     * Non-blocking: a failure here never breaks the upload response.
     *
     * Pipeline:
     *   1. Load document entity
     *   2. Extract text from PDF (PDFBox)
     *   3. Delete old vectors (idempotent re-ingestion)
     *   4. Split text into chunks (TokenTextSplitter)
     *   5. Store in Qdrant — Spring AI calls nomic-embed-text automatically
     */
    @Async
    @Transactional(readOnly = true)
    public void ingest(Long documentId) {
        if (!ragProps.isEnabled()) {
            log.debug("RAG disabled — skipping ingestion for doc {}", documentId);
            return;
        }

        Document doc = documentRepo.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document introuvable : " + documentId));

        if (doc.getFichierPdf() == null) {
            log.warn("RAG ingestion skipped — no PDF attached to doc {}", documentId);
            return;
        }

        try {
            log.info("RAG ingestion started — doc [{}] '{}'",
                    documentId, doc.getTitre());

            // Step 1: Extract full text (no page limit — full document for RAG)
            String fullText = extractFullText(doc.getFichierPdf());

            if (fullText.isBlank()) {
                log.warn("RAG: blank text extracted from doc {} — possibly scanned PDF",
                        documentId);
                return;
            }

            // Step 2: Remove stale vectors for this document
            deleteByDocumentId(documentId);

            // Step 3: Split and store
            List<org.springframework.ai.document.Document> chunks =
                    buildChunks(fullText, doc);

            vectorStore.add(chunks);

            log.info("RAG ingestion complete — doc [{}] → {} chunks stored in Qdrant",
                    documentId, chunks.size());

        } catch (Exception e) {
            // Log but do NOT rethrow — upload must succeed even if RAG fails
            log.error("RAG ingestion failed for doc {}: {}",
                    documentId, e.getMessage(), e);
        }
    }

    /**
     * Deletes all Qdrant vectors whose metadata documentId matches.
     * Uses filter expression syntax: documentId == 'value'
     * Called before re-ingestion and on document deletion.
     */
    public void deleteByDocumentId(Long documentId) {
        try {
            // Spring AI 1.0.0-M6 VectorStore.delete(String filterExpression)
            // Filter expression syntax: key == 'value'
            vectorStore.delete("documentId == '" + documentId + "'");
            log.info("RAG: vectors deleted for doc {}", documentId);
        } catch (Exception e) {
            // Non-fatal — stale vectors will simply score below threshold
            log.warn("RAG: could not delete vectors for doc {}: {}",
                    documentId, e.getMessage());
        }
    }

    // ── Private helpers ───────────────────────────────────────────

    private String extractFullText(String pdfPath) throws IOException {
        byte[] bytes = storageService.load(pdfPath);
        try (PDDocument pdDoc = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            // Extract all pages — no limit for RAG (unlike TtsService which caps at 10)
            return stripper.getText(pdDoc)
                    .replaceAll("\\s+", " ")
                    .trim();
        }
    }

    private List<org.springframework.ai.document.Document> buildChunks(
            String fullText, Document doc) {

        // TokenTextSplitter uses GPT-2 tokenizer — language-agnostic,
        // consistent chunk sizes regardless of French/English content
        TokenTextSplitter splitter = new TokenTextSplitter(
                ragProps.getChunkSize(),    // target tokens per chunk
                ragProps.getChunkOverlap(), // overlap tokens between chunks
                5,                          // min tokens to keep a chunk
                10_000,                     // max input tokens per call
                true                        // keep sentence separators
        );

        org.springframework.ai.document.Document sourceDoc =
                new org.springframework.ai.document.Document(
                        fullText,
                        buildMetadata(doc)
                );

        List<org.springframework.ai.document.Document> chunks =
                splitter.apply(List.of(sourceDoc));

        // Enrich each chunk with its position for debugging
        for (int i = 0; i < chunks.size(); i++) {
            chunks.get(i).getMetadata().put("chunkIndex",  i);
            chunks.get(i).getMetadata().put("totalChunks", chunks.size());
        }

        return chunks;
    }

    /**
     * Metadata stored alongside each vector in Qdrant.
     * Used for: filter-based deletion, context display, debugging.
     */
    private Map<String, Object> buildMetadata(Document doc) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("documentId", String.valueOf(doc.getId()));
        meta.put("titre",      doc.getTitre());
        meta.put("auteur",     doc.getAuteur());
        meta.put("isbn",       doc.getIsbn());
        meta.put("annee",      String.valueOf(doc.getAnneePublication()));
        meta.put("categorie",  doc.getCategorie() != null
                ? doc.getCategorie().getNom() : "N/A");
        meta.put("source",     "bibliotech-pdf");
        return meta;
    }
}