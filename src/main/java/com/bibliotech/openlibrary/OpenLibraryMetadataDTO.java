package com.bibliotech.openlibrary;

/**
 * DTO renvoyé au front-end après lookup/search OpenLibrary.
 * Directement mappable sur le formulaire d'ajout de document.
 */
public record OpenLibraryMetadataDTO(
        String titre,
        String auteur,
        String isbn,
        Integer anneePublication,
        String editeur,
        String description,
        String couvertureUrl,
        String source // "isbn_lookup" ou "search"
) {}