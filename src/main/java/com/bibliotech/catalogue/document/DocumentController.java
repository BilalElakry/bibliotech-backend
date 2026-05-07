package com.bibliotech.catalogue.document;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService service;

    @GetMapping
    public List<DocumentDTO> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categorieId) {
        if (q != null && !q.isBlank())    return service.search(q);
        if (categorieId != null)           return service.findByCategorie(categorieId);
        return service.findAll();
    }

    @GetMapping("/{id}")
    public DocumentDTO findById(@PathVariable Long id) {
        return service.findById(id);
    }

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
                anneePublication, editeur, description, couvertureUrl, categorieId);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req, pdfFile));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
                anneePublication, editeur, description, couvertureUrl, categorieId);
        return service.update(id, req, pdfFile);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

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