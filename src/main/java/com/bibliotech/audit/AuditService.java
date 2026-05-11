package com.bibliotech.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repo;

    @Async
    public void log(String action, String utilisateur,
                    String details, String ip) {
        save(action, utilisateur, details, ip, AuditLevel.INFO);
    }

    @Async
    public void warn(String action, String utilisateur,
                     String details, String ip) {
        save(action, utilisateur, details, ip, AuditLevel.WARNING);
    }

    @Async
    public void error(String action, String utilisateur,
                      String details, String ip) {
        save(action, utilisateur, details, ip, AuditLevel.ERROR);
    }

    private void save(String action, String utilisateur,
                      String details, String ip, AuditLevel niveau) {
        try {
            repo.save(AuditLog.builder()
                    .action(action)
                    .utilisateur(utilisateur != null
                            ? utilisateur : "anonymous")
                    .details(details)
                    .adresseIp(ip != null ? ip : "unknown")
                    .niveau(niveau)
                    .build());
        } catch (Exception e) {
            log.error("Audit log failed: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> findAll(Pageable pageable) {
        return repo.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> findByUser(String email, Pageable pageable) {
        return repo.findByUtilisateurOrderByDateDesc(email, pageable);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> findRecent() {
        return repo.findTop20ByOrderByDateDesc();
    }

    @Transactional(readOnly = true)
    public List<AuditLog> findErrors() {
        return repo.findByNiveauOrderByDateDesc(AuditLevel.ERROR);
    }
}