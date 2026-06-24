package com.bibliotech.chatbot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * Historique d'une session — filtré par utilisateur ET sessionId.
     * Sans le filtre username, un utilisateur B pourrait accéder
     * à la session d'un utilisateur A s'il connaît son UUID.
     */
    List<ChatMessage> findByUsernameAndSessionIdOrderByTimestampAsc(
            String username, String sessionId);

    /**
     * Suppression cloisonnée : on ne peut effacer que SA propre session.
     */
    void deleteByUsernameAndSessionId(String username, String sessionId);

    // ── PLUS utilisées nulle part, gardées pour migration / admin ──
    List<ChatMessage> findBySessionIdOrderByTimestampAsc(String sessionId);
    void deleteBySessionId(String sessionId);
}