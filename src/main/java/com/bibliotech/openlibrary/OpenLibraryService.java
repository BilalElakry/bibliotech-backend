package com.bibliotech.openlibrary;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenLibraryService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String BOOKS_API =
            "https://openlibrary.org/api/books";
    private static final String SEARCH_API =
            "https://openlibrary.org/search.json";
    private static final String COVERS_BASE =
            "https://covers.openlibrary.org/b/isbn/";

    /**
     * Lookup d'un livre par ISBN (ISBN-10 ou ISBN-13).
     * Utilise l'API Books d'OpenLibrary.
     */
    public Optional<OpenLibraryMetadataDTO> lookupByIsbn(String isbn) {
        try {
            String cleanIsbn = isbn.replaceAll("[^0-9X]", "");
            String url = UriComponentsBuilder
                    .fromHttpUrl(BOOKS_API)
                    .queryParam("bibkeys", "ISBN:" + cleanIsbn)
                    .queryParam("format", "json")
                    .queryParam("jscmd", "details")
                    .toUriString();

            log.info("OpenLibrary ISBN lookup : {}", url);
            String json = restTemplate.getForObject(url, String.class);

            if (json == null || json.equals("{}")) {
                log.warn("OpenLibrary : aucun résultat pour ISBN {}", isbn);
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(json);
            JsonNode bookNode = root.get("ISBN:" + cleanIsbn);
            if (bookNode == null) return Optional.empty();

            JsonNode details = bookNode.get("details");
            if (details == null) return Optional.empty();

            String titre = getTextOrNull(details, "title");
            String auteur = extractFirstAuthor(details);
            Integer annee = extractYear(details);
            String editeur = extractFirstPublisher(details);
            String description = extractDescription(details);
            String couverture = COVERS_BASE + cleanIsbn + "-M.jpg";

            return Optional.of(new OpenLibraryMetadataDTO(
                    titre, auteur, cleanIsbn,
                    annee, editeur, description,
                    couverture, "isbn_lookup"
            ));

        } catch (Exception e) {
            log.error("Erreur OpenLibrary ISBN lookup {} : {}",
                    isbn, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Recherche de livres par mot-clé (titre, auteur, sujet).
     * Retourne jusqu'à 10 résultats.
     */
    public List<OpenLibraryMetadataDTO> searchBooks(String query, int limit) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(SEARCH_API)
                    .queryParam("q", query)
                    .queryParam("limit", Math.min(limit, 20))
                    .queryParam("fields",
                            "key,title,author_name,first_publish_year,"
                                    + "publisher,isbn,cover_i,subject")
                    .toUriString();

            log.info("OpenLibrary search : {}", url);
            OpenLibrarySearchResult result = restTemplate.getForObject(
                    url, OpenLibrarySearchResult.class);

            if (result == null || result.docs() == null) {
                return Collections.emptyList();
            }

            return result.docs().stream()
                    .filter(d -> d.title() != null)
                    .map(d -> new OpenLibraryMetadataDTO(
                            d.title(),
                            d.firstAuthor(),
                            d.firstIsbn(),
                            d.firstPublishYear(),
                            (d.publishers() != null && !d.publishers().isEmpty())
                                    ? d.publishers().get(0) : null,
                            null, // description non disponible dans search
                            d.coverUrl(),
                            "search"
                    ))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Erreur OpenLibrary search '{}' : {}",
                    query, e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────

    private String getTextOrNull(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n != null && !n.isNull()) ? n.asText() : null;
    }

    private String extractFirstAuthor(JsonNode details) {
        JsonNode authors = details.get("authors");
        if (authors != null && authors.isArray() && !authors.isEmpty()) {
            JsonNode name = authors.get(0).get("name");
            if (name != null) return name.asText();
        }
        return null;
    }

    private Integer extractYear(JsonNode details) {
        JsonNode dateNode = details.get("publish_date");
        if (dateNode == null) return null;
        String dateStr = dateNode.asText();
        // formats possibles : "2005", "January 2005", "2005-01-01"
        try {
            return Integer.parseInt(
                    dateStr.replaceAll("[^0-9]", "").substring(0, 4));
        } catch (Exception e) {
            return null;
        }
    }

    private String extractFirstPublisher(JsonNode details) {
        JsonNode publishers = details.get("publishers");
        if (publishers != null && publishers.isArray() && !publishers.isEmpty()) {
            JsonNode name = publishers.get(0).get("name");
            if (name != null) return name.asText();
        }
        return null;
    }

    private String extractDescription(JsonNode details) {
        JsonNode excerpts = details.get("excerpts");
        if (excerpts != null && excerpts.isArray() && !excerpts.isEmpty()) {
            JsonNode text = excerpts.get(0).get("text");
            if (text != null) return text.asText();
        }
        // Fallback : subjects as description
        JsonNode subjects = details.get("subjects");
        if (subjects != null && subjects.isArray()) {
            List<String> subs = new ArrayList<>();
            subjects.forEach(s -> subs.add(s.get("name") != null
                    ? s.get("name").asText() : s.asText()));
            if (!subs.isEmpty()) {
                return "Thèmes : " + String.join(", ",
                        subs.subList(0, Math.min(5, subs.size())));
            }
        }
        return null;
    }
}