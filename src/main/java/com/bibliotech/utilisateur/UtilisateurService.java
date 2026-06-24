package com.bibliotech.utilisateur;

import com.bibliotech.audit.*;
import com.bibliotech.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private final UtilisateurRepository repo;
    private final PasswordEncoder encoder;
    private final AuditService auditService;
    @Transactional(readOnly = true)
    public List<UtilisateurDTO> findAll() {
        return repo.findAll().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public UtilisateurDTO findById(Long id) {
        return toDTO(getOrThrow(id));
    }

    @Transactional
    public UtilisateurDTO create(CreateUtilisateurRequest req) {
        if (repo.existsByEmail(req.email()))
            throw new BusinessException(
                    "Email déjà utilisé : " + req.email());

        Utilisateur u = Utilisateur.builder()
                .nom(req.nom()).prenom(req.prenom())
                .email(req.email())
                .motDePasseHash(encoder.encode(req.motDePasse()))
                .role(Role.UTILISATEUR).actif(true).build();

        UtilisateurDTO saved = toDTO(repo.save(u));
        auditService.log(AuditAction.USER_CREATE, "admin",
                "Compte créé pour: " + req.email(), "");
        return saved;
    }

    @Transactional
    public UtilisateurDTO toggleActif(Long id) {
        Utilisateur u = getOrThrow(id);
        boolean wasActif = u.isActif();
        u.setActif(!wasActif);
        UtilisateurDTO saved = toDTO(repo.save(u));

        auditService.log(AuditAction.USER_TOGGLE, "admin",
                "Compte " + u.getEmail() + " "
                        + (wasActif ? "désactivé" : "activé"), "");


        return saved;
    }

    @Transactional
    public void delete(Long id) {
        Utilisateur u = getOrThrow(id);
        String email = u.getEmail();
        repo.deleteById(id);
        auditService.log(AuditAction.USER_DELETE, "admin",
                "Compte supprimé: " + email, "");
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest req) {
        Utilisateur u = repo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur introuvable : " + email));
        if (!encoder.matches(req.ancienMotDePasse(), u.getMotDePasseHash()))
            throw new BusinessException("Ancien mot de passe incorrect");
        u.setMotDePasseHash(encoder.encode(req.nouveauMotDePasse()));
        repo.save(u);

        auditService.log(AuditAction.PASSWORD_CHANGE, email,
                "Mot de passe modifié", "");

    }

    private Utilisateur getOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur introuvable : " + id));
    }

    public UtilisateurDTO toDTO(Utilisateur u) {
        return new UtilisateurDTO(u.getId(), u.getNom(), u.getPrenom(),
                u.getEmail(), u.getRole(), u.isActif(),
                u.getDateInscription());
    }
}