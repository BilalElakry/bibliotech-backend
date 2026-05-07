package com.bibliotech.catalogue.document;

import com.bibliotech.catalogue.categorie.Categorie;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "documents", indexes = {
        @Index(name = "idx_isbn", columnList = "isbn", unique = true),
        @Index(name = "idx_titre", columnList = "titre"),
        @Index(name = "idx_auteur", columnList = "auteur")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String titre;

    @Column(nullable = false, length = 300)
    private String auteur;

    @Column(nullable = false, unique = true, length = 20)
    private String isbn;

    @Column(name = "annee_publication")
    private int anneePublication;

    @Column(length = 200)
    private String editeur;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "couverture_url", length = 500)
    private String couvertureUrl;

    @Column(name = "fichier_pdf", length = 500)
    private String fichierPdf;

    @Column(name = "taille_octets")
    private Long tailleOctets;

    @Column(name = "nombre_telechargements", nullable = false)
    @Builder.Default
    private int nombreTelechargements = 0;

    @Column(name = "date_ajout")
    private LocalDate dateAjout;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorie_id", nullable = false)
    private Categorie categorie;

    @PrePersist
    public void prePersist() {
        this.dateAjout = LocalDate.now();
    }
}