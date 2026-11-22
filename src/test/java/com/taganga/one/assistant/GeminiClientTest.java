package com.taganga.one.assistant;

import com.taganga.one.dto.request.ChatTurn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiClientTest {

    private static final String BASE = "https://gemini.test/v1beta";
    private static final String URL = BASE + "/models/gemini-test:generateContent";

    private MockRestServiceServer server;
    private GeminiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GeminiClient("clave-secreta", "gemini-test", 700, builder);
    }

    @Test
    void shouldSendInstructionHistoryAndQuestionAndReadTheAnswer() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "clave-secreta"))
                .andExpect(jsonPath("$.systemInstruction.parts[0].text").value("Eres Tagui"))
                // El saludo inicial del asistente se descarta: Gemini espera empezar por el usuario
                .andExpect(jsonPath("$.contents.length()").value(3))
                .andExpect(jsonPath("$.contents[0].role").value("user"))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value("¿Dónde bucear?"))
                .andExpect(jsonPath("$.contents[1].role").value("model"))
                .andExpect(jsonPath("$.contents[2].role").value("user"))
                .andExpect(jsonPath("$.contents[2].parts[0].text").value("¿Y queda lejos?"))
                .andExpect(jsonPath("$.generationConfig.maxOutputTokens").value(700))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"role":"model","parts":[{"text":"Unos **20 minutos**."}]},
                          "finishReason":"STOP"}],"usageMetadata":{"totalTokenCount":42}}
                        """, MediaType.APPLICATION_JSON));

        List<ChatTurn> history = List.of(
                new ChatTurn("assistant", "¡Hola! Soy Tagui."),
                new ChatTurn("user", "¿Dónde bucear?"),
                new ChatTurn("assistant", "En las zonas de buceo concertado."));

        assertThat(client.generate("Eres Tagui", history, "¿Y queda lejos?"))
                .contains("Unos **20 minutos**.");
        server.verify();
    }

    @Test
    void shouldReturnEmptyWhenQuotaIsExhausted() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThat(client.generate("x", List.of(), "hola")).isEmpty();
    }

    @Test
    void shouldReturnEmptyOnServerError() {
        server.expect(requestTo(URL)).andRespond(withServerError());

        assertThat(client.generate("x", List.of(), "hola")).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenResponseHasNoText() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess("{\"candidates\":[{\"finishReason\":\"SAFETY\"}]}", MediaType.APPLICATION_JSON));

        assertThat(client.generate("x", List.of(), "hola")).isEmpty();
    }

    @Test
    void shouldNotCallGoogleWithoutApiKey() {
        GeminiClient disabled = new GeminiClient("  ", "gemini-test", 700, RestClient.builder().baseUrl(BASE));

        assertThat(disabled.isEnabled()).isFalse();
        assertThat(disabled.generate("x", List.of(), "hola")).isEmpty();
    }
}
