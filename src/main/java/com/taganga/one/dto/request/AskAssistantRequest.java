package com.taganga.one.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AskAssistantRequest(

        @NotBlank(message = "Escribe tu pregunta.")
        @Size(max = 300, message = "La pregunta admite máximo 300 caracteres.")
        String question,

        // Opcional: primer nombre del visitante, solo para personalizar el saludo.
        @Size(max = 150, message = "El nombre admite máximo 150 caracteres.")
        String visitorName,

        // Opcional: últimos turnos de la conversación (del más antiguo al más reciente).
        @Size(max = 10, message = "El historial admite máximo 10 mensajes.")
        List<@Valid ChatTurn> history

) {

    public AskAssistantRequest(String question, String visitorName) {
        this(question, visitorName, null);
    }

    public List<ChatTurn> historyOrEmpty() {
        return history == null ? List.of() : history;
    }
}
