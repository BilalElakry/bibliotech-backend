// tts/TtsCaptionResponse.java
package com.bibliotech.tts;

import java.util.List;

public record TtsCaptionResponse(
        Long documentId,
        String titre,
        int totalSegments,
        long totalDurationMs,
        List<CaptionSegment> segments
) {}