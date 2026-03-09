package com.example.demo.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class Aes256UtilTest {

    // =========================================================================
    // encrypt — cifrado AES-256-GCM
    // =========================================================================

    @Test
    @DisplayName("Prueba 1: el encriptado genera cadena de texto diferente a contraseña plana")
    void encryptPassword_shouldReturnDifferentStringThanInput() {
        // Arrange — contraseña en texto plano
        String plainPassword = "MiContraseña@2024";

        // Act — cifrar la contraseña
        String encryptedPassword = Aes256Util.encrypt(plainPassword);

        // Assert — el resultado no debe ser igual al texto original ni estar vacío
        assertThat(encryptedPassword)
                .isNotNull()
                .isNotBlank()
                .isNotEqualTo(plainPassword);
    }

    // =========================================================================
    // encrypt + decrypt — consistencia del ciclo completo
    // =========================================================================

    @Test
    @DisplayName("Prueba 2: el encriptado y desencriptado son consistentes")
    void encryptAndDecrypt_shouldBeConsistent() {
        // Arrange — contraseña original en texto plano
        String originalPassword = "MiContraseña@2024";

        // Act — cifrar y luego descifrar
        String encryptedPassword = Aes256Util.encrypt(originalPassword);
        String decryptedPassword = Aes256Util.decrypt(encryptedPassword);

        // Assert — el texto descifrado debe ser idéntico al original
        assertThat(decryptedPassword)
                .isNotNull()
                .isEqualTo(originalPassword);
    }
}
