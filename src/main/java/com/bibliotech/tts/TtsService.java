package com.bibliotech.tts;

import com.bibliotech.exception.BusinessException;
import com.bibliotech.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TtsService {

    private final StorageService storageService;
    private static final String ESPEAK_PATH =
            "C:\\Program Files\\eSpeak NG\\espeak-ng.exe";
    /**
     * Extrait le texte d'un PDF stocké.
     * @param fichierPdfPath chemin absolu du PDF (depuis Document.fichierPdf)
     */
    public String extractText(String fichierPdfPath) {
        byte[] pdfBytes = storageService.load(fichierPdfPath);
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(Math.min(doc.getNumberOfPages(), 10));
            return stripper.getText(doc).replaceAll("\\s+", " ").trim();
        } catch (IOException e) {
            throw new BusinessException(
                    "Erreur extraction texte PDF : " + e.getMessage());
        }
    }

    /**
     * Génère un fichier MP3 à partir du texte via espeak-ng (Windows/Linux).
     * Retourne les bytes audio.
     */
    public byte[] generateAudioEspeak(String text, String langue)
            throws IOException, InterruptedException {

        String truncated = text.length() > 3000
                ? text.substring(0, 3000) + "..." : text;

        Path tempOutput = Files.createTempFile("tts_", ".wav");
        Path tempText   = Files.createTempFile("tts_text_", ".txt");
        Files.writeString(tempText, truncated, StandardCharsets.UTF_8);

        try {
            String voice = "fr".equals(langue) ? "fr" : "en";

            ProcessBuilder pb = new ProcessBuilder(
                    ESPEAK_PATH,    // ✅ chemin absolu
                    "-f", tempText.toAbsolutePath().toString(),
                    "-w", tempOutput.toAbsolutePath().toString(),
                    "-v", voice,
                    "--speed=150"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output = new String(
                    process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                log.error("espeak-ng output: {}", output);
                throw new BusinessException(
                        "espeak-ng a échoué (code "
                                + exitCode + ") : " + output);
            }

            byte[] audioBytes = Files.readAllBytes(tempOutput);
            log.info("Audio généré : {} bytes", audioBytes.length);
            return audioBytes;

        } finally {
            Files.deleteIfExists(tempText);
            Files.deleteIfExists(tempOutput);
        }
    }
}