// DocumentStatDTO.java
package com.bibliotech.stats;

public record DocumentStatDTO(
        Long id,
        String titre,
        String auteur,
        int nombreTelechargements
) {}