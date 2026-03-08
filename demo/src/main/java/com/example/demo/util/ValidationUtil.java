package com.example.demo.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.demo.dto.UserRequestDto;

/**
 * Clase de utilidad que contiene validaciones a nivel de dominio para la API de Usuarios.
 *
 * <p>Cada método regresa silenciosamente (entrada válida) o lanza una
 * {@link IllegalArgumentException} con un mensaje descriptivo (entrada inválida).
 * Este diseño permite a los llamadores (servicios, controladores) capturar y convertir
 * excepciones en la respuesta HTTP apropiada vía {@code CustomException}.</p>
 *
 * <p>Todos los patrones regex se compilan una vez como constantes {@code static final}
 * para evitar recompilación en cada llamada.</p>
 */
public final class ValidationUtil {

    // -------------------------------------------------------------------------
    // Constructor — clase de utilidad, no instanciable
    // -------------------------------------------------------------------------

    private ValidationUtil() {
        throw new UnsupportedOperationException("ValidationUtil es una clase de utilidad");
    }

    // =========================================================================
    // Validación de RFC (taxId)
    // =========================================================================

    /**
     * Estructura del RFC (ID fiscal mexicano):
     *
     * <pre>
     *  Posición  Longitud  Contenido
     *  ────────  ──────  ─────────────────────────────────────────────────
     *  1–2        2      Primeras dos letras del primer apellido (paterno)
     *  3          1      Primera letra del segundo apellido (materno)
     *  4          1      Primera letra del nombre dado
     *  5–10       6      Fecha de nacimiento YYMMDD
     *  11–13      3      Homoclave (alfanumérica, asignada por SAT)
     * </pre>
     *
     * <p>Longitud total: 13 caracteres para personas físicas.
     * La variante de 4 caracteres (personas morales) está fuera del alcance de esta API.</p>
     */

    /**
     * Edad mínima permitida (en años) derivada de la fecha de nacimiento en el RFC.
     *
     * <p>Actualmente se define en {@value #MINIMUM_AGE_YEARS} años, aunque el SAT permite RFC
     * desde los 16. Para ajustar la validación a 16 años basta con modificar el valor de
     * {@code MINIMUM_AGE_YEARS} y revisar la poca lógica dependiente de esta constante.</p>
     */

    private static final int MINIMUM_AGE_YEARS = 18;

    /**
     * Valida un RFC mexicano (persona física, 13 caracteres).
     *
     * <p>Verificaciones realizadas:</p>
     * <ol>
     *   <li>Validez de fecha: el segmento {@code YYMMDD} debe ser una fecha real del calendario.</li>
     *   <li>Condición de edad: la persona debe tener al menos {@value #MINIMUM_AGE_YEARS} años
     *       en relación a hoy.</li>
     * </ol>
     *
     * @param taxId La cadena RFC a validar. Puede ser mayúsculas/minúsculas; normalizada internamente.
     * @throws IllegalArgumentException estructuralmente inválido,
     *                                  contiene una fecha imposible, o implica que la persona
     *                                  tiene menos de {@value #MINIMUM_AGE_YEARS} años de edad.
     */
    public static void validateTaxId(final String taxId) {

        final String normalized = taxId.trim().toUpperCase();

        final Matcher matcher = Pattern
        .compile(UserRequestDto.RFC_REGEX, Pattern.CASE_INSENSITIVE)
        .matcher(normalized);

        // Groups: 1=pat1 y pat2, 2=mat, 3=name, 4=YY, 5=MM, 6=DD, 7=homoclave
        final String yearPart  = matcher.group(4);
        final String monthPart = matcher.group(5);
        final String dayPart   = matcher.group(6);

        final LocalDate birthDate = parseBirthDate(yearPart, monthPart, dayPart, taxId);
        validateMinimumAge(birthDate, taxId);
    }

    // =========================================================================
    // Validación de nombre completo
    // =========================================================================

    /**
     * Regex para un nombre completo: al menos un token de nombre dado seguido de exactamente
     * dos tokens de apellido, todos separados por espacios simples.
     *
     * <p>Cada token debe comenzar con una letra mayúscula (o acentuada) y puede
     * contener letras (incluyendo vocales acentuadas en español y Ñ), guiones,
     * y apóstrofes (ej. "O'Brien", "María-José").</p>
     *
     * <p>Desglose del patrón:</p>
     * <pre>
     *  ^                     inicio de cadena
     *  [A-ZÁÉÍÓÚÑÜ]          primer token comienza con letra mayúscula
     *  [a-záéíóúñü'-]*       resto del primer token (minúsculas, guiones, apóstrofes)
     *  (?: [A-ZÁÉÍÓÚÑÜ][a-záéíóúñü'-]*)* opcional tokens adicionales de nombre dado
     *   [A-ZÁÉÍÓÚÑÜ][a-záéíóúñü'-]+  apellido paterno (al menos 2 caracteres)
     *   [A-ZÁÉÍÓÚÑÜ][a-záéíóúñü'-]+  apellido materno (al menos 2 caracteres)
     *  $                     fin de cadena
     * </pre>
     */
    private static final Pattern FULL_NAME_PATTERN = Pattern.compile(
            "^[A-ZÁÉÍÓÚÑÜ][a-záéíóúñü'\\-]*(?: [A-ZÁÉÍÓÚÑÜ][a-záéíóúñü'\\-]*)*"
            + " [A-ZÁÉÍÓÚÑÜ][a-záéíóúñü'\\-]+"
            + " [A-ZÁÉÍÓÚÑÜ][a-záéíóúñü'\\-]+$"
    );

