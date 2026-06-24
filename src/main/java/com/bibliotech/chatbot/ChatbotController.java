package com.bibliotech.chatbot;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    /**
     * Envoyer un message.
     * Principal est garanti non-null : "/api/chatbot/**" est protégé par .authenticated()
     * dans SecurityConfig.
     */
    @PostMapping("/message")
    public ResponseEntity<ChatResponse> message(
            @Valid @RequestBody ChatRequest req,
            Principal principal) {

        return ResponseEntity.ok(
                chatbotService.chat(req.message(), req.sessionId(), principal.getName()));
    }

    /**
     * Récupérer l'historique d'une session.
     * Filtré par (username AND sessionId) → 100% privé.
     */
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<ChatMessage>> getHistory(
            @PathVariable String sessionId,
            Principal principal) {

        return ResponseEntity.ok(
                chatbotService.getHistory(sessionId, principal.getName()));
    }

    /**
     * Effacer une session.
     * Un utilisateur ne peut supprimer que ses propres sessions.
     */
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Void> clearSession(
            @PathVariable String sessionId,
            Principal principal) {

        chatbotService.clearSession(sessionId, principal.getName());
        return ResponseEntity.noContent().build();
    }
}