package com.bibliotech.openlibrary;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

// Réponse brute de l'API OpenLibrary /api/books?bibkeys=ISBN:...
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenLibraryDTO(
        String titre,
        String auteur,
        Integer anneePublication,
        String editeur,
        String description,
        String couvertureUrl,
        String isbn
) {
    // DTO interne pour parser la réponse nested d'OpenLibrary
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BookDetails(
            @JsonProperty("title") String title,
            @JsonProperty("authors") List<AuthorRef> authors,
            @JsonProperty("publishers") List<Publisher> publishers,
            @JsonProperty("publish_date") String publishDate,
            @JsonProperty("identifiers") Identifiers identifiers,
            @JsonProperty("cover") Cover cover,
            @JsonProperty("excerpts") List<Excerpt> excerpts
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AuthorRef(@JsonProperty("name") String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Publisher(@JsonProperty("name") String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Identifiers(
            @JsonProperty("isbn_13") List<String> isbn13,
            @JsonProperty("isbn_10") List<String> isbn10
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cover(
            @JsonProperty("medium") String medium,
            @JsonProperty("large") String large
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Excerpt(@JsonProperty("text") String text) {}
}