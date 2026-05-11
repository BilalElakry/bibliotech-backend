package com.bibliotech.auth;

import com.bibliotech.audit.*;
import com.bibliotech.exception.BusinessException;
import com.bibliotech.notification.EmailService;
import com.bibliotech.security.JwtUtils;
import com.bibliotech.utilisateur.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtils jwtUtils;
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final UtilisateurService utilisateurService;
    private final AuditService auditService;
    private final EmailService emailService;
    public AuthResponse login(AuthRequest req) {
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            req.email(), req.password()));
        } catch (BadCredentialsException e) {
            auditService.warn(AuditAction.FAILED_LOGIN, req.email(),
                    "Mot de passe incorrect", "");
            throw e;
        } catch (DisabledException e) {
            auditService.warn(AuditAction.FAILED_LOGIN, req.email(),
                    "Compte désactivé", "");
            throw e;
        }

        var userDetails = userDetailsService
                .loadUserByUsername(req.email());
        String token = jwtUtils.generate(userDetails);
        Utilisateur u = utilisateurRepository
                .findByEmail(req.email()).orElseThrow();

        auditService.log(AuditAction.LOGIN, req.email(),
                "Connexion réussie — rôle: " + u.getRole().name(), "");

        return new AuthResponse(token, u.getRole().name(),
                u.getNom() + " " + u.getPrenom(), u.getEmail());
    }

    @Transactional
    public UtilisateurDTO register(CreateUtilisateurRequest req) {
        if (utilisateurRepository.existsByEmail(req.email()))
            throw new BusinessException(
                    "Email déjà utilisé : " + req.email());

        Utilisateur u = Utilisateur.builder()
                .nom(req.nom()).prenom(req.prenom())
                .email(req.email())
                .motDePasseHash(passwordEncoder.encode(req.motDePasse()))
                .role(Role.UTILISATEUR).actif(true)
                .dateInscription(LocalDate.now())
                .build();

        UtilisateurDTO saved = utilisateurService.toDTO(
                utilisateurRepository.save(u));

        auditService.log(AuditAction.REGISTER, req.email(),
                "Nouveau compte: " + req.prenom() + " " + req.nom(), "");

        // ── Send welcome email ────────────────────────────────────
        emailService.sendWelcome(req.email(), req.prenom());

        return saved;
    }
}