    /**
     * Valida que un nombre completo contenga al menos un nombre dado y dos apellidos,
     * separados por espacios simples, todos comenzando con una letra mayúscula.
     *
     * <p>Ejemplos válidos:</p>
     * <ul>
     *   <li>{@code "Juan García López"}</li>
     *   <li>{@code "María José Rodríguez Fernández"}</li>
     *   <li>{@code "Ana O'Brien Ñúñez"}</li>
     * </ul>
     *
     * <p>Ejemplos inválidos:</p>
     * <ul>
     *   <li>{@code "Juan"} — solo un token, sin apellidos</li>
     *   <li>{@code "Juan garcia López"} — apellido comienza con minúscula</li>
     *   <li>{@code "  Juan  García  López  "} — espacios extra</li>
     * </ul>
     *
     * @param fullName La cadena de nombre a validar.
     * @throws IllegalArgumentException si es null, vacío, o no contiene
     *                                  al menos un nombre dado y dos apellidos.
     */
    public static void validateFullName(final String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("fullName no debe ser null o vac´´io");
        }

        final String trimmed = fullName.trim();

        if (!FULL_NAME_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(
                    "fullName '" + trimmed + "' es inválido. "
                    + "Debe contener al menos un nombre y dos apellidos, "
                    + "cada uno comenzando con letra mayúscula y separados por espacios únicos. "
                    + "Ejemplo: \"Juan García López\"");
        }
    }

    // =========================================================================
    // Validación de correspondencia entre RFC y nombre completo
    // =========================================================================

    /**
     * Valida que los primeros cuatro caracteres del RFC coincidan con las iniciales
     * derivadas del nombre completo (dos letras del apellido paterno, una del materno
     * y una del nombre).
     *
     * <p>Ejemplo:</p>
     * <ul>
     *   <li>Nombre: {@code "Juan García López"} → Iniciales: {@code "GALJ"}</li>
     *   <li>RFC esperado: {@code GALJ850101XXX}</li>
     * </ul>
     *
     * @param taxId    RFC a validar.
     * @param fullName Nombre completo a validar contra el RFC.
     * @throws IllegalArgumentException si las iniciales del nombre no coinciden con
     *                                  los primeros cuatro caracteres del RFC.
     */
    public static void validateRfcMatchesName(final String taxId, final String fullName) {

        final String normalizedRfc = taxId.trim().toUpperCase();
        final String trimmedName   = fullName.trim();

        final String[] tokens = trimmedName.split(" ");

        final String apellidoPaterno = tokens[tokens.length - 2];
        final String apellidoMaterno = tokens[tokens.length - 1];
        final String nombre          = tokens[0];

        final String expectedPrefix =
            apellidoPaterno.substring(0, Math.min(2, apellidoPaterno.length())).toUpperCase()
            + apellidoMaterno.substring(0, 1).toUpperCase()
            + nombre.substring(0, 1).toUpperCase();

        final String actualPrefix = normalizedRfc.substring(0, 4);

        if (!expectedPrefix.equals(actualPrefix)) {
            throw new IllegalArgumentException(
                "El RFC '" + taxId + "' no coincide con las iniciales derivadas del nombre '" 
                + fullName + "'. Se esperaba prefijo '" + expectedPrefix + "'."
            );
        }

    }  

    // =========================================================================
    // Auxiliares privados
    // =========================================================================

    /**
     * Interpreta el año de dos dígitos usando una ventana deslizante:
     * años 00–29 → 2000–2029, años 30–99 → 1930–1999.
     *
     * <p>Esto coincide con la convención SAT RFC para fechas de nacimiento
     * y evita clasificar erróneamente un nacimiento en 1985 como 2085.</p>
     */
    private static LocalDate parseBirthDate(
            final String yy,
            final String mm,
            final String dd,
            final String originalTaxId
    ) {
        final int twoDigitYear = Integer.parseInt(yy);
        final int fullYear     = (twoDigitYear <= 29) ? 2000 + twoDigitYear : 1900 + twoDigitYear;
        final String isoDate   = String.format("%04d-%s-%s", fullYear, mm, dd);

        try {
            // STRICT resolver rejects dates like Feb 30
            return LocalDate.parse(
                    isoDate,
                    DateTimeFormatter.ofPattern("uuuu-MM-dd")
                            .withResolverStyle(ResolverStyle.STRICT)
            );
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "El taxId '" + originalTaxId + "' contiene un segmento de fecha de nacimiento inválido '"
                    + yy + mm + dd + "' (interpretado como " + isoDate + "). "
                    + "La fecha no existe en el calendario.");
        }
    }

    /**
     * Asegura que la persona derivada del RFC tenga al menos {@value #MINIMUM_AGE_YEARS} años de edad.
     *
     * @param birthDate      Fecha de nacimiento analizada.
     * @param originalTaxId  Cadena RFC original (usada en el mensaje de error).
     * @throws IllegalArgumentException si la persona tiene menos de la edad mínima.
     */
    private static void validateMinimumAge(
            final LocalDate birthDate,
            final String originalTaxId
    ) {
        final LocalDate minimumBirthDate = LocalDate.now().minusYears(MINIMUM_AGE_YEARS);

        if (birthDate.isAfter(minimumBirthDate)) {
            throw new IllegalArgumentException(
                    "taxId '" + originalTaxId + "' implica una fecha de nacimiento de " + birthDate
                    + ", lo que significa que la persona tiene menos de " + MINIMUM_AGE_YEARS + " años de edad. "
                    + "Los usuarios deben tener al menos " + MINIMUM_AGE_YEARS + " años.");
        }
    }
}