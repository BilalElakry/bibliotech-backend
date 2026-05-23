package com.bibliotech.tts;

import com.bibliotech.catalogue.document.Document;
import com.bibliotech.catalogue.document.DocumentRepository;
import com.bibliotech.exception.BusinessException;
import com.bibliotech.exception.ResourceNotFoundException;
import com.bibliotech.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
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

    // ── Endpoint 1 : Audio WAV via espeak-ng ─────────────────────
    @GetMapping("/documents/{id}/audio")
    public ResponseEntity<byte[]> getAudio(
            @PathVariable Long id,
            @RequestParam(defaultValue = "fr") String langue) {

        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document introuvable : " + id));

        if (doc.getFichierPdf() == null)
            throw new BusinessException(
                    "Ce document n'a pas de fichier PDF");

        try {
            String text = ttsService.extractText(doc.getFichierPdf());

            if (text.isBlank())
                throw new BusinessException(
                        "PDF non extractible (scanné sans OCR ?)");

            byte[] audioBytes = ttsService.generateAudioEspeak(text, langue);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + doc.getTitre() + ".wav\"")
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                    .contentType(MediaType.parseMediaType("audio/wav"))
                    .contentLength(audioBytes.length)
                    .body(audioBytes);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur TTS audio doc {} : {}", id, e.getMessage());
            throw new BusinessException(
                    "Erreur génération audio : " + e.getMessage());
        }
    }

    // ── Endpoint 2 : Texte extrait pour Web Speech API ───────────
    @GetMapping("/documents/{id}/text")
    public ResponseEntity<TtsTextResponse> getText(
            @PathVariable Long id,
            @RequestParam(defaultValue = "5000") int maxChars) {

        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document introuvable : " + id));

        if (doc.getFichierPdf() == null)
            throw new BusinessException(
                    "Ce document n'a pas de fichier PDF");

        try {
            String fullText = ttsService.extractText(doc.getFichierPdf());

            if (fullText.isBlank())
                throw new BusinessException(
                        "PDF non extractible (scanné sans OCR ?)");

            String textForSpeech = fullText.length() > maxChars
                    ? fullText.substring(0, maxChars) + "..."
                    : fullText;

            return ResponseEntity.ok(new TtsTextResponse(
                    doc.getId(),
                    doc.getTitre(),
                    textForSpeech,
                    fullText.split("\\s+").length,
                    fullText.length()
            ));

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur TTS text doc {} : {}", id, e.getMessage());
            throw new BusinessException(
                    "Erreur extraction texte : " + e.getMessage());
        }
    }

    // ── Endpoint 3 : JSON caption segments ───────────────────────
    @GetMapping("/documents/{id}/captions")
    public ResponseEntity<TtsCaptionResponse> getCaptions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") int maxPages,
            @RequestParam(defaultValue = "0") int skipPages) {

        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document introuvable : " + id));

        if (doc.getFichierPdf() == null)
            throw new BusinessException(
                    "Ce document n'a pas de fichier PDF");

        try {
            byte[] pdfBytes = storageService.load(doc.getFichierPdf());
            List<CaptionSegment> segments =
                    chunkingService.buildCaptions(pdfBytes, maxPages, skipPages);

            long totalDuration = segments.isEmpty() ? 0
                    : segments.get(segments.size() - 1).endMs();

            return ResponseEntity.ok(new TtsCaptionResponse(
                    doc.getId(),
                    doc.getTitre(),
                    segments.size(),
                    totalDuration,
                    segments
            ));

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur génération captions doc {} : {}",
                    id, e.getMessage());
            throw new BusinessException(
                    "Erreur génération captions : " + e.getMessage());
        }
    }

    // ── Endpoint 4 : WebVTT file download ────────────────────────
    @GetMapping("/documents/{id}/vtt")
    public ResponseEntity<byte[]> getVtt(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") int maxPages,
            @RequestParam(defaultValue = "0") int skipPages) {

        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document introuvable : " + id));

        if (doc.getFichierPdf() == null)
            throw new BusinessException(
                    "Ce document n'a pas de fichier PDF");

        try {
            byte[] pdfBytes = storageService.load(doc.getFichierPdf());
            List<CaptionSegment> segments =
                    chunkingService.buildCaptions(pdfBytes, maxPages, skipPages);

            String vtt = vttSerializer.serialize(segments);
            byte[] vttBytes = vtt.getBytes(StandardCharsets.UTF_8);

            String filename = doc.getTitre()
                    .replaceAll("[^a-zA-Z0-9]", "_") + ".vtt";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType
                            .parseMediaType("text/vtt; charset=UTF-8"))
                    .contentLength(vttBytes.length)
                    .body(vttBytes);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur génération VTT doc {} : {}",
                    id, e.getMessage());
            throw new BusinessException(
                    "Erreur génération VTT : " + e.getMessage());
        }
    }
}