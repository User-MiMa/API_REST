package com.example.demo.model;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio que representa a un usuario del sistema.
 *
 * <p><strong>Decisiones de diseño:</strong></p>
 * <ul>
 *   <li>{@code password} se almacena cifrado con AES-256 (delegado a {@code util/Aes256Util}).</li>
 *   <li>{@code password} <em>nunca</em> debe incluirse en ningún {@code UserResponseDto}.</li>
 *   <li>{@code taxId} (RFC) es único por usuario; la unicidad se valida en {@code UserService}.</li>
 *   <li>{@code createdAt} se guarda como {@link ZonedDateTime} con zona Africa/Antananarivo
 *       (Madagascar, UTC+3). El formateado "dd-MM-yyyy HH:mm" se realiza en la capa de
 *       presentación ({@code UserResponseDto}).</li>
 *   <li>{@code equals}/{@code hashCode} están basados en {@code id} para operaciones
 *       correctas sobre listas (filtrado, actualización, eliminación).</li>
 * </ul>
 */
public class User {

    // -------------------------------------------------------------------------
    // Atributos
    // -------------------------------------------------------------------------

    /** Identificador único del usuario (UUID v4). */
    private UUID id;

    /** Correo electrónico del usuario. Debe ser único (validado en servicio). */
    private String email;

    /** Nombre completo del usuario. */
    private String name;

    /**
     * Teléfono en "E.164: código de país + 10 dígitos.
     * Ejemplo: "+52 5512345678" o la convención definida en {@code UserRequestDto}.
     * La validación de formato se delega a {@code dto/UserRequestDto}.
     */
    private String phone;

    /**
     * Contraseña cifrada con AES-256.
     *
     * <p><strong>IMPORTANTE:</strong> Este campo JAMÁS debe ser expuesto
     * en ninguna respuesta REST. El cifrado/descifrado es responsabilidad
     * de {@code util/Aes256Util}.</p>
     */
    private String password;

    /**
     * RFC (Registro Federal de Contribuyentes) — identificador fiscal único.
     * Actúa también como <em>username</em> para el endpoint {@code POST /login}.
     * La validación de formato y unicidad se realiza en la capa de servicio.
     */
    private String taxId;

    /**
     * Marca de tiempo de creación en zona horaria de Madagascar (Africa/Antananarivo, UTC+3).
     * Se persiste como {@link ZonedDateTime} para preservar la zona; el formato
     * "dd-MM-yyyy HH:mm" se aplica al serializar en {@code UserResponseDto}.
     */
    private ZonedDateTime createdAt;

    /**
     * Lista de direcciones asociadas al usuario.
     * Inicializada vacía para evitar {@code NullPointerException} al agregar elementos.
     */
    private List<Address> addresses = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Constructores
    // -------------------------------------------------------------------------

    /** Constructor vacío requerido para deserialización (Jackson, etc.). */
    public User() {}

    /**
     * Constructor completo.
     *
     * @param id        Identificador único. Si es {@code null}, se genera uno automáticamente.
     * @param email     Correo electrónico.
     * @param name      Nombre completo.
     * @param phone     Teléfono en E.164.
     * @param password  Contraseña ya cifrada en AES-256.
     * @param taxId     RFC único del usuario.
     * @param createdAt Timestamp de creación (zona Africa/Antananarivo).
     * @param addresses Lista de direcciones (puede ser {@code null}; se normaliza a lista vacía).
     */
    
    public User(
            UUID id,
            String email,
            String name,
            String phone,
            String password,
            String taxId,
            ZonedDateTime createdAt,
            List<Address> addresses
    ) {
        this.id        = (id != null) ? id : UUID.randomUUID();
        this.email     = email;
        this.name      = name;
        this.phone     = phone;
        this.password  = password;
        this.taxId     = taxId;
        this.createdAt = createdAt;
        this.addresses = (addresses != null) ? addresses : new ArrayList<>();
    }

    // -------------------------------------------------------------------------
    // Getters y Setters
    // -------------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    /** Si se recibe {@code null}, genera un UUID automáticamente. */
    public void setId(UUID id) {
        this.id = (id != null) ? id : UUID.randomUUID();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    /**
     * Almacena el teléfono tal como viene.
     * La validación de formato "E.164" (country code + 10 dígitos) se delega
     * a {@code dto/UserRequestDto}.
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Retorna la contraseña cifrada.
     *
     * <p><strong>Nunca</strong> incluir este valor en respuestas REST.
     * Usar {@code UserResponseDto} que omite este campo.</p>
     */
    public String getPassword() {
        return password;
    }

    /**
     * Almacena la contraseña. Se espera que el valor ya esté cifrado con AES-256
     * antes de llamar a este setter (responsabilidad de {@code util/Aes256Util}).
     */
    public void setPassword(String password) {
        this.password = password;
    }

    public String getTaxId() {
        return taxId;
    }

    /**
     * Normaliza el RFC a mayúsculas antes de almacenarlo.
     * La validación de formato y unicidad se delega al servicio.
     */
    public void setTaxId(String taxId) {
        this.taxId = (taxId != null) ? taxId.toUpperCase() : null;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    /** Normaliza a lista vacía si se pasa {@code null}. */
    public void setAddresses(List<Address> addresses) {
        this.addresses = (addresses != null) ? addresses : new ArrayList<>();
    }

    // -------------------------------------------------------------------------
    // equals / hashCode — basados en `id` (clave natural de la entidad)
    // -------------------------------------------------------------------------

    /**
     * Dos usuarios son iguales si comparten el mismo {@code id}.
     *
     * <p>Basar equals/hashCode en {@code id} (y no en campos mutables como email o taxId)
     * garantiza comportamiento correcto en colecciones ({@code List}, {@code Set}, {@code Map})
     * incluso después de actualizar atributos del usuario.</p>
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // -------------------------------------------------------------------------
    // toString — excluye `password` por seguridad
    // -------------------------------------------------------------------------

    /**
     * Representación de texto del usuario.
     *
     * <p><strong>El campo {@code password} está deliberadamente excluido</strong>
     * para evitar que aparezca en logs u otras salidas de texto.</p>
     */
    @Override
    public String toString() {
        return "User{" +
                "id="          + id                    +
                ", email='"    + email                 + '\'' +
                ", name='"     + name                  + '\'' +
                ", phone='"    + phone                 + '\'' +
                ", password='" + "[PROTECTED]"         + '\'' +
                ", taxId='"    + taxId                 + '\'' +
                ", createdAt=" + createdAt             +
                ", addresses=" + addresses             +
                '}';
    }
}