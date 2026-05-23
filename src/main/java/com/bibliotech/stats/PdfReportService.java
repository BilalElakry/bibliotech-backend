package com.bibliotech.stats;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfReportService {

    private final StatsService statsService;

    // ── Fonts ─────────────────────────────────────────────────────
    private static final Font FONT_TITLE = new Font(
            Font.HELVETICA, 22, Font.BOLD, new Color(30, 58, 138));
    private static final Font FONT_SUBTITLE = new Font(
            Font.HELVETICA, 10, Font.NORMAL, new Color(100, 116, 139));
    private static final Font FONT_SECTION = new Font(
            Font.HELVETICA, 13, Font.BOLD, new Color(30, 58, 138));
    private static final Font FONT_TABLE_HEADER = new Font(
            Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
    private static final Font FONT_TABLE_CELL = new Font(
            Font.HELVETICA, 10, Font.NORMAL, new Color(30, 41, 59));
    private static final Font FONT_SUMMARY_LABEL = new Font(
            Font.HELVETICA, 11, Font.NORMAL, new Color(71, 85, 105));
    private static final Font FONT_SUMMARY_VALUE = new Font(
            Font.HELVETICA, 14, Font.BOLD, new Color(30, 58, 138));
    private static final Font FONT_FOOTER = new Font(
            Font.HELVETICA, 8, Font.NORMAL, new Color(148, 163, 184));

    // ── Colors ────────────────────────────────────────────────────
    private static final Color COLOR_HEADER_BG  = new Color(30, 58, 138);
    private static final Color COLOR_ROW_ODD    = new Color(248, 250, 252);
    private static final Color COLOR_ROW_EVEN   = Color.WHITE;
    private static final Color COLOR_BORDER     = new Color(203, 213, 225);
    private static final Color COLOR_SUMMARY_BG = new Color(239, 246, 255);
    private static final Color COLOR_ACCENT     = new Color(59, 130, 246);

    public byte[] generateReport() {
        try {
            StatsDTO stats = statsService.getStats();
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Document doc = new Document(PageSize.A4, 40, 40, 60, 60);
            PdfWriter writer = PdfWriter.getInstance(doc, out);

            // ── Page event for header/footer on every page ────────
            writer.setPageEvent(new HeaderFooterEvent());

            doc.open();

            addTitle(doc);
            addGeneratedDate(doc);
            addSeparator(doc);
            addSummarySection(doc, stats);
            addSeparator(doc);
            addTopDocumentsSection(doc, stats);
            addSeparator(doc);
            addRecentAccessSection(doc, stats);

            doc.close();
            return out.toByteArray();

        } catch (Exception e) {
            log.error("Erreur génération rapport PDF : {}", e.getMessage());
            throw new RuntimeException(
                    "Erreur génération rapport PDF : " + e.getMessage(), e);
        }
    }

    // ── Title ─────────────────────────────────────────────────────
    private void addTitle(Document doc) throws DocumentException {
        Paragraph title = new Paragraph("📚 BiblioTech", FONT_TITLE);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(4);
        doc.add(title);

        Paragraph sub = new Paragraph(
                "Rapport Statistiques de la Bibliothèque Numérique",
                FONT_SUBTITLE);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(6);
        doc.add(sub);
    }

    // ── Generated date ────────────────────────────────────────────
    private void addGeneratedDate(Document doc) throws DocumentException {
        String date = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm"));
        Paragraph p = new Paragraph("Généré le : " + date, FONT_SUBTITLE);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(10);
        doc.add(p);
    }

    // ── Horizontal separator ──────────────────────────────────────
    private void addSeparator(Document doc) throws DocumentException {
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setFixedHeight(2f);
        cell.setBackgroundColor(COLOR_ACCENT);
        cell.setBorder(Rectangle.NO_BORDER);
        line.addCell(cell);
        line.setSpacingBefore(10);
        line.setSpacingAfter(14);
        doc.add(line);
    }

    // ── Summary section ───────────────────────────────────────────
    private void addSummarySection(Document doc,
                                   StatsDTO stats) throws DocumentException {
        addSectionTitle(doc, "Résumé général");

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingAfter(10);

        addSummaryCard(table, "Documents",      String.valueOf(stats.totalDocuments()));
        addSummaryCard(table, "Utilisateurs",   String.valueOf(stats.totalUtilisateurs()));
        addSummaryCard(table, "Téléchargements",String.valueOf(stats.totalTelechargements()));
        addSummaryCard(table, "Lectures",       String.valueOf(stats.totalLectures()));

        doc.add(table);
    }

    private void addSummaryCard(PdfPTable table,
                                String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(COLOR_SUMMARY_BG);
        cell.setBorderColor(COLOR_BORDER);
        cell.setBorderWidth(1f);
        cell.setPadding(14);

        Paragraph lbl = new Paragraph(label, FONT_SUMMARY_LABEL);
        lbl.setAlignment(Element.ALIGN_CENTER);
        Paragraph val = new Paragraph(value, FONT_SUMMARY_VALUE);
        val.setAlignment(Element.ALIGN_CENTER);

        cell.addElement(lbl);
        cell.addElement(val);
        table.addCell(cell);
    }

    // ── Top documents section ─────────────────────────────────────
    private void addTopDocumentsSection(Document doc,
                                        StatsDTO stats) throws DocumentException {
        addSectionTitle(doc, "Top 5 — Documents les plus téléchargés");

        if (stats.topDocuments().isEmpty()) {
            doc.add(new Paragraph("Aucun téléchargement enregistré.",
                    FONT_TABLE_CELL));
            return;
        }

        PdfPTable table = new PdfPTable(new float[]{0.5f, 3.5f, 2.5f, 1f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(10);

        // Header row
        addTableHeader(table, "#", "Titre", "Auteur", "Téléch.");

        // Data rows
        int rank = 1;
        for (DocumentStatDTO d : stats.topDocuments()) {
            boolean odd = (rank % 2 != 0);
            addTableRow(table, odd,
                    String.valueOf(rank++),
                    truncate(d.titre(), 45),
                    truncate(d.auteur(), 30),
                    String.valueOf(d.nombreTelechargements()));
        }

        doc.add(table);
    }

    // ── Recent access section ─────────────────────────────────────
    private void addRecentAccessSection(Document doc,
                                        StatsDTO stats) throws DocumentException {
        addSectionTitle(doc, "10 accès les plus récents");

        if (stats.accesRecents().isEmpty()) {
            doc.add(new Paragraph("Aucun accès enregistré.",
                    FONT_TABLE_CELL));
            return;
        }

        PdfPTable table = new PdfPTable(new float[]{3f, 2f, 1.5f, 2f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(10);

        // Header row
        addTableHeader(table, "Document",
                "Utilisateur", "Type", "Date");

        // Data rows
        int i = 1;
        for (AccesRecentDTO a : stats.accesRecents()) {
            boolean odd = (i++ % 2 != 0);
            addTableRow(table, odd,
                    truncate(a.documentTitre(), 40),
                    truncate(a.utilisateurEmail(), 28),
                    formatType(a.typeAcces()),
                    formatDate(a.dateAcces()));
        }

        doc.add(table);
    }

    // ── Helpers ───────────────────────────────────────────────────

    private void addSectionTitle(Document doc,
                                 String title) throws DocumentException {
        Paragraph p = new Paragraph(title, FONT_SECTION);
        p.setSpacingBefore(4);
        p.setSpacingAfter(10);
        doc.add(p);
    }

    private void addTableHeader(PdfPTable table, String... headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, FONT_TABLE_HEADER));
            cell.setBackgroundColor(COLOR_HEADER_BG);
            cell.setPadding(8);
            cell.setBorderColor(COLOR_HEADER_BG);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            table.addCell(cell);
        }
    }

    private void addTableRow(PdfPTable table, boolean odd,
                             String... values) {
        Color bg = odd ? COLOR_ROW_ODD : COLOR_ROW_EVEN;
        for (String v : values) {
            PdfPCell cell = new PdfPCell(
                    new Phrase(v != null ? v : "—", FONT_TABLE_CELL));
            cell.setBackgroundColor(bg);
            cell.setPadding(7);
            cell.setBorderColor(COLOR_BORDER);
            cell.setBorderWidth(0.5f);
            table.addCell(cell);
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "—";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    private String formatType(String type) {
        if (type == null) return "—";
        return switch (type) {
            case "TELECHARGEMENT"   -> "Téléch.";
            case "LECTURE_EN_LIGNE" -> "Lecture";
            default                  -> type;
        };
    }

    private String formatDate(String iso) {
        if (iso == null) return "—";
        try {
            LocalDateTime dt = LocalDateTime.parse(iso);
            return dt.format(DateTimeFormatter
                    .ofPattern("dd/MM/yyyy HH:mm"));
        } catch (Exception e) {
            return iso.length() > 16 ? iso.substring(0, 16) : iso;
        }
    }

    // ── Header/Footer page event ──────────────────────────────────
    private class HeaderFooterEvent extends PdfPageEventHelper {

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            Rectangle page = document.getPageSize();

            // Top colored bar
            cb.setColorFill(COLOR_HEADER_BG);
            cb.rectangle(0, page.getHeight() - 30,
                    page.getWidth(), 30);
            cb.fill();

            // Header text
            ColumnText.showTextAligned(cb,
                    Element.ALIGN_LEFT,
                    new Phrase("ScribeAI — Rapport Statistiques",
                            new Font(Font.HELVETICA, 9,
                                    Font.NORMAL, Color.WHITE)),
                    40, page.getHeight() - 18, 0);

            ColumnText.showTextAligned(cb,
                    Element.ALIGN_RIGHT,
                    new Phrase("EMSI Rabat — 2025/2026",
                            new Font(Font.HELVETICA, 9,
                                    Font.NORMAL, Color.WHITE)),
                    page.getWidth() - 40,
                    page.getHeight() - 18, 0);

            // Bottom footer line
            cb.setColorFill(COLOR_ACCENT);
            cb.rectangle(0, 28, page.getWidth(), 2);
            cb.fill();

            // Page number
            ColumnText.showTextAligned(cb,
                    Element.ALIGN_CENTER,
                    new Phrase("Page " + writer.getPageNumber()
                            + "  —  © 2026 Bilal Elakry & Haitam Khouna",
                            FONT_FOOTER),
                    page.getWidth() / 2, 15, 0);
        }
    }
}