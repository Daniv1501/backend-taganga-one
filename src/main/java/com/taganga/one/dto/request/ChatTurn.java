package com.taganga.one.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Un turno anterior de la conversación, en texto plano, para que el asistente
 * entienda preguntas de seguimiento ("¿y queda lejos?").
 */
public record ChatTurn(

        @NotBlank
        @Pattern(regexp = "user|assistant", message = "El rol debe ser user o assistant.")
        String role,

        @NotBlank
        @Size(max = 2000, message = "Cada mensaje del historial admite máximo 2000 caracteres.")
        String text

) {
}
