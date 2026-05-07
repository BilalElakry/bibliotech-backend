package com.bibliotech.catalogue.document;

import com.bibliotech.catalogue.categorie.*;
import com.bibliotech.exception.*;
import com.bibliotech.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // ── QUERIES ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<DocumentDTO> findAll() {
        return documentRepo.findAll().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentDTO> search(String query) {
        return documentRepo.search(query).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentDTO> findByCategorie(Long categorieId) {
        return documentRepo.findByCategorieId(categorieId).stream()
                .map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public DocumentDTO findById(Long id) {
        return toDTO(getOrThrow(id));
    }

    // ── COMMANDS ──────────────────────────────────────────────

    @Transactional
    public DocumentDTO create(DocumentRequest req, MultipartFile pdfFile) {
        if (documentRepo.existsByIsbn(req.isbn()))
            throw new BusinessException("ISBN déjà enregistré : " + req.isbn());

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

        return toDTO(documentRepo.save(doc));
    }

    @Transactional
    public DocumentDTO update(Long id, DocumentRequest req, MultipartFile pdfFile) {
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
            if (doc.getFichierPdf() != null) storageService.delete(doc.getFichierPdf());
            String path = storageService.store(pdfFile, doc.getIsbn());
            doc.setFichierPdf(path);
            doc.setTailleOctets(storageService.size(path));
        }

        return toDTO(documentRepo.save(doc));
    }

    @Transactional
    public void delete(Long id) {
        Document doc = getOrThrow(id);
        if (doc.getFichierPdf() != null) storageService.delete(doc.getFichierPdf());
        documentRepo.deleteById(id);
    }

    // ── PDF bytes (used by AccesService) ─────────────────────

    @Transactional(readOnly = true)
    public byte[] getPdfBytes(Long id) {
        Document doc = getOrThrow(id);
        if (doc.getFichierPdf() == null)
            throw new BusinessException("Ce document n'a pas de fichier PDF");
        return storageService.load(doc.getFichierPdf());
    }

    @Transactional
    public void incrementerTelechargements(Long id) {
        Document doc = getOrThrow(id);
        doc.setNombreTelechargements(doc.getNombreTelechargements() + 1);
        documentRepo.save(doc);
    }

    // ── Helpers ───────────────────────────────────────────────

    private Document getOrThrow(Long id) {
        return documentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document introuvable : " + id));
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
                        ? new CategorieDTO(d.getCategorie().getId(),
                        d.getCategorie().getNom(),
                        d.getCategorie().getDescription())
                        : null
        );
    }
}