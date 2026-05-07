package com.bibliotech.acces;

import com.bibliotech.catalogue.document.Document;
import com.bibliotech.utilisateur.Utilisateur;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "acces", indexes = {
        @Index(name = "idx_acces_user", columnList = "utilisateur_id"),
        @Index(name = "idx_acces_doc",  columnList = "document_id")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Acces {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "date_acces", nullable = false)
    private LocalDateTime dateAcces;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_acces", nullable = false, length = 30)
    private TypeAcces typeAcces;

    @Column(name = "adresse_ip", length = 45)
    private String adresseIp;

    @PrePersist
    public void prePersist() {
        this.dateAcces = LocalDateTime.now();
    }
}