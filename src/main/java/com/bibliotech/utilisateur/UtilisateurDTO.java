package com.bibliotech.utilisateur;

import java.time.LocalDate;

public record UtilisateurDTO(
        Long id,
        String nom,
        String prenom,
        String email,
        Role role,
        boolean actif,
        LocalDate dateInscription
) {}