// AccesRecentDTO.java
package com.bibliotech.stats;

public record AccesRecentDTO(
        String documentTitre,
        String utilisateurEmail,
        String typeAcces,
        String dateAcces
) {}