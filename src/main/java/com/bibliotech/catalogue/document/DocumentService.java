package com.bibliotech.catalogue.document;

import com.bibliotech.audit.*;
import com.bibliotech.catalogue.categorie.*;
import com.bibliotech.exception.*;
import com.bibliotech.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepo;
    private final CategorieRepository categorieRepo;
    private final StorageService storageService;
    private final AuditService auditService;

    // ── QUERIES ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Document> findAllForRag() {
        return documentRepo.findAll();
    }

    @Transactional(readOnly = true)
    public Page<DocumentDTO> findPaginated(String q, Long categorieId,
                                           Pageable pageable) {
        Page<Document> page;
        if (q != null && !q.isBlank()) {
            page = documentRepo.searchPaginated(q, pageable);
        } else if (categorieId != null) {
            page = documentRepo.findByCategorieId(categorieId, pageable);
        } else {
            page = documentRepo.findAll(pageable);
        }
        return page.map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<DocumentDTO> advancedSearch(String q, Long categorieId,
                                            Integer anneeMin, Integer anneeMax,
                                            Pageable pageable) {
        return documentRepo
                .advancedSearch(q, categorieId, anneeMin, anneeMax, pageable)
                .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public DocumentDTO findById(Long id) {
        return toDTO(getOrThrow(id));
    }

    // ── COMMANDS ──────────────────────────────────────────────────

    @Transactional
    public DocumentDTO create(DocumentRequest req, MultipartFile pdfFile) {
        if (documentRepo.existsByIsbn(req.isbn()))
            throw new BusinessException(
                    "ISBN déjà enregistré : " + req.isbn());

        Categorie cat = categorieRepo.findById(req.categorieId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Catégorie introuvable : " + req.categorieId()));

        Document doc = Document.builder()
                .titre(req.titre())
                .auteur(req.auteur())
                .isbn(req.isbn())
                .anneePublication(req.anneePublication())
                .editeur(req.editeur())
                .description(req.description())
                .couvertureUrl(req.couvertureUrl())
                .categorie(cat)
                .build();

        if (pdfFile != null && !pdfFile.isEmpty()) {
            String path = storageService.store(pdfFile, req.isbn());
            doc.setFichierPdf(path);
            doc.setTailleOctets(storageService.size(path));
        }

        Document saved = documentRepo.save(doc);

        auditService.log(AuditAction.UPLOAD, currentUser(),
                "Document ajouté: \"" + saved.getTitre()
                        + "\" [ISBN: " + saved.getIsbn() + "]", "");

        return toDTO(saved);
    }

    @Transactional
    public DocumentDTO update(Long id, DocumentRequest req,
                              MultipartFile pdfFile) {
        Document doc = getOrThrow(id);
        Categorie cat = categorieRepo.findById(req.categorieId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Catégorie introuvable : " + req.categorieId()));

        doc.setTitre(req.titre());
        doc.setAuteur(req.auteur());
        doc.setAnneePublication(req.anneePublication());
        doc.setEditeur(req.editeur());
        doc.setDescription(req.description());
        doc.setCouvertureUrl(req.couvertureUrl());
        doc.setCategorie(cat);

        if (pdfFile != null && !pdfFile.isEmpty()) {
            if (doc.getFichierPdf() != null)
                storageService.delete(doc.getFichierPdf());
            String path = storageService.store(pdfFile, doc.getIsbn());
            doc.setFichierPdf(path);
            doc.setTailleOctets(storageService.size(path));
        }

        Document saved = documentRepo.save(doc);

        auditService.log(AuditAction.UPDATE, currentUser(),
                "Document modifié: \"" + saved.getTitre()
                        + "\" [id: " + id + "]", "");

        return toDTO(saved);
    }

    @Transactional
    public void delete(Long id) {
        Document doc = getOrThrow(id);
        String titre = doc.getTitre();
        if (doc.getFichierPdf() != null)
            storageService.delete(doc.getFichierPdf());
        documentRepo.deleteById(id);

        auditService.log(AuditAction.DELETE, currentUser(),
                "Document supprimé: \"" + titre + "\" [id: " + id + "]", "");
    }

    // ── PDF ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] getPdfBytes(Long id) {
        Document doc = getOrThrow(id);
        if (doc.getFichierPdf() == null)
            throw new BusinessException(
                    "Ce document n'a pas de fichier PDF");
        return storageService.load(doc.getFichierPdf());
    }

    @Transactional
    public void incrementerTelechargements(Long id) {
        Document doc = getOrThrow(id);
        doc.setNombreTelechargements(
                doc.getNombreTelechargements() + 1);
        documentRepo.save(doc);
    }

    // ── Helpers ───────────────────────────────────────────────────

    private Document getOrThrow(Long id) {
        return documentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document introuvable : " + id));
    }

    private String currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }

    public DocumentDTO toDTO(Document d) {
        return new DocumentDTO(
                d.getId(), d.getTitre(), d.getAuteur(), d.getIsbn(),
                d.getAnneePublication(), d.getEditeur(), d.getDescription(),
                d.getCouvertureUrl(),
                d.getFichierPdf() != null,
                d.getTailleOctets(),
                d.getNombreTelechargements(),
                d.getDateAjout(),
                d.getCategorie() != null
                        ? new CategorieDTO(
                        d.getCategorie().getId(),
                        d.getCategorie().getNom(),
                        d.getCategorie().getDescription())
                        : null
        );
    }
}