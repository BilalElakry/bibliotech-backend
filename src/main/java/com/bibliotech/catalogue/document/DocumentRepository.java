package com.bibliotech.catalogue.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByIsbn(String isbn);
    boolean existsByIsbn(String isbn);
    List<Document> findByCategorieId(Long categorieId);

    @Query("""
        SELECT d FROM Document d
        WHERE LOWER(d.titre)  LIKE LOWER(CONCAT('%',:q,'%'))
           OR LOWER(d.auteur) LIKE LOWER(CONCAT('%',:q,'%'))
           OR LOWER(d.isbn)   LIKE LOWER(CONCAT('%',:q,'%'))
        """)
    List<Document> search(@Param("q") String query);
}