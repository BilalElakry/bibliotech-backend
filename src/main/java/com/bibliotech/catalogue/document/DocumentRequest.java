package com.bibliotech.catalogue.document;

import jakarta.validation.constraints.*;

public record DocumentRequest(
        @NotBlank String titre,
        @NotBlank String auteur,
        @NotBlank @Pattern(regexp = "\\d{10}|\\d{13}",
                message = "ISBN-10 ou ISBN-13 requis") String isbn,
        @Min(1000) @Max(2100) int anneePublication,
        String editeur,
        String description,
        String couvertureUrl,
        @NotNull Long categorieId
) {}