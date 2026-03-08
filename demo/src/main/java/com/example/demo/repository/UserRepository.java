package com.example.demo.repository;

import com.example.demo.model.Address;
import com.example.demo.model.User;
import com.example.demo.util.Aes256Util;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Repositorio en memoria para la entidad {@link User}.
 *
 * <p>Actúa como capa de persistencia simulada durante el desarrollo.
 * Puede reemplazarse por una implementación JPA/MongoDB sin modificar
 * las capas superiores (servicio, controlador).</p>
 * <p>Se usa {@link CopyOnWriteArrayList} para garantizar acceso seguro
 * en entornos con múltiples hilos (thread-safe reads + writes).</p>
 */
public class UserRepository {

    // Zona horaria de Madagascar requerida por el sistema
    private static final ZoneId MADAGASCAR_TZ = ZoneId.of("UTC+3");

    // Almacenamiento principal en memoria, thread-safe
    private final List<User> storage = new CopyOnWriteArrayList<>(seedUsers());

    // -------------------------------------------------------------------------
    // Métodos públicos del repositorio
    // -------------------------------------------------------------------------

    /**
     * Retorna una copia inmutable de todos los usuarios almacenados.
     * Se retorna copia para evitar modificaciones externas al estado interno.
     */
    public List<User> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(storage));
    }

    /**
     * Busca un usuario por su identificador único (UUID).
     *
     * @param id UUID del usuario a buscar.
     * @return {@link Optional} con el usuario si existe, vacío si no.
     */
    public Optional<User> findById(final UUID id) {
        if (id == null) return Optional.empty();

        return storage.stream()
                .filter(user -> id.equals(user.getId()))
                .findFirst();
    }

    /**
     * Busca un usuario por su RFC (taxId).
     * La comparación se hace en mayúsculas para garantizar unicidad sin distinción de caso.
     *
     * @param taxId RFC del usuario a buscar.
     * @return {@link Optional} con el usuario si existe, vacío si no.
     */
    public Optional<User> findByTaxId(final String taxId) {
        if (taxId == null || taxId.isBlank()) return Optional.empty();

        final String normalizedTaxId = taxId.toUpperCase();
        return storage.stream()
                .filter(user -> normalizedTaxId.equals(user.getTaxId()))
                .findFirst();
    }

    /**
     * Verifica si ya existe un usuario registrado con el RFC proporcionado.
     * Útil para validar unicidad antes de crear o actualizar usuarios.
     *
     * @param taxId RFC a verificar.
     * @return {@code true} si el RFC ya está registrado, {@code false} en caso contrario.
     */
    public boolean existsByTaxId(final String taxId) {
        return findByTaxId(taxId).isPresent();
    }

    /**
     * Guarda o actualiza un usuario en el repositorio.
     *
     * <ul>
     *   <li>Si ya existe un usuario con el mismo {@code id}, se reemplaza (update).</li>
     *   <li>Si no existe, se agrega al final de la lista (insert).</li>
     * </ul>
     *
     * <p>La operación de reemplazo se realiza de forma atómica mediante
     * {@code removeIf} + {@code add} para mantener consistencia.</p>
     *
     * @param user Usuario a persistir. No debe ser {@code null}.
     * @throws IllegalArgumentException si {@code user} es {@code null}.
     */
    public void save(final User user) {
        if (user == null) {
            throw new IllegalArgumentException("El usuario a guardar no puede ser null");
        }

        System.out.println("**********************************************************************RFC guardado: " + user.getTaxId());


        // Elimina versión anterior si existe (update), luego inserta
        storage.removeIf(existing -> existing.getId().equals(user.getId()));
        storage.add(user);
    }

    /**
     * Elimina el usuario con el UUID proporcionado.
     * Si no existe ningún usuario con ese id, la operación no tiene efecto.
     *
     * @param id UUID del usuario a eliminar.
     */
    public void deleteById(final UUID id) {
        if (id == null) return;
        storage.removeIf(user -> id.equals(user.getId()));
    }

    // -------------------------------------------------------------------------
    // Datos semilla — 3 usuarios iniciales
    // -------------------------------------------------------------------------

    /**
     * Genera la lista de usuarios iniciales que se cargan al arrancar la aplicación.
     * Las contraseñas se cifran con AES-256 antes de almacenarse.
     * Los timestamps se generan en zona horaria de Madagascar (UTC+3).
     */
    private static List<User> seedUsers() {
        final List<User> users = new ArrayList<>();

        // ── Usuario 1 ─────────────────────────────────────────────────────────
        final User andres = new User(
                UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"),
                "andres.garcia@email.com",
                "Andrés García López",
                "+525512345678",
                Aes256Util.encrypt("Secreta@2024"),
                "GALA850101AB1",
                ZonedDateTime.of(2024, 1, 15, 9, 30, 0, 0, MADAGASCAR_TZ),
                List.of(
                        new Address(UUID.randomUUID(), "Casa",    "Av. Insurgentes Sur 1234, Col. Del Valle", "MX"),
                        new Address(UUID.randomUUID(), "Oficina", "Paseo de la Reforma 505, Piso 8",          "MX")
                )
        );

        // ── Usuario 2 ─────────────────────────────────────────────────────────
        final User sofia = new User(
                UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901"),
                "sofia.martinez@email.com",
                "Sofía Martínez Hernández",
                "+525598765432",
                Aes256Util.encrypt("Clave$Segura99"),
                "MAHS920315CD2",
                ZonedDateTime.of(2024, 3, 22, 14, 0, 0, 0, MADAGASCAR_TZ),
                List.of(
                        new Address(UUID.randomUUID(), "Casa",  "Calle Madero 77, Centro Histórico",    "MX"),
                        new Address(UUID.randomUUID(), "Local", "Plaza Satélite Local 22, Naucalpan",   "MX")
                )
        );

        // ── Usuario 3 ─────────────────────────────────────────────────────────
        final User carlos = new User(
                UUID.fromString("c3d4e5f6-a7b8-9012-cdef-123456789012"),
                "carlos.rodriguez@email.com",
                "Carlos Rodríguez Sánchez",
                "+525544332211",
                Aes256Util.encrypt("P@ssw0rd!Carlos"),
                "ROSC880720EF3",
                ZonedDateTime.of(2024, 6, 10, 8, 15, 0, 0, MADAGASCAR_TZ),
                List.of(
                        new Address(UUID.randomUUID(), "Casa",      "Blvd. Manuel Ávila Camacho 32",   "MX"),
                        new Address(UUID.randomUUID(), "Gimnasio",  "Calle Leibnitz 10, Anzures",       "MX"),
                        new Address(UUID.randomUUID(), "Oficina",   "Torre Reforma 115, Piso 21",       "MX")
                )
        );

        users.add(andres);
        users.add(sofia);
        users.add(carlos);

        return users;
    }
}