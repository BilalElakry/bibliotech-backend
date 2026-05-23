package com.bibliotech.tts;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkingService {

    private static final int WORDS_PER_CHUNK = 12;
    private static final double WPM = 150.0;
    private static final double MS_PER_WORD = (60_000.0 / WPM);
    private static final int MIN_WORDS_PER_LINE = 3;
    private static final int MAX_LINE_LENGTH = 300;

    public List<CaptionSegment> buildCaptions(byte[] pdfBytes,
                                              int maxPages,
                                              int skipPages)
            throws IOException {

        List<RawLine> lines = extractByPage(pdfBytes, maxPages, skipPages);
        List<String[]> chunks = chunkWords(lines);

        List<CaptionSegment> segments = new ArrayList<>();
        long cursor = 0L;

        for (int i = 0; i < chunks.size(); i++) {
            String[] parts = chunks.get(i);
            String text = parts[0];
            int page = Integer.parseInt(parts[1]);

            int wordCount = text.split("\\s+").length;
            long duration = Math.round(wordCount * MS_PER_WORD);

            segments.add(new CaptionSegment(
                    i,
                    cursor,
                    cursor + duration,
                    text,
                    page
            ));
            cursor += duration;
        }

        return segments;
    }

    // ── Extract text line by line, page by page ───────────────────
    private List<RawLine> extractByPage(byte[] pdfBytes,
                                        int maxPages,
                                        int skipPages)
            throws IOException {

        List<RawLine> lines = new ArrayList<>();

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            int totalPages = doc.getNumberOfPages();
            int startPage = 1 + skipPages;
            int endPage = Math.min(totalPages, startPage + maxPages - 1);

            if (startPage > totalPages) return lines;

            for (int p = startPage; p <= endPage; p++) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                String pageText = stripper.getText(doc);

                for (String line : pageText.split("\\r?\\n")) {
                    String trimmed = line.trim();
                    if (!isNoisyLine(trimmed)) {
                        lines.add(new RawLine(trimmed, p));
                    }
                }
            }
        }

        return lines;
    }

    // ── Group words into fixed-size chunks ────────────────────────
    private List<String[]> chunkWords(List<RawLine> lines) {
        List<String> wordBuffer = new ArrayList<>();
        List<Integer> pageBuffer = new ArrayList<>();

        for (RawLine line : lines) {
            String[] words = line.text().split("\\s+");
            for (String w : words) {
                if (!w.isBlank()) {
                    wordBuffer.add(w);
                    pageBuffer.add(line.page());
                }
            }
        }

        List<String[]> chunks = new ArrayList<>();
        int i = 0;
        while (i < wordBuffer.size()) {
            int end = Math.min(i + WORDS_PER_CHUNK, wordBuffer.size());
            String text = String.join(" ", wordBuffer.subList(i, end));
            int page = pageBuffer.get(i);
            chunks.add(new String[]{text, String.valueOf(page)});
            i = end;
        }

        return chunks;
    }

    // ── Noise filter ──────────────────────────────────────────────
    private boolean isNoisyLine(String line) {
        if (line.isEmpty()) return true;

        // Too short to be meaningful content
        String[] words = line.split("\\s+");
        if (words.length < MIN_WORDS_PER_LINE) return true;

        // All uppercase — typical PDF headers, chapter titles, cover text
        if (line.equals(line.toUpperCase())
                && line.matches(".*[A-Z]{3,}.*")) return true;

        // URLs and domain names
        if (line.toLowerCase().contains("http")
                || line.toLowerCase().contains("www.")
                || line.matches(".*\\.[a-z]{2,4}/.*")) return true;

        // Lines that are just numbers (page numbers, footnote markers)
        if (line.matches("^[0-9\\s\\-–—.]+$")) return true;

        // Garbled / extremely long lines (OCR artifacts)
        if (line.length() > MAX_LINE_LENGTH) return true;

        return false;
    }

    private record RawLine(String text, int page) {}
}