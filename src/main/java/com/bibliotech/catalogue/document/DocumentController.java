package com.bibliotech.catalogue.document;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService service;

    // ── GET — paginated list / search ─────────────────────────────
    @GetMapping
    public ResponseEntity<Page<DocumentDTO>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categorieId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "dateAjout") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equals("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(
                service.findPaginated(q, categorieId, pageable));
    }

    // ── GET — advanced search ─────────────────────────────────────
    @GetMapping("/advanced")
    public ResponseEntity<Page<DocumentDTO>> advancedSearch(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categorieId,
            @RequestParam(required = false) Integer anneeMin,
            @RequestParam(required = false) Integer anneeMax,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("dateAjout").descending());
        return ResponseEntity.ok(
                service.advancedSearch(q, categorieId,
                        anneeMin, anneeMax, pageable));
    }

    // ── GET — by id ───────────────────────────────────────────────
    @GetMapping("/{id}")
    public DocumentDTO findById(@PathVariable Long id) {
        return service.findById(id);
    }

    // ── POST — create ─────────────────────────────────────────────
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentDTO> create(
            @RequestParam String titre,
            @RequestParam String auteur,
            @RequestParam String isbn,
            @RequestParam int anneePublication,
            @RequestParam(required = false) String editeur,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String couvertureUrl,
            @RequestParam Long categorieId,
            @RequestParam(required = false) MultipartFile pdfFile) {

        DocumentRequest req = new DocumentRequest(titre, auteur, isbn,
                anneePublication, editeur, description,
                couvertureUrl, categorieId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(req, pdfFile));
    }

    // ── PUT — update ──────────────────────────────────────────────
    @PutMapping(value = "/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public DocumentDTO update(
            @PathVariable Long id,
            @RequestParam String titre,
            @RequestParam String auteur,
            @RequestParam int anneePublication,
            @RequestParam(required = false) String editeur,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String couvertureUrl,
            @RequestParam Long categorieId,
            @RequestParam(required = false) MultipartFile pdfFile) {

        DocumentRequest req = new DocumentRequest(titre, auteur, null,
                anneePublication, editeur, description,
                couvertureUrl, categorieId);
        return service.update(id, req, pdfFile);
    }

    // ── DELETE ────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── GET — download PDF ────────────────────────────────────────
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        DocumentDTO doc = service.findById(id);
        byte[] bytes = service.getPdfBytes(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + doc.titre() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(bytes.length)
                .body(bytes);
    }

    // ── GET — read online ─────────────────────────────────────────
    @GetMapping("/{id}/pdf/inline")
    public ResponseEntity<byte[]> readOnline(@PathVariable Long id) {
        DocumentDTO doc = service.findById(id);
        byte[] bytes = service.getPdfBytes(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + doc.titre() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(bytes.length)
                .body(bytes);
    }
}