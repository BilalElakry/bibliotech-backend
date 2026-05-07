package com.bibliotech.acces;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AccesRepository extends JpaRepository<Acces, Long> {
    List<Acces> findByUtilisateurIdOrderByDateAccesDesc(Long utilisateurId);
    List<Acces> findByDocumentIdOrderByDateAccesDesc(Long documentId);
    long countByUtilisateurIdAndTypeAcces(Long utilisateurId, TypeAcces type);
}