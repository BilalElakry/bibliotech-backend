package com.bibliotech.catalogue.categorie;

import jakarta.validation.constraints.NotBlank;

public record CategorieRequest(
        @NotBlank String nom,
        String description
) {}
