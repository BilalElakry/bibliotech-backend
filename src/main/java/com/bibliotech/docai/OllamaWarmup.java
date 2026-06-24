package com.bibliotech.docai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class OllamaWarmup {

    private final ChatClient chatClient;

    private static final int MAX_RETRIES = 5;       // Limite de tentatives si Ollama démarre en même temps
    private static final int RETRY_DELAY_MS = 5000; // Délai d'attente entre deux échecs (5 secondes)

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("[DocAI] Initialisation du préchauffage asynchrone d'Ollama...");

        CompletableFuture.runAsync(() -> {
            int attempt = 0;
            boolean warmedUp = false;

            while (attempt < MAX_RETRIES && !warmedUp) {
                attempt++;
                try {
                    long startTime = System.currentTimeMillis();

                    // Aligné sur l'identité de ton DocumentAiService pour maximiser le Prompt Caching
                    chatClient.prompt()
                            .system("Tu es ScribeAI, assistant académique intégré à l'application BiblioTech.")
                            .user("Ping pour initialisation de la VRAM.")
                            .call();

                    long duration = System.currentTimeMillis() - startTime;
                    log.info("[DocAI] Préchauffage réussi en {} ms ! Modèle qwen2.5:3b verrouillé en VRAM.", duration);
                    warmedUp = true;
                } catch (Exception e) {
                    log.warn("[DocAI] Serveur Ollama injoignable (Tentative {}/{}). Nouvelle vérification dans {} secondes...",
                            attempt, MAX_RETRIES, (RETRY_DELAY_MS / 1000));

                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("[DocAI] Processus de préchauffage interrompu brusquement.", ie);
                        return;
                    }
                }
            }

            if (!warmedUp) {
                log.error("[DocAI] Critique : Impossible de préchauffer le modèle après {} tentatives. " +
                        "Vérifiez qu'Ollama tourne localement sur le port 11434.", MAX_RETRIES);
            }
        });
    }
}