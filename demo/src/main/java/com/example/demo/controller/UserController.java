package com.example.demo.controller;

import com.example.demo.dto.UserRequestDto;
import com.example.demo.dto.UserResponseDto;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador REST que expone los endpoints de la API de usuarios.
 *
 * <p>Este controlador es responsable únicamente de:</p>
 * <ul>
 *   <li>Recibir y parsear las peticiones HTTP.</li>
 *   <li>Delegar la lógica de negocio a {@link UserService}.</li>
 *   <li>Retornar la respuesta HTTP con el código de estado apropiado.</li>
 * </ul>
 *
 * <p>El manejo de excepciones (conversión de {@link IllegalArgumentException}
 * a respuestas HTTP) se centraliza en {@code GlobalExceptionHandler}
 * para mantener este controlador limpio.</p>
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    // Inyección por constructor — permite testing sin contexto de Spring
    public UserController(final UserService userService) {
        this.userService = userService;
    }

    // =========================================================================
    // GET /users — listar usuarios con filtro y/u ordenamiento opcionales
    // =========================================================================

    /**
     * Retorna la lista de usuarios, opcionalmente filtrada y/u ordenada.
     *
     * <p>Parámetros opcionales:</p>
     * <ul>
     *   <li>{@code sortedBy} — atributo por el que ordenar (email, name, taxId, etc.).</li>
     *   <li>{@code filter}   — expresión en formato {@code atributo+operador+valor}.</li>
     * </ul>
     *
     * <p>Si {@code filter} si se envía vacío o en blanco, se rechaza con 400.</p>
     *
     * @return 200 OK con la lista de usuarios (puede estar vacía).
     *         400 Bad Request si {@code filter} es inválido o {@code sortedBy} no es soportado.
     */
    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getUsers(
            @RequestParam(required = false) final String sortedBy,
            @RequestParam(required = false) final String filter
    ) {
        // Rechazar filter vacío explícitamente antes de llegar al servicio
        if (filter != null && filter.isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .build();
        }

        final List<UserResponseDto> users = userService.getUsers(sortedBy, filter);
        return ResponseEntity.ok(users);
    }

    // =========================================================================
    // POST /users — crear nuevo usuario
    // =========================================================================

    /**
     * Crea un nuevo usuario con los datos proporcionados en el cuerpo de la petición.
     *
     * <p>Bean Validation ({@code @Valid}) valida el formato del DTO antes de llegar
     * al servicio. El servicio aplica las reglas de negocio adicionales (unicidad RFC,
     * edad mínima, etc.).</p>
     *
     * <p>La contraseña es cifrada con AES-256 por el servicio y nunca aparece
     * en la respuesta.</p>
     *
     * @param dto Datos del nuevo usuario. Validado automáticamente por Jakarta Bean Validation.
     * @return 201 Created con el {@link UserResponseDto} del usuario creado.
     *         400 Bad Request si los datos no pasan la validación de formato.
     *         409 Conflict si el {@code taxId} ya está registrado.
     */
    @PostMapping
    public ResponseEntity<UserResponseDto> createUser(
            @Valid @RequestBody final UserRequestDto dto
    ) {
        final UserResponseDto created = userService.createUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // =========================================================================
    // PATCH /users/{id} — actualización parcial de usuario
    // =========================================================================

    /**
     * Aplica una actualización parcial sobre el usuario identificado por {@code id}.
     *
     * <p>Solo se modifican los campos presentes en el mapa del cuerpo. Campos
     * soportados: {@code email}, {@code name}, {@code phone}, {@code password},
     * {@code taxId}, {@code addresses}.</p>
     *
     * <p>Si se actualiza {@code taxId} o {@code phone}, el servicio re-valida
     * las reglas de negocio correspondientes.</p>
     *
     * @param id      UUID del usuario a actualizar.
     * @param updates Mapa de campos con sus nuevos valores.
     * @return 200 OK con el {@link UserResponseDto} actualizado.
     *         400 Bad Request si algún valor del mapa es inválido.
     *         404 Not Found si no existe un usuario con el {@code id} indicado.
     *         409 Conflict si el nuevo {@code taxId} ya pertenece a otro usuario.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<UserResponseDto> updateUser(
            @PathVariable final UUID id,
            @RequestBody final Map<String, Object> updates
    ) {
        // Rechazar petición con cuerpo vacío — no hay nada que actualizar
        if (updates == null || updates.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        final UserResponseDto updated = userService.updateUser(id, updates);
        return ResponseEntity.ok(updated);
    }

    // =========================================================================
    // DELETE /users/{id} — eliminar usuario
    // =========================================================================

    /**
     * Elimina el usuario identificado por {@code id}.
     *
     * @param id UUID del usuario a eliminar.
     * @return 204 No Content si la eliminación fue exitosa.
     *         404 Not Found si no existe un usuario con el {@code id} indicado.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable final UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // POST /users/login — autenticación de usuario
    // =========================================================================

    /**
     * Autentica a un usuario usando su {@code taxId} (RFC) como username y su contraseña.
     *
     * <p>El servicio descifra la contraseña almacenada (AES-256) y la compara con
     * la proporcionada. Por seguridad, el mensaje de error no indica si el RFC
     * no existe o si la contraseña es incorrecta.</p>
     *
     * <p>Cuerpo esperado:</p>
     * <pre>{@code
     * {
     *   "taxId":    "ABCD850101XY1",
     *   "password": "miContraseña"
     * }
     * }</pre>
     *
     * @param loginRequest Mapa con {@code taxId} y {@code password}.
     * @return 200 OK con el {@link UserResponseDto} del usuario autenticado.
     *         400 Bad Request si faltan los campos {@code taxId} o {@code password}.
     *         401 Unauthorized si las credenciales son inválidas.
     */
    @PostMapping("/login")
    public ResponseEntity<UserResponseDto> login(
            @RequestBody final Map<String, String> loginRequest
    ) {
        final String taxId    = loginRequest.get("taxId");
        final String password = loginRequest.get("password");

        // Validar presencia de credenciales antes de llegar al servicio
        if (taxId == null || taxId.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        final UserResponseDto authenticated = userService.login(taxId, password);
        return ResponseEntity.ok(authenticated);
    }
}