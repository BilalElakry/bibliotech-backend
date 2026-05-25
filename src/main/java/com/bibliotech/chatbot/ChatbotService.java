package com.bibliotech.chatbot;

import com.bibliotech.catalogue.document.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final ChatClient chatClient;
    private final DocumentService documentService;
    private final ChatMessageRepository chatMessageRepository; // ✅ Injection de la DB

    private static final String SYSTEM_PROMPT = """
        Tu es ScribeAI, un assistant académique spécialisé en recherche documentaire.
        Voici les documents disponibles dans notre catalogue :
        
        %s
        
        Réponds **toujours** en français, de manière courte et utile en te basant uniquement sur ces documents.
        Si le document demandé n'est pas dans le catalogue, dis-le honnêtement.
        """;

    @Transactional
    public ChatResponse chat(String userMessage, String sessionId) {
        String sid = (sessionId != null && !sessionId.isBlank())
                ? sessionId : UUID.randomUUID().toString();

        log.info("🚀 Début chat – session={} | userMessage='{}'", sid, truncate(userMessage, 80));

        // 1. Sauvegarder le message de l'utilisateur en base
        ChatMessage dbUserMsg = ChatMessage.builder()
                .sessionId(sid)
                .role("USER")
                .content(userMessage)
                .build();
        chatMessageRepository.save(dbUserMsg);

        // 2. Contexte catalogue
        String catalogueContext = buildCatalogueContext();

        // 3. Récupérer l'historique complet depuis MySQL
        List<ChatMessage> dbHistory = chatMessageRepository.findBySessionIdOrderByTimestampAsc(sid);
        log.info("🗂️ Historique DB récupéré – nombre de messages = {}", dbHistory.size());

        // 4. Construction du prompt IA
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(String.format(SYSTEM_PROMPT, catalogueContext)));

        // Convertir les entités DB en messages compris par Spring AI
        for (ChatMessage msg : dbHistory) {
            if ("USER".equals(msg.getRole())) {
                messages.add(new UserMessage(msg.getContent()));
            } else if ("AI".equals(msg.getRole())) {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }

        int totalChars = 0;
        for (Message m : messages) {
            if (m.getText() != null) totalChars += m.getText().length();
        }
        log.info("✉️ Prompt complet – {} messages, {} caractères au total", messages.size(), totalChars);

        // 5. Appel à l'IA
        String reply;
        try {
            long start = System.currentTimeMillis();
            reply = chatClient.prompt(new Prompt(messages)).call().content();
            long duration = System.currentTimeMillis() - start;
            log.info("🤖 Réponse IA reçue en {} ms – longueur={}", duration, (reply != null ? reply.length() : 0));
        } catch (Exception e) {
            log.error("❌ Erreur ChatClient pour la session [{}]", sid, e);
            throw new RuntimeException("Erreur de communication avec l'assistant IA", e);
        }

        if (reply == null || reply.isBlank()) {
            reply = "Je n’ai pas pu générer de réponse. Veuillez reformuler votre question.";
        }

        // 6. Sauvegarder la réponse de l'IA en DB
        ChatMessage dbAiMsg = ChatMessage.builder()
                .sessionId(sid)
                .role("AI")
                .content(reply)
                .build();
        chatMessageRepository.save(dbAiMsg);

        return new ChatResponse(reply, sid);
    }

    public List<ChatMessage> getHistory(String sessionId) {
        return chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
    }

    private String buildCatalogueContext() {
        var documents = documentService.findAllForRag();
        if (documents.isEmpty()) return "Aucun document disponible.";

        StringBuilder sb = new StringBuilder();
        int maxDocs = Math.min(documents.size(), 5);
        for (int i = 0; i < maxDocs; i++) {
            var d = documents.get(i);
            sb.append(String.format(
                    "- \"%s\" de %s (%d) — Catégorie: %s — ISBN: %s\n",
                    d.getTitre(), d.getAuteur(), d.getAnneePublication(),
                    d.getCategorie() != null ? d.getCategorie().getNom() : "N/A",
                    d.getIsbn()));
        }
        String context = sb.toString();
        return context.length() > 800 ? context.substring(0, 800) : context;
    }

    @Transactional
    public void clearSession(String sessionId) {
        chatMessageRepository.deleteBySessionId(sessionId);
        log.info("🧽 Session effacée en base de données : {}", sessionId);
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "null";
        return str.length() <= maxLen ? str : str.substring(0, maxLen) + "...";
    }
}