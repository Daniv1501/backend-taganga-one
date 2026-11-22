package com.taganga.one.assistant;

import com.taganga.one.dto.request.AskAssistantRequest;

import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Respuesta del asistente con IA: conocimiento de Taganga + Gemini + formato HTML seguro.
 * Vacío cuando Gemini no está configurado o no respondió.
 */
@Component
public class GeminiAssistant {

    private final GeminiClient client;
    private final TagangaKnowledge knowledge;

    public GeminiAssistant(GeminiClient client, TagangaKnowledge knowledge) {
        this.client = client;
        this.knowledge = knowledge;
    }

    public Optional<String> answer(AskAssistantRequest request) {
        if (!client.isEnabled()) {
            return Optional.empty();
        }
        return client.generate(
                        knowledge.systemInstruction(request.visitorName()),
                        request.historyOrEmpty(),
                        request.question().trim())
                .map(AnswerFormatter::toHtml)
                .filter(html -> !html.isBlank());
    }
}
