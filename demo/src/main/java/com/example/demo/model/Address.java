package com.example.demo.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio que representa una dirección asociada a un usuario.
 *
 * <p>Cada dirección tiene un identificador único (UUID), un nombre descriptivo,
 * la calle y el código de país (ISO 3166-1 alpha-2, ej. "MX", "US").</p>
 */
public class Address {

    // -------------------------------------------------------------------------
    // Atributos
    // -------------------------------------------------------------------------

    /** Identificador único de la dirección. */
    private UUID id;

    /** Nombre o alias de la dirección (ej. "Casa", "Oficina"). */
    private String name;

    /** Calle, número exterior e interior. */
    private String street;

    /**
     * Código de país en formato ISO 3166-1 alpha-2 (ej. "MX", "US", "MG").
     * Se almacena siempre en mayúsculas.
     */
    private String countryCode;

    // -------------------------------------------------------------------------
    // Constructores
    // -------------------------------------------------------------------------

    /** Constructor vacío requerido para deserialización (Jackson, etc.). */
    public Address() {}

    /**
     * Constructor completo.
     *
     * @param id          Identificador único. Si es {@code null}, se genera uno automáticamente.
     * @param name        Nombre o alias de la dirección.
     * @param street      Calle y número.
     * @param countryCode Código de país ISO 3166-1 alpha-2.
     */
    public Address(UUID id, String name, String street, String countryCode) {
        this.id          = (id != null) ? id : UUID.randomUUID();
        this.name        = name;
        this.street      = street;
        setCountryCode(countryCode); // normalización en setter
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCountryCode() {
        return countryCode;
    }

    /**
     * Normaliza el código de país a mayúsculas antes de almacenarlo.
     *
     * <p>La validación de formato (longitud, caracteres permitidos) se delega
     * a {@code util/ValidationUtil} para mantener la responsabilidad única.</p>
     *
     * @param countryCode Código ISO 3166-1 alpha-2 (ej. "mx" → se guarda como "MX").
     */
    public void setCountryCode(String countryCode) {
        this.countryCode = (countryCode != null) ? countryCode.toUpperCase() : null;
    }

    // -------------------------------------------------------------------------
    // equals / hashCode — basados en `id` (clave natural de la entidad)
    // -------------------------------------------------------------------------

    /**
     * Dos direcciones son iguales si comparten el mismo {@code id}.
     * Esto es fundamental para operaciones de búsqueda y actualización en listas.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Address other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // -------------------------------------------------------------------------
    // toString — excluye datos sensibles (ninguno en Address, pero por buena práctica)
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "Address{" +
                "id="          + id          +
                ", name='"     + name        + '\'' +
                ", street='"   + street      + '\'' +
                ", countryCode='" + countryCode + '\'' +
                '}';
    }
}