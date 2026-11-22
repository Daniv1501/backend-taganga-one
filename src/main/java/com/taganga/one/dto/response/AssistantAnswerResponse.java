package com.taganga.one.dto.response;

/**
 * @param answer respuesta en HTML seguro (todo texto externo va escapado)
 * @param source quién respondió: {@code gemini} (IA) o {@code keywords} (motor de palabras clave)
 */
public record AssistantAnswerResponse(
        String answer,
        String source
) {

    public static final String SOURCE_GEMINI = "gemini";
    public static final String SOURCE_KEYWORDS = "keywords";

    public AssistantAnswerResponse(String answer) {
        this(answer, SOURCE_KEYWORDS);
    }
}
