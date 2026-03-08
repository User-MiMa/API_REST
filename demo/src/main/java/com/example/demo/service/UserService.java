package com.example.demo.service;

import com.example.demo.dto.UserRequestDto;
import com.example.demo.dto.UserResponseDto;
import com.example.demo.model.Address;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.Aes256Util;
import com.example.demo.util.ValidationUtil;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Capa de servicio que centraliza toda la lógica de negocio relacionada con usuarios.
 *
 * <p>Actúa como intermediario entre el controlador ({@code UserController}) y el
 * repositorio ({@code UserRepository}), aplicando validaciones de negocio,
 * cifrado de contraseñas y transformaciones de datos.</p>
 */
@Service
public class UserService {

    // Zona horaria oficial de Madagascar (UTC+3)
    private static final ZoneId MADAGASCAR_TZ = ZoneId.of("UTC+3");

    // Separador del parámetro de filtro: "atributo+operador+valor"
    private static final String FILTER_DELIMITER = "\\+";

    private final UserRepository userRepository;

    public UserService(final UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // =========================================================================
    // 1. Obtener usuarios — con soporte de filtrado y ordenamiento dinámico
    // =========================================================================

    /**
     * Retorna la lista de usuarios aplicando opcionalmente filtro y/u ordenamiento.
     *
     * <p>El parámetro {@code filter} sigue el formato: {@code atributo+operador+valor}.
     * Operadores soportados: {@code co} (contains), {@code eq} (equals),
     * {@code sw} (startsWith), {@code ew} (endsWith).</p>
     *
     * <p>El parámetro {@code sortedBy} acepta: {@code id}, {@code email}, {@code name},
     * {@code phone}, {@code taxId}, {@code createdAt}.</p>
     *
     * @param sortedBy Atributo por el que se desea ordenar. Puede ser {@code null}.
     * @param filter   Expresión de filtro. Puede ser {@code null}.
     * @return Lista de {@link UserResponseDto} filtrada y/o ordenada.
     */
    public List<UserResponseDto> getUsers(final String sortedBy, final String filter) {
        List<User> users = userRepository.findAll();

        // Aplicar filtro si se proporcionó
        if (filter != null && !filter.isBlank()) {
            final Predicate<User> predicate = buildFilterPredicate(filter);
            users = users.stream().filter(predicate).collect(Collectors.toList());
        }

        // Aplicar ordenamiento si se proporcionó
        if (sortedBy != null && !sortedBy.isBlank()) {
            final Comparator<User> comparator = resolveComparator(sortedBy.trim().toLowerCase());
            users = users.stream().sorted(comparator).collect(Collectors.toList());
        }

        return users.stream()
                .map(UserResponseDto::from)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // 2. Crear usuario
    // =========================================================================

    /**
     * Crea un nuevo usuario aplicando validaciones de negocio, cifrando la contraseña
     * y asignando los valores generados (UUID, createdAt).
     *
     * <p>Validaciones de negocio aplicadas (las de formato ya fueron resueltas por
     * Bean Validation en el DTO y en el Util):</p>
     * <ul>
     *   <li>Fecha de nacimiento válida y edad mínima (via RFC).</li>
     *   <li>Unicidad del {@code taxId}.</li>
     * </ul>
     *
     * @param dto Datos del nuevo usuario. No debe ser {@code null}.
     * @return {@link UserResponseDto} del usuario recién creado.
     * @throws IllegalArgumentException si alguna validación de negocio falla.
     */
    public UserResponseDto createUser(final UserRequestDto dto) {
        // Validaciones de negocio que van más allá del formato (Bean Validation ya cubrió el resto)
        ValidationUtil.validateTaxId(dto.getTaxId());
        ValidationUtil.validatePhone(dto.getPhone());
        ValidationUtil.validateFullName(dto.getName());

        // Verificar unicidad del RFC en el repositorio
        ensureTaxIdIsUnique(dto.getTaxId(), null);

        final User user = new User(
                UUID.randomUUID(),
                dto.getEmail(),
                dto.getName(),
                dto.getPhone(),
                Aes256Util.encrypt(dto.getPassword()), 
                dto.getTaxId().toUpperCase(),
                ZonedDateTime.now(MADAGASCAR_TZ),
                dto.getAddresses()
        );

        userRepository.save(user);

        return UserResponseDto.from(user);
    }

    // =========================================================================
    // 3. Actualización parcial de usuario (PATCH)
    // =========================================================================

    /**
     * Aplica una actualización parcial sobre el usuario identificado por {@code id}.
     *
     * <p>Solo se actualizan los campos presentes en el mapa {@code updates}.
     * Campos soportados: {@code email}, {@code name}, {@code phone}, {@code password},
     * {@code taxId}, {@code addresses}.</p>
     *
     * <p>Si se actualiza {@code taxId} o {@code phone}, se re-ejecutan las
     * validaciones de negocio correspondientes.</p>
     *
     * @param id      UUID del usuario a actualizar.
     * @param updates Mapa de campos a actualizar con sus nuevos valores.
     * @return {@link UserResponseDto} con los datos actualizados.
     * @throws IllegalArgumentException si el usuario no existe o alguna validación falla.
     */
    public UserResponseDto updateUser(final UUID id, final Map<String, Object> updates) {
        final User user = findUserOrThrow(id);

        applyUpdates(user, updates);

        userRepository.save(user);

        return UserResponseDto.from(user);
    }

    // =========================================================================
    // 4. Eliminar usuario
    // =========================================================================

    /**
     * Elimina el usuario con el {@code id} proporcionado.
     *
     * @param id UUID del usuario a eliminar.
     * @throws IllegalArgumentException si no existe un usuario con ese {@code id}.
     */
    public void deleteUser(final UUID id) {
        // Verificar existencia antes de eliminar para retornar error descriptivo
        findUserOrThrow(id);
        userRepository.deleteById(id);
    }

    // =========================================================================
    // 5. Login
    // =========================================================================

    /**
     * Autentica a un usuario usando su {@code taxId} (RFC) como username y su contraseña.
     *
     * <p>La contraseña almacenada está cifrada; se descifra en memoria solo para
     * la comparación y nunca se persiste ni retorna en texto plano.</p>
     *
     * @param taxId    RFC del usuario (actúa como username).
     * @param password Contraseña en texto plano proporcionada por el cliente.
     * @return {@link UserResponseDto} del usuario autenticado.
     * @throws IllegalArgumentException si el {@code taxId} no existe o la contraseña es incorrecta.
     */
    public UserResponseDto login(final String taxId, final String password) {
        // Mensaje genérico intencionalmente: no revelar si el RFC existe o no (práctica de seguridad)
        final String invalidCredentialsMessage = "Credenciales inválidas";

        final User user = userRepository.findByTaxId(taxId)
                .orElseThrow(() -> new IllegalArgumentException(invalidCredentialsMessage));

        final String decryptedPassword = Aes256Util.decrypt(user.getPassword());

        if (!decryptedPassword.equals(password)) {
            throw new IllegalArgumentException(invalidCredentialsMessage);
        }

        return UserResponseDto.from(user);
    }

    // =========================================================================
    // Métodos privados de soporte
    // =========================================================================

    /**
     * Construye un {@link Predicate} a partir de la expresión de filtro.
     * Formato esperado: {@code atributo+operador+valor}
     *
     * @param filter Expresión de filtro raw del query param.
     * @return Predicado listo para aplicar sobre el stream de usuarios.
     * @throws IllegalArgumentException si el formato o el operador son inválidos.
     */
    private Predicate<User> buildFilterPredicate(final String filter) {
        final String[] parts = filter.split(FILTER_DELIMITER, 3);

        if (parts.length != 3) {
            throw new IllegalArgumentException(
                    "El parámetro 'filter' debe tener el formato: atributo+operador+valor. " +
                    "Ejemplo: name+co+juan");
        }

        final String attribute = parts[0].trim().toLowerCase();
        final String operator  = parts[1].trim().toLowerCase();
        final String value     = parts[2].trim().toLowerCase();

        // Obtener el extractor de texto del atributo solicitado
        final Function<User, String> extractor = resolveAttributeExtractor(attribute);

        // Construir el predicado según el operador
        return switch (operator) {
            case "co" -> user -> {
                final String field = extractor.apply(user);
                return field != null && field.toLowerCase().contains(value);
            };
            case "eq" -> user -> {
                final String field = extractor.apply(user);
                return field != null && field.equalsIgnoreCase(value);
            };
            case "sw" -> user -> {
                final String field = extractor.apply(user);
                return field != null && field.toLowerCase().startsWith(value);
            };
            case "ew" -> user -> {
                final String field = extractor.apply(user);
                return field != null && field.toLowerCase().endsWith(value);
            };
            default -> throw new IllegalArgumentException(
                    "Operador de filtro no soportado: '" + operator + "'. " +
                    "Operadores válidos: co, eq, sw, ew");
        };
    }

    /**
     * Resuelve el extractor de atributo de texto para el filtrado.
     * {@code createdAt} se convierte a String para permitir filtros por fecha.
     *
     * @param attribute Nombre del atributo (en minúsculas).
     * @return Función que extrae el valor de texto del atributo indicado.
     * @throws IllegalArgumentException si el atributo no está soportado.
     */
    private Function<User, String> resolveAttributeExtractor(final String attribute) {
        return switch (attribute) {
            case "id"        -> user -> user.getId() != null ? user.getId().toString() : null;
            case "email"     -> User::getEmail;
            case "name"      -> User::getName;
            case "phone"     -> User::getPhone;
            case "taxid"     -> User::getTaxId;
            case "createdat" -> user -> user.getCreatedAt() != null
                                        ? user.getCreatedAt().toString() : null;
            default -> throw new IllegalArgumentException(
                    "Atributo de filtro no soportado: '" + attribute + "'. " +
                    "Atributos válidos: id, email, name, phone, taxId, createdAt");
        };
    }

    /**
     * Resuelve el {@link Comparator} de usuario para el ordenamiento dinámico.
     *
     * @param sortedBy Nombre del atributo (en minúsculas).
     * @return Comparador correspondiente al atributo solicitado.
     * @throws IllegalArgumentException si el atributo no está soportado.
     */
    private Comparator<User> resolveComparator(final String sortedBy) {
        return switch (sortedBy) {
            case "id"        -> Comparator.comparing(u -> u.getId().toString());
            case "email"     -> Comparator.comparing(User::getEmail,
                                    Comparator.nullsLast(String::compareToIgnoreCase));
            case "name"      -> Comparator.comparing(User::getName,
                                    Comparator.nullsLast(String::compareToIgnoreCase));
            case "phone"     -> Comparator.comparing(User::getPhone,
                                    Comparator.nullsLast(String::compareToIgnoreCase));
            case "taxid"     -> Comparator.comparing(User::getTaxId,
                                    Comparator.nullsLast(String::compareToIgnoreCase));
            case "createdat" -> Comparator.comparing(User::getCreatedAt,
                                    Comparator.nullsLast(Comparator.naturalOrder()));
            default -> throw new IllegalArgumentException(
                    "Atributo de ordenamiento no soportado: '" + sortedBy + "'. " +
                    "Atributos válidos: id, email, name, phone, taxId, createdAt");
        };
    }

    /**
     * Aplica los campos del mapa {@code updates} sobre el usuario mutando su estado.
     * Re-valida {@code taxId} y {@code phone} si son parte de la actualización.
     *
     * @param user    Usuario existente a modificar.
     * @param updates Mapa de campos con sus nuevos valores.
     */
    @SuppressWarnings("unchecked")
    private void applyUpdates(final User user, final Map<String, Object> updates) {
        if (updates.containsKey("email")) {
            user.setEmail((String) updates.get("email"));
        }

        if (updates.containsKey("name")) {
            final String newName = (String) updates.get("name");
            ValidationUtil.validateFullName(newName);
            user.setName(newName);
        }

        if (updates.containsKey("phone")) {
            final String newPhone = (String) updates.get("phone");
            ValidationUtil.validatePhone(newPhone);
            user.setPhone(newPhone);
        }

        if (updates.containsKey("password")) {
            // Cifrar la nueva contraseña antes de persistirla
            user.setPassword(Aes256Util.encrypt((String) updates.get("password")));
        }

        if (updates.containsKey("taxId")) {
            final String newTaxId = (String) updates.get("taxId");
            ValidationUtil.validateTaxId(newTaxId);
            // Verificar unicidad excluyendo al propio usuario que se está actualizando
            ensureTaxIdIsUnique(newTaxId, user.getId());
            user.setTaxId(newTaxId.toUpperCase());
        }

        if (updates.containsKey("addresses")) {
            user.setAddresses((List<Address>) updates.get("addresses"));
        }
    }

    /**
     * Verifica que el {@code taxId} no esté en uso por otro usuario distinto al de {@code excludeId}.
     *
     * @param taxId     RFC a verificar.
     * @param excludeId UUID del usuario que puede poseer ese RFC sin conflicto (para updates).
     *                  Pasar {@code null} en creaciones nuevas.
     * @throws IllegalArgumentException si el RFC ya está registrado por otro usuario.
     */
    private void ensureTaxIdIsUnique(final String taxId, final UUID excludeId) {
        final Optional<User> existing = userRepository.findByTaxId(taxId);

        final boolean takenByAnotherUser = existing.isPresent()
                && !existing.get().getId().equals(excludeId);

        if (takenByAnotherUser) {
            throw new IllegalArgumentException(
                    "El taxId (RFC) '" + taxId + "' ya está registrado por otro usuario");
        }
    }

    /**
     * Busca un usuario por UUID o lanza excepción descriptiva si no existe.
     *
     * @param id UUID del usuario buscado.
     * @return El {@link User} encontrado.
     * @throws IllegalArgumentException si no existe un usuario con ese {@code id}.
     */
    private User findUserOrThrow(final UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró un usuario con id: " + id));
    }
}