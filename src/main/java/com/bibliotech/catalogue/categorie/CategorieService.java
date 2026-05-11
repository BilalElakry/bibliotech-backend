package com.bibliotech.catalogue.categorie;

import com.bibliotech.audit.*;
import com.bibliotech.catalogue.document.DocumentRepository;
import com.bibliotech.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategorieService {

    private final CategorieRepository repo;
    private final AuditService auditService;
    private final DocumentRepository documentRepo;

    @Transactional(readOnly = true)
    public List<CategorieDTO> findAll() {
        return repo.findAll().stream()
                .map(c -> new CategorieDTO(
                        c.getId(), c.getNom(), c.getDescription()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CategorieDTO findById(Long id) {
        Categorie c = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Catégorie introuvable : " + id));
        return new CategorieDTO(c.getId(), c.getNom(), c.getDescription());
    }

    @Transactional
    public CategorieDTO create(CategorieRequest req) {
        if (repo.existsByNom(req.nom()))
            throw new BusinessException(
                    "Catégorie déjà existante : " + req.nom());
        Categorie saved = repo.save(Categorie.builder()
                .nom(req.nom()).description(req.description()).build());
        auditService.log(AuditAction.CATEGORY_CREATE, "admin",
                "Catégorie créée: \"" + saved.getNom() + "\"", "");
        return new CategorieDTO(
                saved.getId(), saved.getNom(), saved.getDescription());
    }

    @Transactional
    public CategorieDTO update(Long id, CategorieRequest req) {
        Categorie c = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Catégorie introuvable : " + id));
        String ancienNom = c.getNom();
        c.setNom(req.nom());
        c.setDescription(req.description());
        Categorie saved = repo.save(c);
        auditService.log(AuditAction.CATEGORY_UPDATE, "admin",
                "Catégorie: \"" + ancienNom
                        + "\" → \"" + saved.getNom() + "\"", "");
        return new CategorieDTO(
                saved.getId(), saved.getNom(), saved.getDescription());
    }

    @Transactional
    public void delete(Long id) {
        Categorie c = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Catégorie introuvable : " + id));

        // Check if any documents are linked to this category
        if (documentRepo.existsByCategorieId(id)) {
            throw new BusinessException(
                    "Impossible de supprimer la catégorie \""
                            + c.getNom()
                            + "\" : elle contient des documents. "
                            + "Supprimez ou déplacez les documents d'abord.");
        }

        String nom = c.getNom();
        repo.deleteById(id);
        auditService.log(AuditAction.CATEGORY_DELETE, "admin",
                "Catégorie supprimée: \"" + nom + "\"", "");
    }
}