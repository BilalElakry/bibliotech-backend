package com.bibliotech.openlibrary;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/openlibrary")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class OpenLibraryController {

    private final OpenLibraryService openLibraryService;

    /**
     * GET /api/openlibrary/isbn/{isbn}
     * Lookup d'un livre par ISBN, retourne les métadonnées pour pré-remplir le formulaire.
     */
    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<OpenLibraryMetadataDTO> getByIsbn(
            @PathVariable String isbn) {
        return openLibraryService.lookupByIsbn(isbn)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/openlibrary/search?q=...&limit=10
     * Recherche par mot-clé, retourne une liste de résultats.
     */
    @GetMapping("/search")
    public ResponseEntity<List<OpenLibraryMetadataDTO>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {
        if (q == null || q.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(
                openLibraryService.searchBooks(q, limit));
    }
}