package com.bibliotech.acces;

import java.time.LocalDateTime;

public record AccesDTO(
        Long id,
        Long documentId,
        String documentTitre,
        String documentAuteur,
        TypeAcces typeAcces,
        LocalDateTime dateAcces
) {}