package com.example.demo.service;

import com.example.demo.dto.UserRequestDto;
import com.example.demo.dto.UserResponseDto;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.Aes256Util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserServiceTest {

    // -------------------------------------------------------------------------
    // Constantes de los usuarios semilla definidos en UserRepository.seedUsers()
    //
    //  UUID a1b2c3d4-e5f6-7890-abcd-ef1234567890  taxId: GALA850101AB1  pass: "Secreta@2024"
    //  UUID b2c3d4e5-f6a7-8901-bcde-f12345678901  taxId: MAHS920315CD2  pass: "Clave$Segura99"
    //  UUID c3d4e5f6-a7b8-9012-cdef-123456789012  taxId: ROSC880720EF3  pass: "P@ssw0rd!Carlos"
    // -------------------------------------------------------------------------

    private static final UUID   SEED_UUID_SOFIA   = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");
    private static final UUID   SEED_UUID_CARLOS  = UUID.fromString("c3d4e5f6-a7b8-9012-cdef-123456789012");

    private static final String SEED_TAXID_SOFIA  = "MAHS920315CD2";
    private static final String SEED_PASS_SOFIA   = "Clave$Segura99";



    private UserRepository repository;
    private UserService    userService;

    // -------------------------------------------------------------------------
    // Setup — se instancia un repositorio fresco con los 3 semilla antes de cada test
    // -------------------------------------------------------------------------

    @BeforeEach
    void setUp() {
        repository  = new UserRepository();  // carga los 3 usuarios semilla automáticamente
        userService = new UserService(repository);
    }

    // =========================================================================
    // getUsers — ordenamiento y filtrado
    // =========================================================================

    @Test
    @DisplayName("Prueba 1: getUsers ordenado por email retorna la lista en orden alfabético ascendente")
    void getUsersSortedByEmail_shouldReturnSortedList() {
        // Arrange
        String sortedBy = "email";

        // Act
        List<UserResponseDto> result = userService.getUsers(sortedBy, null);

        // Assert — todos los emails deben estar ordenados alfabéticamente
        assertThat(result)
                .isNotEmpty()
                .extracting(UserResponseDto::getEmail)
                .isSortedAccordingTo(String::compareToIgnoreCase);
    }

    @Test
    @DisplayName("Prueba 2: getUsers filtrado por nombre con 'co+mart' retorna solo los usuarios que coinciden")
    void getUsersFilteredByNameContains_shouldReturnMatchingUsers() {
        // Arrange — solo "Sofía Martínez Hernández" del seed contiene "mart"
        String filter = "name co mart";

        // Act
        List<UserResponseDto> result = userService.getUsers(null, filter);

        // Assert — exactamente un resultado cuyo nombre contiene "Martínez"
        assertThat(result)
                .hasSize(1)
                .first()
                .extracting(UserResponseDto::getName)
                .asString()
                .containsIgnoringCase("Martínez");
    }

    // =========================================================================
    // createUser — creación con contraseña cifrada
    // =========================================================================

    @Test
    @DisplayName("Prueba 3: createUser guarda el usuario con la contraseña cifrada, nunca en texto plano")
    void createUser_shouldStoreUserWithEncryptedPassword() {
        // Arrange — RFC distinto a los 3 semilla para evitar conflicto de unicidad
        String plainPassword = "NuevaClave@2025";

        UserRequestDto dto = UserRequestDto.builder()
                .email("nuevo.usuario@email.com")
                .name("Laura Pérez Gómez")
                .phone("+525511112222")
                .password(plainPassword)
                .taxId("PEGL900210XY4")   // RFC único, no presente en los semilla
                .addresses(List.of())
                .build();

        // Act
        UserResponseDto response = userService.createUser(dto);

        // Assert — contraseña cifrada en repositorio, nunca expuesta en el DTO de respuesta
        UUID storedId = response.getId();
        String storedEncryptedPassword = repository.findById(storedId)
                .orElseThrow()
                .getPassword();

        assertThat(storedEncryptedPassword)
                .isNotNull()
                .isNotEqualTo(plainPassword);

        assertThat(Aes256Util.decrypt(storedEncryptedPassword))
                .isEqualTo(plainPassword);
    }

    // =========================================================================
    // updateUser — actualización parcial
    // =========================================================================

    @Test
    @DisplayName("Prueba 4: updateUser modifica correctamente el atributo email del usuario por id")
    void updateUser_shouldModifyAttributeById() {
        // Arrange — actualizar email del semilla Sofía usando su UUID conocido
        String newEmail = "sofia.nueva@email.com";
        Map<String, Object> updates = Map.of("email", newEmail);

        // Act
        UserResponseDto result = userService.updateUser(SEED_UUID_SOFIA, updates);

        // Assert — email actualizado; taxId e id permanecen intactos
        assertThat(result.getEmail()).isEqualTo(newEmail);
        assertThat(result.getId()).isEqualTo(SEED_UUID_SOFIA);
        assertThat(result.getTaxId()).isEqualTo(SEED_TAXID_SOFIA);
    }

    // =========================================================================
    // deleteUser — eliminación por id
    // =========================================================================

    @Test
    @DisplayName("Prueba 5: deleteUser elimina correctamente el usuario con el id proporcionado")
    void deleteUser_shouldRemoveUserById() {
        // Arrange — eliminar el semilla Carlos usando su UUID conocido
        // Act
        userService.deleteUser(SEED_UUID_CARLOS);

        // Assert — el usuario ya no existe en el repositorio
        assertThat(repository.findById(SEED_UUID_CARLOS)).isEmpty();
    }

    // =========================================================================
    // login — autenticación
    // =========================================================================

    @Test
    @DisplayName("Prueba 6: login autentica correctamente con taxId y contraseña válidos del semilla")
    void login_shouldAuthenticateWithTaxIdAndPassword() {
        // Arrange — credenciales exactas del semilla Sofía
        // Act
        UserResponseDto result = userService.login(SEED_TAXID_SOFIA, SEED_PASS_SOFIA);

        // Assert — retorna el DTO del usuario correcto sin contraseña
        assertThat(result).isNotNull();
        assertThat(result.getTaxId()).isEqualTo(SEED_TAXID_SOFIA);
        assertThat(result.getEmail()).isEqualTo("sofia.martinez@email.com");
    }

    @Test
    @DisplayName("Prueba 7: login falla con contraseña incorrecta y lanza IllegalArgumentException")
    void login_shouldFailWithWrongPassword() {
        // Arrange — taxId válido del semilla Sofía pero contraseña incorrecta
        String wrongPassword = "ContraseñaEquivocada@000";

        // Act + Assert — debe lanzar excepción con mensaje genérico (sin revelar qué campo falló)
        assertThatThrownBy(() -> userService.login(SEED_TAXID_SOFIA, wrongPassword))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Credenciales inválidas");
    }
}