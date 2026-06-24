package com.bibliotech.chatbot;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "chat_history",
        indexes = {
                // Index composite pour les requêtes filtrées par (username, sessionId)
                @Index(name = "idx_chat_user_session",
                        columnList = "username, sessionId")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Email/username de l'utilisateur connecté — null = impossible vu le @PreAuthorize */
    @Column(nullable = false, length = 255)
    private String username;

    /** UUID côté client — identifie la "fenêtre" de conversation */
    @Column(nullable = false)
    private String sessionId;

    @Column(nullable = false, length = 4)
    private String role; // "USER" | "AI"

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}