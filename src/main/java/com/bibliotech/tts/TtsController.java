package com.bibliotech.tts;

import com.bibliotech.catalogue.document.Document;
import com.bibliotech.catalogue.document.DocumentRepository;
import com.bibliotech.exception.BusinessException;
import com.bibliotech.exception.ResourceNotFoundException;
import com.bibliotech.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/tts")
@RequiredArgsConstructor
public class TtsController {

    private final TtsService ttsService;
    private final DocumentRepository documentRepository;
    private final ChunkingService chunkingService;
    private final VttSerializer vttSerializer;
    private final StorageService storageService;

    // ─────────────────────────────────────────────
    // 1. AUDIO — synchrone, simple, fiable.
    //    Le cache disque (generateAudioCached) rend les appels
    //    répétés quasi-instantanés ; seule la 1ère génération
    //    par (document, langue) coûte ~30-40s.
    // ─────────────────────────────────────────────
    @GetMapping("/documents/{id}/audio")
    public ResponseEntity<Resource> getAudio(
            @PathVariable Long id,
            @RequestParam(defaultValue = "fr") String langue) {


        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable : " + id));

        if (doc.getFichierPdf() == null) {
            throw new BusinessException("Ce document n'a pas de fichier PDF");
        }

        try {
            String text = ttsService.extractText(doc.getFichierPdf());
            if (text == null || text.isBlank()) {
                throw new BusinessException("PDF vide ou non extractible");
            }

            // 🔁 Force l’existence du fichier cache (génération si besoin)
            ttsService.generateAudioCached(id, text, langue);

            // 🎯 Lit directement le fichier cache depuis le disque
            Path cacheFile = ttsService.cacheFileFor(id, langue);
            if (!Files.exists(cacheFile) || Files.size(cacheFile) == 0) {
                throw new BusinessException("Le fichier audio n'a pas pu être créé");
            }

            FileSystemResource resource = new FileSystemResource(cacheFile.toFile());

            String extension = ttsService.isOpenAiEngine() ? ".mp3" : ".wav";
            MediaType mediaType = ttsService.isOpenAiEngine()
                    ? MediaType.valueOf("audio/mpeg")
                    : MediaType.valueOf("audio/wav");


            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .contentLength(resource.contentLength())
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + doc.getTitre().replaceAll("[^a-zA-Z0-9]", "_") + extension + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .body(resource);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Erreur génération audio");
        }
    }

    // ─────────────────────────────────────────────
    // 2. TEXT ONLY (for Web Speech API)
    // ─────────────────────────────────────────────
    @GetMapping("/documents/{id}/text")
    public ResponseEntity<TtsTextResponse> getText(
            @PathVariable Long id,
            @RequestParam(defaultValue = "5000") int maxChars) {

        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable : " + id));

        if (doc.getFichierPdf() == null) {
            throw new BusinessException("PDF manquant");
        }

        String fullText = ttsService.extractText(doc.getFichierPdf());

        if (fullText == null || fullText.isBlank()) {
            throw new BusinessException("Texte vide");
        }

        String clipped = fullText.length() > maxChars
                ? fullText.substring(0, maxChars)
                : fullText;

        return ResponseEntity.ok(new TtsTextResponse(
                doc.getId(),
                doc.getTitre(),
                clipped,
                fullText.split("\\s+").length,
                fullText.length()
        ));
    }

    // ─────────────────────────────────────────────
    // 3. CAPTIONS JSON
    // ─────────────────────────────────────────────
    @GetMapping("/documents/{id}/captions")
    public ResponseEntity<TtsCaptionResponse> getCaptions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") int maxPages,
            @RequestParam(defaultValue = "0") int skipPages) {

        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable : " + id));

        try {
            byte[] pdf = storageService.load(doc.getFichierPdf());

            List<CaptionSegment> segments =
                    chunkingService.buildCaptions(pdf, maxPages, skipPages);

            long duration = segments.isEmpty()
                    ? 0
                    : segments.get(segments.size() - 1).endMs();

            return ResponseEntity.ok(
                    new TtsCaptionResponse(
                            doc.getId(),
                            doc.getTitre(),
                            segments.size(),
                            duration,
                            segments
                    )
            );

        } catch (IOException e) {
            throw new BusinessException("Erreur génération captions PDF");
        }
    }

    // ─────────────────────────────────────────────
    // 4. WEBVTT DOWNLOAD
    // ─────────────────────────────────────────────
    @GetMapping("/documents/{id}/vtt")
    public ResponseEntity<byte[]> getVtt(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") int maxPages,
            @RequestParam(defaultValue = "0") int skipPages) {

        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable : " + id));

        final byte[] pdf;
        final List<CaptionSegment> segments;

        try {
            pdf = storageService.load(doc.getFichierPdf());
            segments = chunkingService.buildCaptions(pdf, maxPages, skipPages);
        } catch (IOException e) {
            throw new BusinessException("Erreur lors de la génération du fichier VTT");
        }

        String vtt = vttSerializer.serialize(segments);
        byte[] bytes = vtt.getBytes(StandardCharsets.UTF_8);

        String filename = doc.getTitre()
                .replaceAll("[^a-zA-Z0-9]", "_") + ".vtt";

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("text/vtt; charset=UTF-8"))
                .contentLength(bytes.length)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(bytes);
    }

    // ─────────────────────────────────────────────
    // 5. ASYNC TRIGGER — Pré-génération pour éviter le Timeout
    // ─────────────────────────────────────────────
    @PostMapping("/documents/{id}/pre-generate")
    public ResponseEntity<java.util.Map<String, String>> preGenerateAudio(
            @PathVariable Long id,
            @RequestParam(defaultValue = "en") String langue) {


        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable : " + id));

        if (doc.getFichierPdf() == null) {
            throw new BusinessException("Ce document n'a pas de fichier PDF");
        }

        // 1. Extraction rapide du texte (synchrone)
        String text = ttsService.extractText(doc.getFichierPdf());
        if (text == null || text.isBlank()) {
            throw new BusinessException("PDF vide ou non extractible");
        }

        // 2. Déclenchement de la génération lourde en tâche de fond (asynchrone)
        ttsService.preGenerateAudioCacheAsync(id, text, langue);

        // 3. Réponse immédiate (Statut 202 Accepted) au navigateur !
        return ResponseEntity.accepted().body(java.util.Map.of(
                "status", "PROCESSING",
                "message", "La génération du cache audio (" + langue + ") a commencé en tâche de fond."
        ));
    }
}

