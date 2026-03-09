package com.example.demo.controller;

import com.example.demo.dto.UserRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración del controlador REST de usuarios.
 *
 * <p>Se levanta el contexto completo de Spring Boot ({@code @SpringBootTest})
 * y MockMvc se construye manualmente desde el {@link WebApplicationContext},
 * evitando la dependencia de {@code @AutoConfigureMockMvc}.</p>
 *
 * <p>{@code @DirtiesContext} reinicia el contexto después de cada test que muta
 * el estado del repositorio en memoria, garantizando aislamiento entre pruebas.</p>
 *
 * <p>Usuarios semilla disponibles (cargados por {@code UserRepository.seedUsers()}):</p>
 * <pre>
 *  taxId: GALA850101AB1  email: andres.garcia@email.com    pass: "Secreta@2024"
 *  taxId: MAHS920315CD2  email: sofia.martinez@email.com   pass: "Clave$Segura99"
 *  taxId: ROSC880720EF3  email: carlos.rodriguez@email.com pass: "P@ssw0rd!Carlos"
 * </pre>
 */
@SpringBootTest
class UserControllerTest {

    // MockMvc construido manualmente desde el contexto web de Spring
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // UUIDs semilla conocidos (definidos en UserRepository.seedUsers())
    private static final String SEED_UUID_ANDRES = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
    private static final String SEED_UUID_SOFIA  = "b2c3d4e5-f6a7-8901-bcde-f12345678901";

    // Credenciales exactas del semilla Sofía
    private static final String SEED_TAXID_SOFIA = "MAHS920315CD2";
    private static final String SEED_PASS_SOFIA  = "Clave$Segura99";

    // -------------------------------------------------------------------------
    // Setup — construir MockMvc desde el WebApplicationContext antes de cada test
    // -------------------------------------------------------------------------

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();
    }

    // =========================================================================
    // GET /users — ordenamiento y filtrado
    // =========================================================================

    @Test
    @DisplayName("Prueba 1: GET /users?sortedBy=email retorna 200 y la lista ordenada alfabéticamente")
    void GET_usersSortedByEmail_shouldReturn200AndSortedList() throws Exception {
        mockMvc.perform(get("/users")
                        .param("sortedBy", "email")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].email", is("andres.garcia@email.com")))
                .andExpect(jsonPath("$[1].email", is("carlos.rodriguez@email.com")))
                .andExpect(jsonPath("$[2].email", is("sofia.martinez@email.com")))
                // La contraseña nunca debe aparecer en ninguna respuesta
                .andExpect(jsonPath("$[*].password").doesNotExist());
    }

    @Test
    @DisplayName("Prueba 2: GET /users?filter=taxId+eq+GALA850101AB1 retorna 200 y solo el usuario filtrado")
    void GET_usersFilteredByTaxIdEq_shouldReturn200AndFilteredList() throws Exception {
        mockMvc.perform(get("/users")
                        .param("filter", "taxId eq GALA850101AB1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].taxId",  is("GALA850101AB1")))
                .andExpect(jsonPath("$[0].email",  is("andres.garcia@email.com")))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    // =========================================================================
    // POST /users — creación de usuario
    // =========================================================================

    @Test
    @DirtiesContext
    @DisplayName("Prueba 3: POST /users retorna 201 y el usuario creado sin exponer la contraseña")
    void POST_users_shouldReturn201AndUserWithoutPassword() throws Exception {
        // RFC único no presente en los semilla para evitar conflicto de unicidad
        UserRequestDto dto = UserRequestDto.builder()
                .email("nuevo.usuario@email.com")
                .name("Laura Pérez Gómez")
                .phone("+525511112222")
                .password("NuevaClave@2025")
                .taxId("PEGL900210XY4")
                .addresses(List.of())
                .build();

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id",    notNullValue()))
                .andExpect(jsonPath("$.email", is("nuevo.usuario@email.com")))
                .andExpect(jsonPath("$.taxId", is("PEGL900210XY4")))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    // =========================================================================
    // PATCH /users/{id} — actualización parcial
    // =========================================================================

    @Test
    @DirtiesContext
    @DisplayName("Prueba 4: PATCH /users/{id} retorna 200 y el usuario con el atributo actualizado")
    void PATCH_usersById_shouldReturn200AndUpdatedUser() throws Exception {
        Map<String, Object> updates = Map.of("email", "sofia.nueva@email.com");

        mockMvc.perform(patch("/users/{id}", SEED_UUID_SOFIA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id",    is(SEED_UUID_SOFIA)))
                .andExpect(jsonPath("$.email", is("sofia.nueva@email.com")))
                // taxId no debe haber cambiado
                .andExpect(jsonPath("$.taxId", is(SEED_TAXID_SOFIA)))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    // =========================================================================
    // DELETE /users/{id} — eliminación
    // =========================================================================

    @Test
    @DirtiesContext
    @DisplayName("Prueba 5: DELETE /users/{id} retorna 204 sin cuerpo y el usuario desaparece del listado")
    void DELETE_usersById_shouldReturn204() throws Exception {
        // Eliminar semilla Andrés y verificar 204
        mockMvc.perform(delete("/users/{id}", SEED_UUID_ANDRES))
                .andExpect(status().isNoContent());

        // Verificación secundaria — el usuario ya no existe en el repositorio
        mockMvc.perform(get("/users")
                        .param("filter", "taxId eq GALA850101AB1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // =========================================================================
    // POST /users/login — autenticación
    // =========================================================================

    @Test
    @DisplayName("Prueba 6: POST /users/login retorna 200 y el usuario autenticado con credenciales válidas")
    void POST_login_shouldReturn200OnValidCredentials() throws Exception {
        Map<String, String> loginRequest = Map.of(
                "taxId",    SEED_TAXID_SOFIA,
                "password", SEED_PASS_SOFIA
        );

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taxId", is(SEED_TAXID_SOFIA)))
                .andExpect(jsonPath("$.email", is("sofia.martinez@email.com")))
                .andExpect(jsonPath("$.id",    notNullValue()))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("Prueba 7: POST /users/login retorna 400 cuando la contraseña es incorrecta)")
    void POST_login_shouldReturn400OnInvalidCredentials() throws Exception {
        Map<String, String> loginRequest = Map.of(
                "taxId",    SEED_TAXID_SOFIA,
                "password", "ContraseñaEquivocada@000"
        );

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest()) //Se usa 400 y no 401 por seguridad
                .andExpect(jsonPath("$.status",  is(400)))
                .andExpect(jsonPath("$.message", is("Credenciales inválidas")));
    }
}