package com.taganga.one.assistant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.taganga.one.dto.request.ChatTurn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Cliente mínimo de la API de Gemini (Google AI Studio), método generateContent.
 * Nunca lanza excepciones hacia afuera: si no hay clave, Google falla, se agota la
 * cuota o tarda demasiado, devuelve vacío y el asistente usa su motor de respaldo.
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final String apiKey;
    private final String model;
    private final int maxOutputTokens;
    private final RestClient http;

    @Autowired
    public GeminiClient(
            @Value("${app.gemini.api-key:}") String apiKey,
            @Value("${app.gemini.model}") String model,
            @Value("${app.gemini.base-url}") String baseUrl,
            @Value("${app.gemini.timeout:20s}") Duration timeout,
            @Value("${app.gemini.max-output-tokens:700}") int maxOutputTokens) {
        this(apiKey, model, maxOutputTokens, RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory(timeout)));
    }

    // Para pruebas: permite enlazar un servidor simulado al builder
    GeminiClient(String apiKey, String model, int maxOutputTokens, RestClient.Builder builder) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.maxOutputTokens = maxOutputTokens;
        this.http = builder.build();
    }

    private static SimpleClientHttpRequestFactory requestFactory(Duration timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(timeout);
        return factory;
    }

    public boolean isEnabled() {
        return !apiKey.isEmpty();
    }

    public String model() {
        return model;
    }

    /**
     * @param systemInstruction reglas y conocimiento del asistente
     * @param history           turnos anteriores en texto plano (pueden estar vacíos)
     * @param question          pregunta actual del visitante
     * @return texto de la respuesta (Markdown ligero), o vacío si no se pudo obtener
     */
    public Optional<String> generate(String systemInstruction, List<ChatTurn> history, String question) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        GenerateRequest request = new GenerateRequest(
                new Instruction(List.of(new Part(systemInstruction))),
                contents(history, question),
                new GenerationConfig(maxOutputTokens));
        try {
            GenerateResponse response = http.post()
                    .uri("/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GenerateResponse.class);
            Optional<String> text = extractText(response);
            if (text.isEmpty()) {
                log.warn("Gemini respondió sin texto; se usa el motor de palabras clave.");
            }
            return text;
        } catch (RestClientException e) {
            // Sin la clave en el mensaje: RestClient no la incluye (va en una cabecera)
            log.warn("Gemini no respondió ({}); se usa el motor de palabras clave.", e.getMessage());
            return Optional.empty();
        }
    }

    // Gemini espera turnos user/model que empiecen por el usuario
    private static List<Content> contents(List<ChatTurn> history, String question) {
        List<Content> contents = new ArrayList<>();
        for (ChatTurn turn : history) {
            String role = "assistant".equals(turn.role()) ? "model" : "user";
            if (contents.isEmpty() && role.equals("model")) {
                continue;
            }
            contents.add(new Content(role, List.of(new Part(turn.text()))));
        }
        contents.add(new Content("user", List.of(new Part(question))));
        return contents;
    }

    private static Optional<String> extractText(GenerateResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            return Optional.empty();
        }
        Content content = response.candidates().get(0).content();
        if (content == null || content.parts() == null) {
            return Optional.empty();
        }
        String text = content.parts().stream()
                .map(Part::text)
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining("\n"))
                .trim();
        return text.isEmpty() ? Optional.empty() : Optional.of(text);
    }

    // --- Formato JSON de generateContent ---

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Part(String text) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Content(String role, List<Part> parts) {
    }

    record Instruction(List<Part> parts) {
    }

    record GenerationConfig(int maxOutputTokens) {
    }

    record GenerateRequest(Instruction systemInstruction, List<Content> contents, GenerationConfig generationConfig) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Candidate(Content content, String finishReason) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GenerateResponse(List<Candidate> candidates) {
    }
}
