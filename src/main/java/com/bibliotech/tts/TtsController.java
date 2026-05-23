package com.bibliotech.tts;

import com.bibliotech.catalogue.document.Document;
import com.bibliotech.catalogue.document.DocumentRepository;
import com.bibliotech.exception.BusinessException;
import com.bibliotech.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/tts")
@RequiredArgsConstructor
public class TtsController {

    private final TtsService ttsService;
    private final DocumentRepository documentRepository;

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

            byte[] audioBytes = ttsService.generateAudioEspeak(
                    text, langue);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\""
                                    + doc.getTitre() + ".wav\"")
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

    // ── Endpoint 2 : Texte extrait pour Web Speech API ────────────
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
    }
}