package com.bibliotech.security;

import com.bibliotech.utilisateur.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return utilisateurRepository.findByEmail(email)
                .map(u -> new User(
                        u.getEmail(),
                        u.getMotDePasseHash(),
                        u.isActif(),
                        true, true, true,
                        List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name()))
                ))
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé : " + email));
    }
}