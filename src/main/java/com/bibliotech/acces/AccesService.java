package com.bibliotech.acces;

import com.bibliotech.catalogue.document.*;
import com.bibliotech.exception.ResourceNotFoundException;
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

    @Transactional
    public byte[] telecharger(Long documentId, HttpServletRequest request) {
        return acceder(documentId, TypeAcces.TELECHARGEMENT, request);
    }

    @Transactional
    public byte[] lireEnLigne(Long documentId, HttpServletRequest request) {
        return acceder(documentId, TypeAcces.LECTURE_EN_LIGNE, request);
    }

    private byte[] acceder(Long documentId, TypeAcces type, HttpServletRequest request) {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        Utilisateur utilisateur = utilisateurRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document introuvable : " + documentId));

        // Log access
        Acces acces = Acces.builder()
                .utilisateur(utilisateur)
                .document(document)
                .typeAcces(type)
                .adresseIp(extractIp(request))
                .build();
        accesRepo.save(acces);

        // Increment counter only for downloads
        if (type == TypeAcces.TELECHARGEMENT) {
            documentService.incrementerTelechargements(documentId);
        }

        log.info("Acces [{}] document '{}' par '{}'",
                type, document.getTitre(), email);

        return documentService.getPdfBytes(documentId);
    }

    @Transactional(readOnly = true)
    public List<AccesDTO> getMonHistorique() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        Utilisateur u = utilisateurRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        return accesRepo.findByUtilisateurIdOrderByDateAccesDesc(u.getId())
                .stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<AccesDTO> getHistoriqueByUser(Long userId) {
        return accesRepo.findByUtilisateurIdOrderByDateAccesDesc(userId)
                .stream().map(this::toDTO).toList();
    }

    private String extractIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        return (xForwardedFor != null) ? xForwardedFor.split(",")[0].trim()
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