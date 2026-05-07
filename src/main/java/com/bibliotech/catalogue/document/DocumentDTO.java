package com.bibliotech.catalogue.document;

import com.bibliotech.catalogue.categorie.CategorieDTO;
import java.time.LocalDate;

public record DocumentDTO(
        Long id,
        String titre,
        String auteur,
        String isbn,
        int anneePublication,
        String editeur,
        String description,
        String couvertureUrl,
        boolean hasPdf,
        Long tailleOctets,
        int nombreTelechargements,
        LocalDate dateAjout,
        CategorieDTO categorie
) {}