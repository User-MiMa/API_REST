package com.example.demo.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción de negocio personalizada que transporta un código HTTP explícito.
 *
 * <p>Debe lanzarse desde la capa de servicio cuando un error de negocio tiene
 * un código HTTP semánticamente preciso que no puede inferirse solo del mensaje.
 * El {@link GlobalExceptionHandler} la captura y construye la respuesta JSON
 * usando el {@link HttpStatus} que esta excepción lleva consigo.</p>
 *
 * <p><strong>Cuándo preferirla sobre {@link IllegalArgumentException}:</strong><br>
 * Usar {@code CustomException} cuando el código HTTP importa (404, 401, 409).
 * Usar {@link IllegalArgumentException} para errores de formato o argumento
 * simples que siempre mapean a 400.</p>
 *
 * <p><strong>Ejemplos de uso:</strong></p>
 * <pre>{@code
 * // 404 — recurso no encontrado
 * throw new CustomException("Usuario no encontrado con id: " + id, HttpStatus.NOT_FOUND);
 *
 * // 409 — conflicto de unicidad
 * throw new CustomException("El RFC '" + taxId + "' ya está registrado", HttpStatus.CONFLICT);
 *
 * // 401 — credenciales inválidas
 * throw new CustomException("Credenciales inválidas", HttpStatus.UNAUTHORIZED);
 *
 * // 400 — datos incorrectos del cliente
 * throw new CustomException("El formato del teléfono es inválido", HttpStatus.BAD_REQUEST);
 * }</pre>
 */
public class CustomException extends RuntimeException {

    /** Código de estado HTTP que será retornado al cliente en la respuesta JSON. */
    private final HttpStatus status;

    /**
     * Construye una excepción de negocio con mensaje descriptivo y código HTTP.
     *
     * @param message Descripción del error. Será incluida en el campo {@code "message"}
     *                del cuerpo JSON de la respuesta. No debe ser {@code null}.
     * @param status  Código HTTP que representa la naturaleza del error.
     *                No debe ser {@code null}.
     */
    public CustomException(final String message, final HttpStatus status) {
        super(message);
        this.status = status;
    }

    /**
     * Retorna el código HTTP asociado a este error de negocio.
     *
     * @return {@link HttpStatus} con el que responderá la API al cliente.
     */
    public HttpStatus getStatus() {
        return status;
    }
}