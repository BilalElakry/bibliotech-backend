package com.bibliotech.acces;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/acces")
@RequiredArgsConstructor
public class AccesController {

    private final AccesService accesService;

    @GetMapping("/documents/{id}/telecharger")
    public ResponseEntity<byte[]> telecharger(
            @PathVariable Long id, HttpServletRequest request) {
        byte[] bytes = accesService.telecharger(id, request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"document-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @GetMapping("/documents/{id}/lire")
    public ResponseEntity<byte[]> lire(
            @PathVariable Long id, HttpServletRequest request) {
        byte[] bytes = accesService.lireEnLigne(id, request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"document-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @GetMapping("/mon-historique")
    public List<AccesDTO> monHistorique() {
        return accesService.getMonHistorique();
    }

    @GetMapping("/utilisateurs/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AccesDTO> historiqueParUser(@PathVariable Long userId) {
        return accesService.getHistoriqueByUser(userId);
    }
}