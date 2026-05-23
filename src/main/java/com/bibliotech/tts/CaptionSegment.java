package com.bibliotech.tts;

public record CaptionSegment(
        int index,
        long startMs,
        long endMs,
        String text,
        int page
) {}