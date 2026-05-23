package com.bibliotech.tts;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VttSerializer {

    /**
     * Produces a valid WebVTT string from caption segments.
     *
     * Format:
     *   WEBVTT
     *
     *   1
     *   00:00:00.000 --> 00:00:02.400
     *   This is the first caption segment.
     */
    public String serialize(List<CaptionSegment> segments) {
        StringBuilder sb = new StringBuilder("WEBVTT\n\n");

        for (CaptionSegment seg : segments) {
            sb.append(seg.index() + 1).append("\n");
            sb.append(formatTimestamp(seg.startMs()))
                    .append(" --> ")
                    .append(formatTimestamp(seg.endMs()))
                    .append("\n");
            sb.append(seg.text()).append("\n\n");
        }

        return sb.toString();
    }

    // ── HH:MM:SS.mmm ─────────────────────────────────────────────
    private String formatTimestamp(long ms) {
        long hours   = ms / 3_600_000;
        long minutes = (ms % 3_600_000) / 60_000;
        long seconds = (ms % 60_000) / 1_000;
        long millis  = ms % 1_000;
        return String.format("%02d:%02d:%02d.%03d",
                hours, minutes, seconds, millis);
    }
}