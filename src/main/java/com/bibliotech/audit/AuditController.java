package com.bibliotech.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<Page<AuditLog>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("date").descending());
        return ResponseEntity.ok(auditService.findAll(pageable));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<AuditLog>> recent() {
        return ResponseEntity.ok(auditService.findRecent());
    }

    @GetMapping("/errors")
    public ResponseEntity<List<AuditLog>> errors() {
        return ResponseEntity.ok(auditService.findErrors());
    }

    @GetMapping("/utilisateurs/{email}")
    public ResponseEntity<Page<AuditLog>> byUser(
            @PathVariable String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("date").descending());
        return ResponseEntity.ok(
                auditService.findByUser(email, pageable));
    }
}