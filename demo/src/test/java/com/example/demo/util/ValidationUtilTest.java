package com.example.demo.util;

import com.example.demo.dto.UserRequestDto;
import com.example.demo.model.Address;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.UserService;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;



class ValidationUtilTest {

    // =========================================================================
    // validateTaxId — RFC
    // =========================================================================

    @Test
    @DisplayName("Prueba 1: Validación RFC válido con datos GALJ850101AB1 - resultado esperado: no hay excepción")
    void validateTaxId_validRfc_doesNotThrowException() {
        // Arrange — RFC válido: persona mayor de 18 años, estructura correcta
        String validTaxId = "GALA850101AB1";

        // Act + Assert — no debe lanzar ninguna excepción
        assertThatCode(() -> ValidationUtil.validateTaxId(validTaxId))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Prueba 2: Validación RFC inválido con datos GALA85010 - resultado esperado: hay excepción")
    void validateTaxId_invalidRfcFormat_throwsIllegalArgumentException() {
        // Arrange — RFC con estructura incorrecta (faltan caracteres del homoclave)
        String invalidTaxId = "GALA85010";

        // Act + Assert — debe lanzar IllegalArgumentException con mensaje descriptivo
        assertThatThrownBy(() -> ValidationUtil.validateTaxId(invalidTaxId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RFC");
    }

    @Test
    @DisplayName("Prueba 3: Validación RFC válido no permitido con datos GALA150101AB1 - resultado esperado: hay excepción")
    void validateTaxId_rfcBelongsToMinor_throwsIllegalArgumentException() {
        // Arrange — RFC con fecha de nacimiento de un menor de 18 años (año reciente)
        String minorTaxId = "GALA150101AB1";

        // Act + Assert — debe rechazar por edad mínima no alcanzada
        assertThatThrownBy(() -> ValidationUtil.validateTaxId(minorTaxId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("18");
    }

    @Test
    @DisplayName("Prueba 4: Validación fallida RFC vs Nombre con datos GALA850101AB1 y Carlos Rodríguez Sánchez - resultado esperado: hay excepción")
    void validateTaxId_rfcDoesNotMatchFullName_throwsIllegalArgumentException() {
        // Arrange — RFC cuyas iniciales no coinciden con el nombre proporcionado
        String taxId    = "GALA850101AB1"; // iniciales: G, A, L, A
        String fullName = "Carlos Rodríguez Sánchez"; // iniciales: R, S, C → no coinciden

        // Act + Assert — debe lanzar excepción por inconsistencia RFC / nombre
        assertThatThrownBy(() -> ValidationUtil.validateRfcMatchesName(taxId, fullName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("coincide");
    }

    // =========================================================================
    // taxId uniqueness — unicidad de RFC en lista de usuarios
    // =========================================================================

    @Test
    @DisplayName("Prueba 5: Validación unicidad de RFC GALA850101AB1 - resultado esperado: hay excepción")
    void createUser_duplicateTaxId_throwsIllegalArgumentException() {
        // Arrange — repositorio con un usuario existente cuyo RFC es "GALA850101AB1"
        String duplicateTaxId = "GALA850101AB1";

        User existingUser = new User(
                UUID.randomUUID(),
                "andres.garcia@email.com",
                "Andrés García López",
                "+525512345678",
                "encryptedPassword",
                duplicateTaxId,
                ZonedDateTime.now(ZoneId.of("UTC+3")),
                List.of(new Address(UUID.randomUUID(), "Casa", "Av. Insurgentes 1234", "MX"))
        );

        UserRepository repository = new UserRepository();
        repository.save(existingUser);

        // Nuevo DTO intentando registrar un usuario con el mismo RFC
        UserRequestDto incomingDto = UserRequestDto.builder()
                .email("andres.garay@email.com")
                .name("Andrés Garay López")
                .phone("+525598765432")
                .password("OtraPass@2024")
                .taxId(duplicateTaxId)
                .addresses(List.of())
                .build();

        UserService userService = new UserService(repository);

        // Act + Assert — UserService detecta el RFC duplicado y lanza excepción
        assertThatThrownBy(() -> userService.createUser(incomingDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya está registrado");
    }



    // =========================================================================
    // validatePhone — Estandar E.164
    // =========================================================================

    @Test
    @DisplayName("Prueba 6: Validación número telefónico en E.164 - resultado esperado: no hay excepción")
    void validatePhone_validE164_doesNotThrowException() {
        // Arrange — teléfono con código de país (+XX o +XXX) seguido de exactamente 10 dígitos
        String validPhone = "+525512345678";

        // Act + Assert — no debe lanzar ninguna excepción
        assertThatCode(() -> ValidationUtil.validatePhone(validPhone))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Prueba 7: Validación número telefónico en E.164 código de país faltante - resultado esperado: hay excepción")
    void validatePhone_phoneMissingCountryCode_throwsIllegalArgumentException() {
        // Arrange — teléfono sin el prefijo '+' ni código de país
        String invalidPhone = "5512345678";

        // Act + Assert — debe rechazar por no cumplir el Estandar E.164
        assertThatThrownBy(() -> ValidationUtil.validatePhone(invalidPhone))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("telefono debe tener el formato");
    }
}