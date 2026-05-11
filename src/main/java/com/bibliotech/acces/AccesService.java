package com.bibliotech.acces;

import com.bibliotech.audit.*;
import com.bibliotech.catalogue.document.*;
import com.bibliotech.exception.ResourceNotFoundException;
import com.bibliotech.notification.EmailService;
import com.bibliotech.utilisateur.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccesService {

    private final AccesRepository accesRepo;
    private final DocumentService documentService;
    private final DocumentRepository documentRepo;
    private final UtilisateurRepository utilisateurRepo;
    private final AuditService auditService;
    private final EmailService emailService;

    @Transactional
    public byte[] telecharger(Long documentId,
                              HttpServletRequest request) {
        byte[] bytes = acceder(documentId,
                TypeAcces.TELECHARGEMENT, request);

        auditService.log(AuditAction.DOWNLOAD, currentUser(),
                "Téléchargement: \""
                        + getDocumentTitre(documentId) + "\"",
                extractIp(request));

        // ── Send download confirmation email ──────────────────────
        String email = currentUser();
        utilisateurRepo.findByEmail(email).ifPresent(u ->
                documentRepo.findById(documentId).ifPresent(doc ->
                        emailService.sendDownloadConfirmation(
                                email,
                                u.getPrenom(),
                                doc.getTitre(),
                                doc.getAuteur())
                )
        );

        return bytes;
    }

    @Transactional
    public byte[] lireEnLigne(Long documentId,
                              HttpServletRequest request) {
        byte[] bytes = acceder(documentId,
                TypeAcces.LECTURE_EN_LIGNE, request);
        auditService.log(AuditAction.READ_ONLINE, currentUser(),
                "Lecture en ligne: \""
                        + getDocumentTitre(documentId) + "\"",
                extractIp(request));
        return bytes;
    }

    private byte[] acceder(Long documentId, TypeAcces type,
                           HttpServletRequest request) {
        String email = currentUser();

        Utilisateur utilisateur = utilisateurRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur introuvable"));

        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document introuvable : " + documentId));

        accesRepo.save(Acces.builder()
                .utilisateur(utilisateur)
                .document(document)
                .typeAcces(type)
                .adresseIp(extractIp(request))
                .build());

        if (type == TypeAcces.TELECHARGEMENT)
            documentService.incrementerTelechargements(documentId);

        return documentService.getPdfBytes(documentId);
    }

    @Transactional(readOnly = true)
    public List<AccesDTO> getMonHistorique() {
        String email = currentUser();
        Utilisateur u = utilisateurRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur introuvable"));
        return accesRepo
                .findByUtilisateurIdOrderByDateAccesDesc(u.getId())
                .stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<AccesDTO> getHistoriqueByUser(Long userId) {
        return accesRepo
                .findByUtilisateurIdOrderByDateAccesDesc(userId)
                .stream().map(this::toDTO).toList();
    }

    private String currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }

    private String getDocumentTitre(Long documentId) {
        return documentRepo.findById(documentId)
                .map(Document::getTitre)
                .orElse("Document #" + documentId);
    }

    private String extractIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return xff != null
                ? xff.split(",")[0].trim()
                : request.getRemoteAddr();
    }

    private AccesDTO toDTO(Acces a) {
        return new AccesDTO(
                a.getId(),
                a.getDocument().getId(),
                a.getDocument().getTitre(),
                a.getDocument().getAuteur(),
                a.getTypeAcces(),
                a.getDateAcces()
        );
    }
}