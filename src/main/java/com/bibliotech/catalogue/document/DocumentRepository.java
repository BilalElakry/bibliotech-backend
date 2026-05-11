package com.bibliotech.catalogue.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByIsbn(String isbn);

    boolean existsByCategorieId(Long categorieId);

    boolean existsByIsbn(String isbn);

    // ── Stats — must match exactly: nombreTelechargements ────────
    List<Document> findTop5ByOrderByNombreTelechargementsDesc();

    // ── Category filter ───────────────────────────────────────────
    Page<Document> findByCategorieId(Long categorieId, Pageable pageable);

    // ── Search (non-paginated for chatbot RAG) ────────────────────
    @Query("""
        SELECT d FROM Document d
        WHERE LOWER(d.titre)  LIKE LOWER(CONCAT('%',:q,'%'))
           OR LOWER(d.auteur) LIKE LOWER(CONCAT('%',:q,'%'))
           OR LOWER(d.isbn)   LIKE LOWER(CONCAT('%',:q,'%'))
        """)
    List<Document> search(@Param("q") String query);

    // ── Paginated search ──────────────────────────────────────────
    @Query("""
        SELECT d FROM Document d
        WHERE LOWER(d.titre)  LIKE LOWER(CONCAT('%',:q,'%'))
           OR LOWER(d.auteur) LIKE LOWER(CONCAT('%',:q,'%'))
           OR LOWER(d.isbn)   LIKE LOWER(CONCAT('%',:q,'%'))
        """)
    Page<Document> searchPaginated(@Param("q") String q, Pageable pageable);

    // ── Advanced search ───────────────────────────────────────────
    @Query("""
        SELECT d FROM Document d
        WHERE (:q IS NULL OR
               LOWER(d.titre)  LIKE LOWER(CONCAT('%',:q,'%')) OR
               LOWER(d.auteur) LIKE LOWER(CONCAT('%',:q,'%')))
          AND (:categorieId IS NULL OR d.categorie.id = :categorieId)
          AND (:anneeMin IS NULL OR d.anneePublication >= :anneeMin)
          AND (:anneeMax IS NULL OR d.anneePublication <= :anneeMax)
        """)
    Page<Document> advancedSearch(
            @Param("q") String q,
            @Param("categorieId") Long categorieId,
            @Param("anneeMin") Integer anneeMin,
            @Param("anneeMax") Integer anneeMax,
            Pageable pageable);
}