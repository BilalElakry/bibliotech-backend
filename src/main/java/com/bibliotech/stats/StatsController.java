package com.bibliotech.stats;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class StatsController {

    private final StatsService statsService;

    @GetMapping
    public ResponseEntity<StatsDTO> getStats() {
        return ResponseEntity.ok(statsService.getStats());
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv() {
        byte[] csv = statsService.exportCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"rapport.csv\"")
                .contentType(MediaType
                        .parseMediaType("text/csv"))
                .body(csv);
    }
}