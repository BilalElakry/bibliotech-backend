package com.bibliotech.audit;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_utilisateur", columnList = "utilisateur"),
        @Index(name = "idx_audit_action",      columnList = "action"),
        @Index(name = "idx_audit_date",        columnList = "date")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(nullable = false, length = 150)
    private String utilisateur;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "adresse_ip", length = 45)
    private String adresseIp;

    @Column(nullable = false)
    private LocalDateTime date;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AuditLevel niveau;

    @PrePersist
    public void prePersist() {
        this.date = LocalDateTime.now();
    }
}