package com.example.demo.dto;

import com.example.demo.model.Address;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO de entrada para el endpoint {@code POST /users}.
 *
 * <p>Recibe los datos crudos del cliente. La contraseña llega en texto plano
 * y será cifrada con AES-256 en {@code UserService} antes de persistirse.
 * El {@code taxId} (RFC) es validado por unicidad también en el servicio.</p>
 *
 * <p>Las anotaciones de validación de Bean Validation (Jakarta) son la primera
 * línea de defensa; validaciones de negocio más complejas
 * viven en {@code util/ValidationUtil} y {@code UserService}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestDto {

    /**
     * Correo electrónico del usuario.
     * Debe ser un email con formato válido y no estar en blanco.
     */
    @NotBlank(message = "email es requerido")
    @Email(message = "email debe tener un formato válido")
    private String email;

    /**
     * Nombre completo del usuario.
     */
    @NotBlank(message = "nombre es requerido")
    @Size(min = 2, max = 100, message = "nombre debe tener entre 2 y 100 caracteres")
    private String name;

    /**
     * Teléfono en "E.164: código de país seguido de exactamente 10 dígitos.
     * Ejemplo válido: "+525512345678".
     *
     * <p>El regex valida la estructura básica; la validación semántica completa
     * (código de país real, operadora, etc.) se delega a {@code ValidationUtil}.</p>
     */

    public static final String PHONE_REGEX = "^\\+\\d{1,3}\\d{10}$";

    @NotBlank(message = "telefono es requerido")
    @Pattern(
        regexp  = PHONE_REGEX,
        message = "telefono debe tener el formato: signo más seguido de código de país seguido de 10 dígitos numérticos (ej. +525512345678)"
    )
    private String phone;

    /**
     * Contraseña en texto plano.
     *
     * <p><strong>Nota de seguridad:</strong> Este valor es cifrado con AES-256
     * en {@code UserService} inmediatamente al recibirse. Nunca se persiste
     * ni se retorna en texto plano.</p>
     */
    @NotBlank(message = "contraseña es requerida")
    @Size(min = 4, message = "contraseña debe tener al menos 4 caracteres")
    private String password;

    /**
     * RFC (Registro Federal de Contribuyentes) — identificador fiscal único.
     * Actúa como username para {@code POST /login}.
     *
     * <p>La validación de formato completo (patrón RFC mexicano) y unicidad
     * se realiza en {@code ValidationUtil} y {@code UserService} respectivamente.</p>
     */

    public static final String RFC_REGEX = 
    "^([A-ZÑ]{2})([A-ZÑ]{1})([A-ZÑ]{1})(\\d{2})(\\d{2})(\\d{2})([A-Z0-9]{3})$";

    @NotBlank(message = "RFC es requerido")
    @Pattern(
        regexp  = RFC_REGEX,
        flags   = Pattern.Flag.CASE_INSENSITIVE,
        message = "RFC debe de tener formato válido (e.g. ABCD + DD/MM/AA + XY1)"
    )
    private String taxId;

    /**
     * Lista de direcciones del usuario.
     *
     * <p>{@code @Valid} propaga la validación de Bean Validation a cada
     * elemento {@link Address} de la lista.</p>
     */
    @NotNull(message = "direcciones no puede ser null, en todo caso enviar una lista vacía []")
    @Valid
    @Builder.Default
    private List<Address> addresses = new ArrayList<>();
}