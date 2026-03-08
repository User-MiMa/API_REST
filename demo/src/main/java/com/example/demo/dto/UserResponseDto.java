package com.example.demo.dto;

import com.example.demo.model.Address;
import com.example.demo.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * DTO de salida utilizado en todas las respuestas REST que involucren usuarios.
 *
 * <p><strong>Regla de oro:</strong> {@code password} está deliberadamente ausente.
 * Este DTO es la única representación de {@link User} que cruza el límite HTTP,
 * garantizando que la contraseña (cifrada o no) nunca llegue al cliente.</p>
 *
 * <p>{@code createdAt} se serializa como {@code String} con el formato
 * {@code dd-MM-yyyy HH:mm} en zona horaria de Madagascar (Africa/Antananarivo, UTC+3),
 * siguiendo el requerimiento del sistema.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {

    // -------------------------------------------------------------------------
    // Constantes de formato — zona horaria y patrón de fecha de Madagascar
    // -------------------------------------------------------------------------

    /** Zona horaria oficial de Madagascar. */
    /*private static final ZoneId MADAGASCAR_TZ = ZoneId.of("Africa/Antananarivo");*/
    private static final ZoneId MADAGASCAR_TZ = ZoneId.of("UTC+3");


    /** Formato de fecha requerido: día-mes-año hora:minuto. */
    private static final DateTimeFormatter CREATED_AT_FORMATTER =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm").withZone(MADAGASCAR_TZ);

    // -------------------------------------------------------------------------
    // Campos expuestos al cliente
    // -------------------------------------------------------------------------

    /** Identificador único del usuario. */
    private UUID id;

    /** Correo electrónico. */
    private String email;

    /** Nombre completo. */
    private String name;

    /** Teléfono en AndresFormat. */
    private String phone;

    /**
     * RFC (identificador fiscal único).
     * Sirve como username visible; nunca se oculta en la respuesta.
     */
    private String taxId;

    /**
     * Fecha y hora de creación formateada como {@code dd-MM-yyyy HH:mm}
     * en zona horaria de Madagascar.
     *
     * <p>Se almacena como {@code String} para evitar que el serializador JSON
     * reintroduzca información de zona que pudiera confundir al consumidor.</p>
     */
    private String createdAt;

    /** Lista de direcciones del usuario. */
    private List<Address> addresses;

    // -------------------------------------------------------------------------
    // Método de mapeo estático
    // -------------------------------------------------------------------------

    /**
     * Construye un {@code UserResponseDto} a partir de la entidad {@link User}.
     *
     * <p>Este método centraliza el mapeo modelo → DTO, manteniendo la capa de
     * presentación desacoplada del dominio. Si en el futuro se adopta MapStruct,
     * este método puede convivir o ser reemplazado sin afectar a los controladores.</p>
     *
     * @param user Entidad de dominio origen. No debe ser {@code null}.
     * @return DTO listo para serializar y enviar al cliente.
     * @throws IllegalArgumentException si {@code user} es {@code null}.
     */
    public static UserResponseDto from(final User user) {
        if (user == null) {
            throw new IllegalArgumentException("Cannot map a null User to UserResponseDto");
        }

        return UserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .taxId(user.getTaxId())
                .createdAt(
                    user.getCreatedAt() != null
                        ? CREATED_AT_FORMATTER.format(user.getCreatedAt())
                        : null
                )
                .addresses(user.getAddresses())
                .build();
    }
}