package com.bibliotech.auth;

import com.bibliotech.exception.BusinessException;
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

    public AuthResponse login(AuthRequest req) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password()));

        var userDetails = userDetailsService.loadUserByUsername(req.email());
        String token = jwtUtils.generate(userDetails);

        Utilisateur u = utilisateurRepository.findByEmail(req.email()).orElseThrow();

        return new AuthResponse(
                token,
                u.getRole().name(),
                u.getNom() + " " + u.getPrenom(),
                u.getEmail()
        );
    }

    @Transactional
    public UtilisateurDTO register(CreateUtilisateurRequest req) {
        if (utilisateurRepository.existsByEmail(req.email()))
            throw new BusinessException("Email déjà utilisé : " + req.email());

        Utilisateur u = Utilisateur.builder()
                .nom(req.nom())
                .prenom(req.prenom())
                .email(req.email())
                .motDePasseHash(passwordEncoder.encode(req.motDePasse()))
                .role(Role.UTILISATEUR)
                .actif(true)
                .dateInscription(LocalDate.now())
                .build();

        return utilisateurService.toDTO(utilisateurRepository.save(u));
    }
}