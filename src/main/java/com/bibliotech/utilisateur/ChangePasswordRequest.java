package com.bibliotech.utilisateur;

import jakarta.validation.constraints.*;

public record ChangePasswordRequest(
        @NotBlank String ancienMotDePasse,
        @NotBlank @Size(min = 8) String nouveauMotDePasse
) {}