package com.bibliotech.utilisateur;

import jakarta.validation.constraints.*;

public record CreateUtilisateurRequest(
        @NotBlank String nom,
        @NotBlank String prenom,
        @Email @NotBlank String email,
        @Size(min = 8) @NotBlank String motDePasse
) {}
