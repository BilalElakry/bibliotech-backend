package com.bibliotech.audit;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByUtilisateurOrderByDateDesc(
            String utilisateur, Pageable pageable);

    List<AuditLog> findByNiveauOrderByDateDesc(AuditLevel niveau);

    List<AuditLog> findTop20ByOrderByDateDesc();
}