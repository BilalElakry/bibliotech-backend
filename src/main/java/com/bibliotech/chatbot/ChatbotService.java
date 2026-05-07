package com.bibliotech.chatbot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final ChatClient chatClient;

    // In-memory session store
    private final Map<String, List<Message>> sessions = new ConcurrentHashMap<>();

    private static final String SYSTEM_PROMPT = """
            Tu es un assistant de bibliothèque numérique intelligent et serviable.
            Tu aides les utilisateurs à :
            - Trouver des documents dans la bibliothèque par titre, auteur ou thématique
            - Répondre aux questions sur les politiques de la bibliothèque
            - Proposer des recommandations de lecture
            Réponds toujours en français, de façon concise et précise.
            Si tu ne sais pas, dis-le honnêtement.
            """;

    public ChatResponse chat(String userMessage, String sessionId) {
        String sid = (sessionId != null && !sessionId.isBlank())
                ? sessionId : UUID.randomUUID().toString();

        List<Message> history = sessions.computeIfAbsent(sid, k -> new ArrayList<>());

        // Build messages
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYSTEM_PROMPT));
        messages.addAll(history);
        messages.add(new UserMessage(userMessage));

        // Call Ollama
        String reply = chatClient.call(new Prompt(messages))
                .getResult()
                .getOutput()
                .getContent();

        // Save to history
        history.add(new UserMessage(userMessage));
        history.add(new AssistantMessage(reply));

        // Keep last 20 messages only
        if (history.size() > 20) {
            history.subList(0, history.size() - 20).clear();
        }

        log.info("Chatbot [{}] — '{}'", sid,
                userMessage.substring(0, Math.min(50, userMessage.length())));

        return new ChatResponse(reply, sid);
    }

    public void clearSession(String sessionId) {
        sessions.remove(sessionId);
    }
}