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
    private final RagRetrievalService ragRetrievalService;

    private static final Set<String> GREETINGS_FR = Set.of(
            "bonjour", "bonsoir", "salut", "coucou", "allo", "all", "bienvenue", "enchant", "enchante",
            "rebonjour", "rebonsoir", "re", "lu", "plop", "chalut", "slt", "bjr", "bsr", "cc", "bvn",
            "cava", "cv", "quoideneuf", "hey", "heyy", "heyyy", "yo", "yop", "yosh", "we", "oue", "ouais",
            "salam", "salem", "slm", "asalam", "assalam", "marhaba", "ahlan", "labas", "ahlane",
            "sbahelkhir", "sbahelkhyr", "mselkhir", "mselkhyr", "sbahlkhir", "sbahlkhyr",
            "bonjourr", "bonjourrr", "bonsoirr", "saluttt", "salutttt", "coucouu", "coucouuu", "yoo", "yooo"
    );

    private static final Set<String> GREETINGS_EN = Set.of(
            "hello", "hi", "howdy", "morning", "goodmorning", "evening", "goodevening", "greetings",
            "sup", "whatsup", "wazzup", "wsp", "wazaa", "heythere", "hellothere", "welcome", "welcom",
            "heya", "heyya", "helloo", "hellooo", "helloooo", "hii", "hiii", "hiiii"
    );

    private static final List<String> GREETING_REPLIES_FR = List.of(
            "Bonjour ! Je suis ScribeAI, votre assistant de bibliothèque. Comment puis-je vous aider ?",
            "Bonsoir ! ScribeAI à votre service. Que recherchez-vous dans nos archives ?",
            "Salutations ! Je suis prêt à explorer les documents de la bibliothèque avec vous."
    );

    private static final List<String> GREETING_REPLIES_EN = List.of(
            "Hello! I am ScribeAI, your library assistant. How can I help your research today?",
            "Good day! ScribeAI at your service. What are you looking for in our archives?"
    );

    private static final Random RNG = new Random();
    private enum DetectedLang { FR, EN, NONE }

    private DetectedLang detectGreetingLanguage(String message) {
        if (message == null || message.isBlank()) return DetectedLang.NONE;
        String cleaned = message.trim().toLowerCase().replaceAll("[!?.,;:''\"()\\-]", "").trim();
        if (GREETINGS_FR.contains(cleaned)) return DetectedLang.FR;
        if (GREETINGS_EN.contains(cleaned)) return DetectedLang.EN;
        return DetectedLang.NONE;
    }

    // ✅ NOUVEAU : Détecter si l'utilisateur demande explicitement la liste globale des documents
    private boolean isCatalogueQuery(String message) {
        if (message == null) return false;
        String msg = message.toLowerCase().trim();
        return msg.contains("documents disponibles")
                || msg.contains("documents dispo")
                || msg.contains("liste des documents")
                || msg.contains("quels livres")
                || msg.contains("catalogue");
    }

    // ═══════════════════════════════════════════════════════════════
    // PROMPTS SYSTÈME OPTIMISÉS
    // ═══════════════════════════════════════════════════════════════

    private static final String SYSTEM_PROMPT_RAG = """
      Tu es ScribeAI, un assistant académique d'élite.
      
      Voici des extraits pertinents de documents :
      <extraits>
      %s
      </extraits>

      [DIRECTIVE DE LANGUE]
      Réponds dans la langue de la question (FRANÇAIS ou ANGLAIS).

      [RÈGLES STRICTES]
      1. Sois extrêmement concis, direct et va droit au but. Réponds en maximum 2 ou 3 phrases.
      2. Base-toi uniquement sur les <extraits>. Ne fabrique rien.
      """;

    private static final String SYSTEM_PROMPT_CATALOGUE = """
      Tu es ScribeAI, un assistant académique d'élite.

      Voici la liste complète et exacte des documents disponibles dans notre bibliothèque :
      <catalogue>
      %s
      </catalogue>

      [DIRECTIVE DE LANGUE]
      Réponds dans la langue de la question (FRANÇAIS ou ANGLAIS).

      [RÈGLES CRITIQUES]
      1. Tu DOIS lister TOUS les documents présents dans la liste <catalogue> ci-dessus, sans exception.
      2. Formate ta réponse sous forme de liste numérotée claire.
      3. Ne donne pas de détails superflus, cite juste les titres, auteurs et dates pour être rapide.
      """;

    @Transactional
    public ChatResponse chat(String userMessage, String sessionId, String username) {
        String sid = (sessionId != null && !sessionId.isBlank()) ? sessionId : UUID.randomUUID().toString();

        log.info("Chat – user={} | session={} | query='{}'", username, sid, truncate(userMessage, 80));

        // 1. FAST-PATH SALUTATIONS
        DetectedLang detectedLang = detectGreetingLanguage(userMessage);
        if (detectedLang != DetectedLang.NONE) {
            String greetingReply = (detectedLang == DetectedLang.FR)
                    ? GREETING_REPLIES_FR.get(RNG.nextInt(GREETING_REPLIES_FR.size()))
                    : GREETING_REPLIES_EN.get(RNG.nextInt(GREETING_REPLIES_EN.size()));

            saveMessage(username, sid, "USER", userMessage);
            saveMessage(username, sid, "AI", greetingReply);
            return new ChatResponse(greetingReply, sid);
        }

        saveMessage(username, sid, "USER", userMessage);

        String systemContent;

        // ✅ 2. ROUTAGE INTELLIGENT : Si demande globale de catalogue -> On court-circuite le RAG vectoriel
        if (isCatalogueQuery(userMessage)) {
            log.info("Requête catalogue détectée pour user={} — Utilisation directe de MySQL", username);
            systemContent = String.format(SYSTEM_PROMPT_CATALOGUE, buildCatalogueContext());
        } else {
            // Requête contextuelle classique -> Recherche vectorielle Qdrant
            String ragContext = ragRetrievalService.retrieveContext(userMessage);
            if (!ragContext.isBlank()) {
                systemContent = String.format(SYSTEM_PROMPT_RAG, ragContext);
                log.info("RAG context injecté ({} chars)", ragContext.length());
            } else {
                log.info("Pas de contexte RAG sémantique — Fallback catalogue pour user={}", username);
                systemContent = String.format(SYSTEM_PROMPT_CATALOGUE, buildCatalogueContext());
            }
        }

        // 3. PRÉPARATION DES MESSAGES ET DE L'HISTORIQUE
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemContent));

        List<ChatMessage> history = chatMessageRepository.findByUsernameAndSessionIdOrderByTimestampAsc(username, sid);
        List<ChatMessage> window = history.size() > 10 ? history.subList(history.size() - 10, history.size()) : history;

        for (ChatMessage msg : window) {
            if ("USER".equals(msg.getRole())) messages.add(new UserMessage(msg.getContent()));
            else if ("AI".equals(msg.getRole())) messages.add(new AssistantMessage(msg.getContent()));
        }

        // 4. APPEL LLM
        String reply;
        try {
            long t0 = System.currentTimeMillis();
            reply = chatClient.prompt(new Prompt(messages)).call().content();
            log.info("LLM a répondu en {} ms (user={})", System.currentTimeMillis() - t0, username);
        } catch (Exception e) {
            log.error("Erreur LLM – user={} : {}", username, e.getMessage());
            throw new RuntimeException("Erreur de communication avec l'assistant IA", e);
        }

        if (reply == null || reply.isBlank()) reply = "Je n'ai pas pu générer de réponse.";

        saveMessage(username, sid, "AI", reply);
        return new ChatResponse(reply, sid);
    }

    public List<ChatMessage> getHistory(String sessionId, String username) {
        return chatMessageRepository.findByUsernameAndSessionIdOrderByTimestampAsc(username, sessionId);
    }

    @Transactional
    public void clearSession(String sessionId, String username) {
        chatMessageRepository.deleteByUsernameAndSessionId(username, sessionId);
        log.info("Session effacée – user={} | session={}", username, sessionId);
    }

    private void saveMessage(String username, String sessionId, String role, String content) {
        chatMessageRepository.save(ChatMessage.builder()
                .username(username).sessionId(sessionId)
                .role(role).content(content).build());
    }

    private String buildCatalogueContext() {
        var documents = documentService.findAllForRag();
        if (documents.isEmpty()) return "Aucun document disponible.";
        StringBuilder sb = new StringBuilder();
        // Tri ou affichage complet de tous les documents de la table
        for (int i = 0; i < documents.size(); i++) {
            var d = documents.get(i);
            sb.append(String.format(
                    "%d. \"%s\" de %s (%d) — Catégorie: %s\n",
                    i + 1, d.getTitre(), d.getAuteur(), d.getAnneePublication(),
                    d.getCategorie() != null ? d.getCategorie().getNom() : "N/A"));
        }
        return sb.toString();
    }

    private String truncate(String s, int max) {
        return s == null ? "null" : s.length() <= max ? s : s.substring(0, max) + "...";
    }
}