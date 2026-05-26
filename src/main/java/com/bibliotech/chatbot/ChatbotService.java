package com.bibliotech.chatbot;

import com.bibliotech.catalogue.document.DocumentService;
import com.bibliotech.rag.RagRetrievalService;
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
    private final ChatMessageRepository chatMessageRepository;
    private final RagRetrievalService ragRetrievalService; // NEW injection

    // Used when RAG returns relevant chunks
    private static final String SYSTEM_PROMPT_RAG = """
        Tu es ScribeAI, un assistant académique spécialisé en recherche documentaire.

        Voici des extraits pertinents issus des documents de la bibliothèque :

        %s

        Réponds **toujours** en français, de manière concise et précise,
        en te basant uniquement sur les extraits fournis.
        Si l'information demandée n'est pas dans les extraits, dis-le clairement.
        Ne fabrique jamais d'informations.
        """;

    // Used when RAG finds no relevant chunks (fallback)
    private static final String SYSTEM_PROMPT_CATALOGUE = """
        Tu es ScribeAI, un assistant académique spécialisé en recherche documentaire.
        Voici les documents disponibles dans notre catalogue :

        %s

        Réponds **toujours** en français, de manière courte et utile,
        en te basant uniquement sur ces documents.
        Si le document demandé n'est pas dans le catalogue, dis-le honnêtement.
        """;

    @Transactional
    public ChatResponse chat(String userMessage, String sessionId) {
        String sid = (sessionId != null && !sessionId.isBlank())
                ? sessionId : UUID.randomUUID().toString();

        log.info("Chat – session={} | query='{}'", sid, truncate(userMessage, 80));

        // 1. Persist user message
        chatMessageRepository.save(ChatMessage.builder()
                .sessionId(sid)
                .role("USER")
                .content(userMessage)
                .build());

        // 2. Build system prompt: try RAG first, fall back to catalogue list
        String ragContext = ragRetrievalService.retrieveContext(userMessage);
        String systemContent;

        if (!ragContext.isBlank()) {
            systemContent = String.format(SYSTEM_PROMPT_RAG, ragContext);
            log.info("RAG context injected ({} chars)", ragContext.length());
        } else {
            log.info("No RAG context — using catalogue fallback");
            systemContent = String.format(SYSTEM_PROMPT_CATALOGUE,
                    buildCatalogueContext());
        }

        // 3. Build conversation with sliding window (last 20 messages)
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemContent));

        List<ChatMessage> history = chatMessageRepository
                .findBySessionIdOrderByTimestampAsc(sid);

        List<ChatMessage> window = history.size() > 20
                ? history.subList(history.size() - 20, history.size())
                : history;

        for (ChatMessage msg : window) {
            if ("USER".equals(msg.getRole()))
                messages.add(new UserMessage(msg.getContent()));
            else if ("AI".equals(msg.getRole()))
                messages.add(new AssistantMessage(msg.getContent()));
        }

        // 4. Call qwen2.5:3b via Ollama (local, free)
        String reply;
        try {
            long t0 = System.currentTimeMillis();
            reply = chatClient.prompt(new Prompt(messages)).call().content();
            log.info("LLM replied in {} ms", System.currentTimeMillis() - t0);
        } catch (Exception e) {
            log.error("LLM error – session [{}]: {}", sid, e.getMessage());
            throw new RuntimeException(
                    "Erreur de communication avec l'assistant IA", e);
        }

        if (reply == null || reply.isBlank())
            reply = "Je n'ai pas pu générer de réponse. Veuillez reformuler.";

        // 5. Persist AI response
        chatMessageRepository.save(ChatMessage.builder()
                .sessionId(sid)
                .role("AI")
                .content(reply)
                .build());

        return new ChatResponse(reply, sid);
    }

    public List<ChatMessage> getHistory(String sessionId) {
        return chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
    }

    @Transactional
    public void clearSession(String sessionId) {
        chatMessageRepository.deleteBySessionId(sessionId);
        log.info("Session cleared: {}", sessionId);
    }

    // ── Fallback: original catalogue listing ─────────────────────
    private String buildCatalogueContext() {
        var documents = documentService.findAllForRag();
        if (documents.isEmpty()) return "Aucun document disponible.";
        StringBuilder sb = new StringBuilder();
        int max = Math.min(documents.size(), 5);
        for (int i = 0; i < max; i++) {
            var d = documents.get(i);
            sb.append(String.format(
                    "- \"%s\" de %s (%d) — Catégorie: %s — ISBN: %s\n",
                    d.getTitre(), d.getAuteur(), d.getAnneePublication(),
                    d.getCategorie() != null ? d.getCategorie().getNom() : "N/A",
                    d.getIsbn()));
        }
        String ctx = sb.toString();
        return ctx.length() > 800 ? ctx.substring(0, 800) : ctx;
    }

    private String truncate(String s, int max) {
        return s == null ? "null"
                : s.length() <= max ? s
                : s.substring(0, max) + "...";
    }
}