package com.bibliotech.auth;

import com.bibliotech.utilisateur.CreateUtilisateurRequest;
import com.bibliotech.utilisateur.UtilisateurDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }
    @PostMapping("/register")
    public ResponseEntity<UtilisateurDTO> register(
            @Valid @RequestBody CreateUtilisateurRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(req));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // JWT is stateless — logout is handled client-side
        // Server-side we just return 200
        return ResponseEntity.noContent().build();
    }
}