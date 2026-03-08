package com.example.demo.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Clase de utilidad para cifrado y descifrado AES-256 usando modo GCM.
 *
 * <p><strong>Algoritmo:</strong> AES/GCM/NoPadding</p>
 * <ul>
 *   <li>Tamaño de clave: 256 bits</li>
 *   <li>Tamaño de IV: 12 bytes (96 bits) — recomendado para GCM</li>
 *   <li>Tamaño de etiqueta de autenticación: 128 bits — máximo, proporciona integridad + autenticidad</li>
 * </ul>
 *
 * <p>El IV se genera aleatoriamente en cada llamada a {@link #encrypt} y se antepone
 * al texto cifrado antes de la codificación Base64, por lo que {@link #decrypt} puede extraerlo
 * sin necesidad de un campo separado.</p>
 *
 * <p><strong>Disposición de la carga útil codificada (Base64):</strong>
 * {@code [ IV (12 bytes) | Texto cifrado + Etiqueta de autenticación GCM ]}</p>
 *
 * <p><strong>Nota de producción:</strong> La clave secreta se almacena como una constante
 * por simplicidad. En un entorno de producción debe inyectarse vía
 * variable de entorno o un gestor de secretos (ej. AWS Secrets Manager, Vault).</p>
 */
public final class Aes256Util {

    // -------------------------------------------------------------------------
    // Constantes criptográficas
    // -------------------------------------------------------------------------

    private static final String ALGORITHM        = "AES";
    private static final String TRANSFORMATION   = "AES/GCM/NoPadding";
    private static final int    KEY_SIZE_BITS     = 256;
    private static final int    IV_SIZE_BYTES     = 12;   // 96 bits — GCM recommended
    private static final int    TAG_SIZE_BITS     = 128;  // maximum auth tag length

    /**
     * Clave secreta de 256 bits (32 bytes) codificada en Base64.
     *
     * <p>⚠️ Reemplaza con un valor cargado desde una variable de entorno o gestor de secretos
     * antes de desplegar a cualquier entorno no local.</p>
     *
     * <p>Genera una nueva clave con:
     * {@code openssl rand -base64 32}</p>
     */
    private static final String SECRET_KEY_BASE64 =
            "dGhpcytpcy1hLXNlY3JldC1rZXktZm9yLWFlcy0yNTY="; // 32 bytes

    /** {@link SecretKey} analizada derivada una vez de {@link #SECRET_KEY_BASE64}. */
    private static final SecretKey SECRET_KEY = buildSecretKey();

    // -------------------------------------------------------------------------
    // Constructor — clase de utilidad, no instanciable
    // -------------------------------------------------------------------------

    private Aes256Util() {
        throw new UnsupportedOperationException("Aes256Util es una clase de utilidad");
    }

    // -------------------------------------------------------------------------
    // API pública
    // -------------------------------------------------------------------------

    /**
     * Cifra una cadena de texto plano usando AES-256-GCM.
     *
     * <p>Un IV aleatorio fresco se genera para cada llamada, asegurando que cifrar
     * el mismo valor dos veces produzca textos cifrados diferentes (seguridad semántica).</p>
     *
     * @param plainText La cadena a cifrar. No debe ser {@code null} o vacía.
     * @return Cadena codificada en Base64 conteniendo {@code IV || texto cifrado+etiqueta}.
     * @throws IllegalArgumentException si {@code plainText} es {@code null} o vacía.
     * @throws EncryptionException      si la operación JCA falla inesperadamente.
     */
    public static String encrypt(final String plainText) {
        if (plainText == null || plainText.isBlank()) {
            throw new IllegalArgumentException("plainText no deber ser null o vacío");
        }

        try {
            final byte[] iv = generateIv();

            final Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY, new GCMParameterSpec(TAG_SIZE_BITS, iv));

            final byte[] cipherTextBytes = cipher.doFinal(plainText.getBytes("UTF-8"));

            // Prepend IV to ciphertext so decrypt() can recover it
            final byte[] payload = ByteBuffer.allocate(iv.length + cipherTextBytes.length)
                    .put(iv)
                    .put(cipherTextBytes)
                    .array();

            return Base64.getEncoder().encodeToString(payload);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new EncryptionException("Encriptar el valor falló", e);
        }
    }

    /**
     * Descifra un texto cifrado AES-256-GCM codificado en Base64 producido por {@link #encrypt}.
     *
     * @param encryptedText Cadena codificada en Base64 ({@code IV || texto cifrado+etiqueta}).
     *                      No debe ser {@code null} o vacía.
     * @return La cadena de texto plano original.
     * @throws IllegalArgumentException si {@code encryptedText} es {@code null} o vacía,
     *                                  o si la carga útil es demasiado corta para contener un IV válido.
     * @throws EncryptionException      si el descifrado o autenticación falla.
     */
    public static String decrypt(final String encryptedText) {
        if (encryptedText == null || encryptedText.isBlank()) {
            throw new IllegalArgumentException("encryptedText no deber ser null o vacío");
        }

        try {
            final byte[] payload = Base64.getDecoder().decode(encryptedText);

            if (payload.length <= IV_SIZE_BYTES) {
                throw new IllegalArgumentException(
                        "Carga de encriptación demsiado corta para contener un IV válido");
            }

            // Extract IV from the first IV_SIZE_BYTES bytes
            final ByteBuffer buffer         = ByteBuffer.wrap(payload);
            final byte[]      iv            = new byte[IV_SIZE_BYTES];
            final byte[]      cipherTextBytes = new byte[payload.length - IV_SIZE_BYTES];

            buffer.get(iv);
            buffer.get(cipherTextBytes);

            final Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, SECRET_KEY, new GCMParameterSpec(TAG_SIZE_BITS, iv));

            final byte[] plainBytes = cipher.doFinal(cipherTextBytes);
            return new String(plainBytes, "UTF-8");

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new EncryptionException("No se pudo desencriptar el valor — los datos pueden estar corruptos o alterados", e);
        }
    }

    // -------------------------------------------------------------------------
    // Auxiliares privados
    // -------------------------------------------------------------------------

    /** Construye la {@link SecretKey} una vez desde la constante codificada en Base64. */
    private static SecretKey buildSecretKey() {
        final byte[] keyBytes = Base64.getDecoder().decode(SECRET_KEY_BASE64);
        if (keyBytes.length * 8 != KEY_SIZE_BITS) {
            throw new IllegalStateException(
                    "La clave secreta debe tener exactamente " + KEY_SIZE_BITS + " bits ("
                    + (KEY_SIZE_BITS / 8) + " bytes); se obtuvieron " + keyBytes.length + " bytes");
        }
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    /** Genera un IV aleatorio criptográficamente de 12 bytes. */
    private static byte[] generateIv() {
        final byte[] iv = new byte[IV_SIZE_BYTES];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    // -------------------------------------------------------------------------
    // Excepción anidada — evita filtrar excepciones criptográficas verificadas a los invocadores
    // -------------------------------------------------------------------------

    /**
     * Excepción no verificada lanzada cuando una operación AES-256-GCM falla.
     *
     * <p>Envuelve la {@link Exception} subyacente para que los llamadores no estén obligados a
     * manejar excepciones criptográficas verificadas mientras se preserva la causa raíz.</p>
     */
    public static final class EncryptionException extends RuntimeException {

        public EncryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}