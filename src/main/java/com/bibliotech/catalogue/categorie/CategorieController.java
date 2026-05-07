package com.bibliotech.catalogue.categorie;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CategorieController {

    private final CategorieService service;

    @GetMapping
    public List<CategorieDTO> findAll() { return service.findAll(); }

    @GetMapping("/{id}")
    public CategorieDTO findById(@PathVariable Long id) { return service.findById(id); }

    @PostMapping
    public ResponseEntity<CategorieDTO> create(@Valid @RequestBody CategorieRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    public CategorieDTO update(@PathVariable Long id,
                               @Valid @RequestBody CategorieRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}