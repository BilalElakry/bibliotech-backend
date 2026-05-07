// ChatbotController.java
package com.bibliotech.chatbot;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/message")
    public ResponseEntity<ChatResponse> message(@Valid @RequestBody ChatRequest req) {
        return ResponseEntity.ok(
                chatbotService.chat(req.message(), req.sessionId()));
    }

    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Void> clearSession(@PathVariable String sessionId) {
        chatbotService.clearSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}