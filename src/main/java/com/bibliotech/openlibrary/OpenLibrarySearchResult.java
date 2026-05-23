package com.bibliotech.openlibrary;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

// Réponse de /search.json
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenLibrarySearchResult(
        @JsonProperty("numFound") int numFound,
        @JsonProperty("docs") List<OpenLibraryDoc> docs
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OpenLibraryDoc(
            @JsonProperty("key") String key,
            @JsonProperty("title") String title,
            @JsonProperty("author_name") List<String> authorNames,
            @JsonProperty("first_publish_year") Integer firstPublishYear,
            @JsonProperty("publisher") List<String> publishers,
            @JsonProperty("isbn") List<String> isbns,
            @JsonProperty("cover_i") Integer coverId,
            @JsonProperty("subject") List<String> subjects
    ) {
        public String firstIsbn() {
            if (isbns == null || isbns.isEmpty()) return null;
            // préférer ISBN-13
            return isbns.stream()
                    .filter(i -> i.length() == 13)
                    .findFirst()
                    .orElse(isbns.get(0));
        }

        public String firstAuthor() {
            return (authorNames != null && !authorNames.isEmpty())
                    ? authorNames.get(0) : "Auteur inconnu";
        }

        public String coverUrl() {
            return coverId != null
                    ? "https://covers.openlibrary.org/b/id/" + coverId + "-M.jpg"
                    : null;
        }
    }
}