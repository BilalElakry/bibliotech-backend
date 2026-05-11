// StatsDTO.java
package com.bibliotech.stats;

import java.util.List;

public record StatsDTO(
        long totalDocuments,
        long totalUtilisateurs,
        long totalTelechargements,
        long totalLectures,
        List<DocumentStatDTO> topDocuments,
        List<AccesRecentDTO> accesRecents
) {}