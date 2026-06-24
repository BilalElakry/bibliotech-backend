package com.bibliotech.tts;

import com.bibliotech.exception.BusinessException;
import com.bibliotech.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.text.Normalizer;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
@Slf4j
@Service
@RequiredArgsConstructor
public class TtsService {

    private final StorageService storageService;

    @Value("${bibliotech.storage.tts.engine:piper}")
    private String engine;

    @Value("${bibliotech.storage.tts.openai-api-key:}")
    private String openAiKey;

    @Value("${bibliotech.storage.tts.max-chars:4000}")
    private int maxChars;

    @Value("${bibliotech.storage.tts.max-pages:10}")
    private int maxPages;

    @Value("${bibliotech.storage.tts.piper.executable-path}")
    private String piperExecutablePath;

    @Value("${bibliotech.storage.upload-dir:./uploads}")
    private String uploadDir;


    public String extractText(String fichierPdfPath) {
        byte[] pdfBytes = storageService.load(fichierPdfPath);
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true); // améliore l'ordre de lecture
            stripper.setStartPage(1);
            stripper.setEndPage(Math.min(doc.getNumberOfPages(), maxPages));

            String raw = stripper.getText(doc);

            // 1. NFC : fusionne les diacritiques combinants séparés (e + ́ → é)
            String normalized = Normalizer.normalize(raw, Normalizer.Form.NFC);

            // 2. Supprime les caractères de contrôle parasites (sauf \n \t)
            normalized = normalized.replaceAll("[\\p{Cntrl}&&[^\n\t\r]]", "");

            // 3. Normalise les espaces
            normalized = normalized.replaceAll("[ \t]+", " ")
                    .replaceAll("\n{3,}", "\n\n")
                    .trim();

            log.info("[TTS] Extraction : {} → {} chars (après normalisation)",
                    raw.length(), normalized.length());
            return normalized;

        } catch (IOException e) {
            throw new BusinessException("Erreur extraction PDF : " + e.getMessage());
        }
    }

    public byte[] generateAudioCached(Long documentId, String text, String langue) {
        Path cacheFile = cacheFileFor(documentId, langue);

        if (Files.exists(cacheFile)) {
            try {
                byte[] cached = Files.readAllBytes(cacheFile);
                if (cached.length > 0) {
                    return cached;
                }
            } catch (IOException ignored) {
                // force regeneration if cache read fails
            }
        }

        byte[] audio = generateAudio(text, langue);

        try {
            Files.write(cacheFile, audio);
        } catch (IOException ignored) {
            // non-blocking cache write failure
        }

        return audio;
    }

    public Path cacheFileFor(Long documentId, String langue) {
        String ext = isOpenAiEngine() ? "mp3" : "wav";
        Path cacheDir = Paths.get(uploadDir, "tts-cache");
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException ignored) {}
        return cacheDir.resolve(documentId + "_" + langue + "." + ext);
    }

    public byte[] generateAudio(String text, String langue) {
        if ("openai".equalsIgnoreCase(engine)) {
            return generateAudioOpenAI(text, langue);
        } else {
            return generateAudioPiper(text, langue);
        }
    }

    public boolean isOpenAiEngine() {
        return "openai".equalsIgnoreCase(engine);
    }

    private byte[] generateAudioPiper(String text, String langue) {
        int safeLimit = Math.min(maxChars, 8000);
        String truncated = text.length() > safeLimit ? text.substring(0, safeLimit) : text;

        String selectedModel;
        if ("en".equalsIgnoreCase(langue)) {
            selectedModel = "C:\\piper\\en_US-ryan-high.onnx";
        } else {
            selectedModel = "C:\\piper\\fr_FR-siwis-medium.onnx";
        }

        Path tempWav = null;
        try {
            tempWav = Files.createTempFile("piper_output_", ".wav");

            ProcessBuilder pb = new ProcessBuilder(
                    piperExecutablePath,
                    "--model", selectedModel,
                    "--output_file", tempWav.toAbsolutePath().toString()
            );

            Process process = pb.start();

            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
                writer.write(truncated);
                writer.flush();
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new BusinessException("Le moteur Piper local a retourné un code d'erreur interne.");
            }

            byte[] audioBytes = Files.readAllBytes(tempWav);
            return audioBytes;

        } catch (Exception e) {
            throw new BusinessException("Impossible de générer l'audio localement via Piper : " + e.getMessage());
        } finally {
            if (tempWav != null) {
                try {
                    Files.deleteIfExists(tempWav);
                } catch (IOException ignored) {}
            }
        }
    }

    public byte[] generateAudioOpenAI(String text, String langue) {
        if (openAiKey == null || openAiKey.isBlank()) {
            throw new BusinessException("La clé API OpenAI n'est pas configurée sur le serveur.");
        }

        int safeLimit = Math.min(maxChars, 4000);
        String truncated = text.length() > safeLimit ? text.substring(0, safeLimit) : text;

        String url = "https://api.openai.com/v1/audio/speech";
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiKey);

        String voice = "fr".equalsIgnoreCase(langue) ? "nova" : "alloy";

        Map<String, String> body = new HashMap<>();
        body.put("model", "tts-1");
        body.put("input", truncated);
        body.put("voice", voice);
        body.put("response_format", "mp3");

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.POST, request, byte[].class);

            byte[] audioBytes = response.getBody();
            if (audioBytes == null || audioBytes.length == 0) {
                throw new BusinessException("Génération échouée : Fichier audio vide retourné par OpenAI.");
            }

            return audioBytes;

        } catch (Exception e) {
            throw new BusinessException("Impossible de contacter le service de voix HD (OpenAI). Vérifiez vos crédits.");
        }
    }

    @Async
    public void preGenerateAudioCacheAsync(Long documentId, String text, String langue) {
        try {
            generateAudioCached(documentId, text, langue);
        } catch (Exception ignored) {}
    }
}