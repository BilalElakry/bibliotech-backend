package com.bibliotech.tts;

public record TtsTextResponse(
        Long documentId,
        String titre,
        String textPreview,
        int nombreMots,
        int nombreCaracteres
) {}