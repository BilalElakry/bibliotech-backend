package com.bibliotech.catalogue.categorie;

import com.bibliotech.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategorieService {

    private final CategorieRepository repo;

    @Transactional(readOnly = true)
    public List<CategorieDTO> findAll() {
        return repo.findAll().stream()
                .map(c -> new CategorieDTO(c.getId(), c.getNom(), c.getDescription()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CategorieDTO findById(Long id) {
        Categorie c = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable : " + id));
        return new CategorieDTO(c.getId(), c.getNom(), c.getDescription());
    }

    @Transactional
    public CategorieDTO create(CategorieRequest req) {
        if (repo.existsByNom(req.nom()))
            throw new BusinessException("Catégorie déjà existante : " + req.nom());
        Categorie saved = repo.save(
                Categorie.builder().nom(req.nom()).description(req.description()).build());
        return new CategorieDTO(saved.getId(), saved.getNom(), saved.getDescription());
    }

    @Transactional
    public CategorieDTO update(Long id, CategorieRequest req) {
        Categorie c = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable : " + id));
        c.setNom(req.nom());
        c.setDescription(req.description());
        Categorie saved = repo.save(c);
        return new CategorieDTO(saved.getId(), saved.getNom(), saved.getDescription());
    }

    @Transactional
    public void delete(Long id) {
        repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable : " + id));
        repo.deleteById(id);
    }
}