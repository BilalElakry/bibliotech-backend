package com.bibliotech.acces;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AccesRepository extends JpaRepository<Acces, Long> {

    List<Acces> findByUtilisateurIdOrderByDateAccesDesc(Long utilisateurId);
    List<Acces> findByDocumentIdOrderByDateAccesDesc(Long documentId);
    long countByUtilisateurIdAndTypeAcces(Long utilisateurId, TypeAcces type);

    // Stats
    long countByTypeAcces(TypeAcces type);
    List<Acces> findTop10ByOrderByDateAccesDesc();

    // Export — eager fetch to avoid LazyInitializationException
    @Query("SELECT a FROM Acces a "
            + "JOIN FETCH a.document "
            + "JOIN FETCH a.utilisateur")
    List<Acces> findAllWithDetails();
}