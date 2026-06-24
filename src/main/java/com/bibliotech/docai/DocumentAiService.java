package com.bibliotech.docai;

import com.bibliotech.catalogue.document.Document;
import com.bibliotech.catalogue.document.DocumentRepository;
import com.bibliotech.chatbot.ChatMessage;
import com.bibliotech.chatbot.ChatMessageRepository;
import com.bibliotech.exception.BusinessException;
import com.bibliotech.exception.ResourceNotFoundException;
import com.bibliotech.tts.TtsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.text.Normalizer;


import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentAiService {

    private final ChatClient chatClient;
    private final DocumentRepository documentRepository;
    private final TtsService ttsService;
    private final ChatMessageRepository chatMessageRepository;

    private static final int MAX_CONTEXT_CHARS = 6000;

    private String getCurrentUsername() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return "ANONYMOUS";
            String name = auth.getName();
            return (name != null && !name.isBlank()) ? name : "ANONYMOUS";
        } catch (Exception e) {
            log.warn("[DocAI] Contexte auth illisible : {}", e.getMessage());
            return "ANONYMOUS";
        }
    }

    public String summarize(Long documentId) {
        Document doc = findDoc(documentId);
        String text = extractTruncated(doc);
        String system = """
            Tu es un assistant littéraire. Fais un résumé global et bien rédigé du document "%s".
            Rédige 2 ou 3 paragraphes fluides. 
            INTERDICTION d'utiliser des listes à puces. Uniquement du texte narratif.
            """.formatted(doc.getTitre());
        return callLlm(system, "Rédige le résumé de ce texte.");
    }

    public String extractKeys(Long documentId) {
        Document doc = findDoc(documentId);
        String text = extractTruncated(doc);
        String system = """
            Tu es un processeur de données structurées. Tu ne fais aucune phrase de politesse.
            Extrais les métadonnées de "%s" et retourne STRICTEMENT un tableau Markdown à 2 colonnes :
            
            | Méta-Donnée | Valeur extraite |
            | :--- | :--- |
            | **Thème principal** | [2 à 4 mots max] |
            | **Mots-Clés purs** | [Liste de 4 termes techniques] |
            | **Chiffres / Dates** | [Données clés ou 'Aucun chiffre'] |
            | **Livrable / But** | [1 phrase synthétique] |
            """.formatted(doc.getTitre());
        return callLlm(system, "Génère le tableau Markdown des clés.");
    }

    public String explain(Long documentId, String userPastedText) {
        if (userPastedText == null || userPastedText.isBlank()) {
            throw new BusinessException("Aucun passage à expliquer.");
        }
        Document doc = findDoc(documentId);
        String fullText = extractTruncated(doc); // texte extrait proprement par le serveur

        // Normalise le texte collé par l'utilisateur (peut être corrompu par le copier-coller)
        String cleanedInput = normalizeUserInput(userPastedText);

        String system = """
        Tu es ScribeAI, assistant académique. Voici le contenu du document "%s" de %s :
        ---
        %s
        ---
        L'étudiant essaie d'expliquer ce passage (la copie peut contenir des caractères corrompus,
        base-toi sur le sens et le contexte du document pour l'identifier) :
        « %s »

        Trouve ce passage dans le document et explique-le clairement en français.
        Si tu ne le trouves pas exactement, explique le passage le plus proche dans le contexte.
        """.formatted(doc.getTitre(), doc.getAuteur(), fullText, cleanedInput);

        return callLlm(system, "Explique ce passage.");
    }

    /**
     * Normalise le texte collé depuis un PDF potentiellement mal encodé.
     * Applique NFC, supprime les caractères de contrôle parasites.
     */
    private static String normalizeUserInput(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFC);
        // Supprime caractères de contrôle sauf espaces/sauts de ligne
        normalized = normalized.replaceAll("[\\p{Cntrl}&&[^\n\t]]", "");
        return normalized.replaceAll("[ \t]+", " ").trim();
    }

    @Transactional
    public DocAiResponse ask(Long documentId, String question, String sessionId) {
        if (question == null || question.isBlank()) throw new BusinessException("Question vide.");
        Document doc = findDoc(documentId);
        String text = extractTruncated(doc);

        String sid = (sessionId != null && !sessionId.isBlank()) ? sessionId : "doc-" + documentId + "-" + UUID.randomUUID();
        String user = getCurrentUsername();

        chatMessageRepository.save(ChatMessage.builder().sessionId(sid).role("USER").content(question).username(user).build());

        String system = "Tu es ScribeAI. Document de référence : \"%s\". Réponds uniquement en te basant sur ce texte.\n---\n%s".formatted(doc.getTitre(), text);

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(system));

        List<ChatMessage> history = chatMessageRepository.findBySessionIdOrderByTimestampAsc(sid);

        // CORRECTION DE SÉCURITÉ : On ne prend que les 6 derniers messages (3 questions / 3 réponses)
        history.stream().skip(Math.max(0, history.size() - 6)).forEach(m -> {
            if ("USER".equals(m.getRole())) messages.add(new UserMessage(m.getContent()));
            else messages.add(new AssistantMessage(m.getContent()));
        });

        String reply = chatClient.prompt(new Prompt(messages)).call().content();
        if (reply == null || reply.isBlank()) reply = "Je n'ai pas pu générer de réponse.";

        chatMessageRepository.save(ChatMessage.builder().sessionId(sid).role("AI").content(reply).username(user).build());

        return new DocAiResponse(reply, sid);
    }

    private Document findDoc(Long id) {
        return documentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Document introuvable"));
    }

    private String extractTruncated(Document doc) {
        String text = ttsService.extractText(doc.getFichierPdf());
        return text.length() > MAX_CONTEXT_CHARS ? text.substring(0, MAX_CONTEXT_CHARS) + "…" : text;
    }

    private String callLlm(String system, String user) {
        try {
            return chatClient.prompt().system(system).user(user).call().content();
        } catch (Exception e) {
            log.error("Erreur critique LLM", e);
            throw new BusinessException("Le moteur IA a mis trop de temps à répondre.");
        }
    }
}