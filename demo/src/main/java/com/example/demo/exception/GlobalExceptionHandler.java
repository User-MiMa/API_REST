package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para todos los controladores REST de la API.
 *
 * <p>Intercepta excepciones no capturadas en los controladores y las convierte
 * en respuestas JSON con estructura consistente:</p>
 * <pre>{@code
 * {
 *   "timestamp": "2024-06-10 08:15",
 *   "status":    404,
 *   "message":   "Usuario no encontrado con id: ..."
 * }
 * }</pre>
 *
 * <p><strong>Jerarquía de handlers (de más específico a más genérico):</strong></p>
 * <ol>
 *   <li>{@link CustomException}                — código HTTP explícito desde el servicio.</li>
 *   <li>{@link MethodArgumentNotValidException} — fallos de {@code @Valid} en DTOs.</li>
 *   <li>{@link IllegalArgumentException}        — errores de argumento → 400.</li>
 *   <li>{@link Exception}                       — cualquier error no anticipado → 500.</li>
 * </ol>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Formato de fecha/hora aplicado al campo "timestamp" de cada respuesta de error
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // -------------------------------------------------------------------------
    // CustomException — errores de negocio con código HTTP explícito
    // -------------------------------------------------------------------------

    /**
     * Se dispara cuando el servicio lanza una {@link CustomException}.
     *
     * <p>Usa el {@link HttpStatus} que la excepción trae consigo, por lo que
     * puede representar cualquier código: 404, 409, 401, 400, etc.</p>
     *
     * <p><strong>Ejemplo de respuesta (404):</strong></p>
     * <pre>{@code
     * {
     *   "timestamp": "2024-06-10 08:15",
     *   "status":    404,
     *   "message":   "Usuario no encontrado con id: a1b2c3d4-..."
     * }
     * }</pre>
     *
     * @param ex Excepción capturada con su mensaje y {@link HttpStatus}.
     * @return {@link ResponseEntity} con el status de la excepción y cuerpo JSON.
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Map<String, Object>> handleCustomException(final CustomException ex) {
        return buildResponse(ex.getMessage(), ex.getStatus());
    }

    // -------------------------------------------------------------------------
    // MethodArgumentNotValidException — fallos de Bean Validation en los DTOs
    // -------------------------------------------------------------------------

    /**
     * Se dispara cuando {@code @Valid} rechaza un {@code @RequestBody} en el controlador.
     *
     * <p>Recolecta todos los mensajes de campo inválido y los une en uno solo
     * separado por {@code "; "} para que el cliente sepa exactamente qué campos corregir.</p>
     *
     * <p><strong>Ejemplo de respuesta (400):</strong></p>
     * <pre>{@code
     * {
     *   "timestamp": "2024-06-10 08:15",
     *   "status":    400,
     *   "message":   "email: must be a valid email address; phone: phone must follow E.164"
     * }
     * }</pre>
     *
     * @param ex Excepción generada por Bean Validation con la lista de errores de campo.
     * @return {@link ResponseEntity} 400 Bad Request con los mensajes de validación.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            final MethodArgumentNotValidException ex
    ) {
        // Concatenar todos los mensajes de campos fallidos en un solo string legible
        final String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return buildResponse(message, HttpStatus.BAD_REQUEST);
    }

    // -------------------------------------------------------------------------
    // IllegalArgumentException — errores de argumento del servicio o utilidades
    // -------------------------------------------------------------------------

    /**
     * Se dispara cuando el servicio o las utilidades lanzan {@link IllegalArgumentException}.
     *
     * <p>Estos errores siempre mapean a {@code 400 Bad Request} ya que representan
     * datos inválidos proporcionados por el cliente (formato de RFC, teléfono, etc.).</p>
     *
     * <p><strong>Ejemplo de respuesta (400):</strong></p>
     * <pre>{@code
     * {
     *   "timestamp": "2024-06-10 08:15",
     *   "status":    400,
     *   "message":   "taxId 'ABCD123' no coincide con el formato RFC esperado"
     * }
     * }</pre>
     *
     * @param ex Excepción capturada con su mensaje descriptivo.
     * @return {@link ResponseEntity} 400 Bad Request con el mensaje del error.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            final IllegalArgumentException ex
    ) {
        return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    // -------------------------------------------------------------------------
    // Exception — captura genérica para errores no anticipados
    // -------------------------------------------------------------------------

    /**
     * Se dispara cuando ningún handler anterior captura la excepción.
     *
     * <p>Retorna un mensaje genérico intencionalmente para no exponer detalles
     * internos de la implementación (stack traces, nombres de clases, etc.)
     * al cliente externo.</p>
     *
     * <p><strong>Ejemplo de respuesta (500):</strong></p>
     * <pre>{@code
     * {
     *   "timestamp": "2024-06-10 08:15",
     *   "status":    500,
     *   "message":   "Ocurrió un error interno. Por favor intenta más tarde."
     * }
     * }</pre>
     *
     * @param ex Excepción no anticipada capturada.
     * @return {@link ResponseEntity} 500 Internal Server Error con mensaje genérico.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(final Exception ex) {
        return buildResponse(
                "Ocurrió un error interno. Por favor intenta más tarde.",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    // -------------------------------------------------------------------------
    // Método de soporte — construye el cuerpo JSON de error de forma consistente
    // -------------------------------------------------------------------------

    /**
     * Construye el cuerpo de la respuesta de error con estructura uniforme.
     *
     * <p>El orden de las claves en el JSON es siempre: {@code timestamp}, {@code status},
     * {@code message} — garantizado por {@link LinkedHashMap}.</p>
     *
     * @param message Mensaje descriptivo del error para el campo {@code "message"}.
     * @param status  Código HTTP para el campo {@code "status"} y el status de la respuesta.
     * @return {@link ResponseEntity} lista para serializar como JSON.
     */
    private ResponseEntity<Map<String, Object>> buildResponse(
            final String message,
            final HttpStatus status
    ) {
        final Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", ZonedDateTime.now().format(TIMESTAMP_FORMATTER));
        body.put("status",    status.value());
        body.put("message",   message);

        return ResponseEntity.status(status).body(body);
    }
}