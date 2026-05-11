package com.bibliotech.chatbot;

import com.bibliotech.catalogue.document.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final ChatClient chatClient;
    private final DocumentService documentService;

    private final Map<String, List<Message>> sessions =
            new ConcurrentHashMap<>();

    private static final String SYSTEM_PROMPT = """
            Tu es un assistant de bibliothèque numérique.
            Voici les documents disponibles dans notre catalogue :

            %s

            Réponds uniquement en te basant sur ces documents.
            Si le document demandé n'est pas dans le catalogue,
            dis-le clairement.
            Réponds toujours en français, de façon concise.
            """;

    public ChatResponse chat(String userMessage, String sessionId) {
        String sid = (sessionId != null && !sessionId.isBlank())
                ? sessionId : UUID.randomUUID().toString();

        String catalogueContext = buildCatalogueContext();
        List<Message> history =
                sessions.computeIfAbsent(sid, k -> new ArrayList<>());

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(
                String.format(SYSTEM_PROMPT, catalogueContext)));
        messages.addAll(history);
        messages.add(new UserMessage(userMessage));

        String reply = chatClient.call(new Prompt(messages))
                .getResult().getOutput().getContent();

        history.add(new UserMessage(userMessage));
        history.add(new AssistantMessage(reply));

        if (history.size() > 20)
            history.subList(0, history.size() - 20).clear();

        log.info("Chatbot [{}] — '{}'", sid,
                userMessage.substring(0,
                        Math.min(50, userMessage.length())));

        return new ChatResponse(reply, sid);
    }

    private String buildCatalogueContext() {
        var documents = documentService.findAllForRag();
        if (documents.isEmpty()) return "Aucun document disponible.";

        StringBuilder sb = new StringBuilder();
        for (var d : documents) {
            sb.append(String.format(
                    "- \"%s\" de %s (%d) — Catégorie: %s — ISBN: %s\n",
                    d.getTitre(), d.getAuteur(), d.getAnneePublication(),
                    d.getCategorie() != null
                            ? d.getCategorie().getNom() : "N/A",
                    d.getIsbn()));
        }
        return sb.toString();
    }

    public void clearSession(String sessionId) {
        sessions.remove(sessionId);
    }
}