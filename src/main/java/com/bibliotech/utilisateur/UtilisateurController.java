package com.bibliotech.utilisateur;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/utilisateurs")
@RequiredArgsConstructor
public class UtilisateurController {

    private final UtilisateurService service;
    private final UtilisateurRepository utilisateurRepository;

    // ── ADMIN endpoints ───────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UtilisateurDTO> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UtilisateurDTO findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UtilisateurDTO> create(
            @Valid @RequestBody CreateUtilisateurRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public UtilisateurDTO toggleActif(@PathVariable Long id) {
        return service.toggleActif(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── USER endpoint — own profile ───────────────────────────

    @GetMapping("/me")
    public ResponseEntity<UtilisateurDTO> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        return utilisateurRepository.findByEmail(userDetails.getUsername())
                .map(service::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest req) {
        service.changePassword(userDetails.getUsername(), req);
        return ResponseEntity.noContent().build();
    }
}