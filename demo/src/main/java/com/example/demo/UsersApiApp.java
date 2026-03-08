package com.example.demo;

import com.example.demo.model.Address;
import com.example.demo.model.User;
import com.example.demo.dto.UserRequestDto;
import com.example.demo.dto.UserResponseDto;
import com.example.demo.util.Aes256Util;
import com.example.demo.util.ValidationUtil;
import com.example.demo.repository.UserRepository;


import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Set;

public class UsersApiApp {
    public static void main(String[] args) {
        // 1. Simular datos de entrada (como si vinieran de un cliente)
        UserRequestDto request = UserRequestDto.builder()
                .email("milton@example.com")
                .name("Milton Sandoval Masson") // aquí puedes probar con "Milton Pérez Hernández"
                .phone("+525512345678")
                .password("secreto123")
                .taxId("SAMM000229XYZ") // ejemplo con fecha de nacimiento 1985-01-01
                .addresses(List.of(new Address(null, "Casa", "Av. Reforma 123", "MX")))
                .build();

        // Validación (anotaciones en DTO)
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set<ConstraintViolation<UserRequestDto>> violations = validator.validate(request);

        if (!violations.isEmpty()) {
            System.out.println("Errores de validación (Bean Validation):");
            violations.forEach(v -> System.out.println("- " + v.getPropertyPath() + ": " + v.getMessage()));
        } else {
            System.out.println("Request válido según anotaciones.");
        }

        try {
            ValidationUtil.validateFullName(request.getName());
        } catch (IllegalArgumentException e) {
            System.out.println("Error nombre: " + e.getMessage());
        }

        try {
            ValidationUtil.validateTaxId(request.getTaxId());
        } catch (IllegalArgumentException e) {
            System.out.println("Error RFC: " + e.getMessage());
        }

        try {
            ValidationUtil.validateRfcMatchesName(request.getTaxId(), request.getName());
        } catch (IllegalArgumentException e) {
            System.out.println("Error coincidencia RFC/nombre: " + e.getMessage());
        }

        System.out.println("\nDTO de entrada (UserRequestDto):");
        System.out.println(request);

        // 2. Cifrar la contraseña antes de crear el User
        String encryptedPassword = Aes256Util.encrypt(request.getPassword());
        System.out.println("\nContraseña cifrada: " + encryptedPassword);

        // 3. Convertir el DTO en modelo de dominio con la contraseña cifrada
        User user = new User(
                UUID.randomUUID(),
                request.getEmail(),
                request.getName(),
                request.getPhone(),
                encryptedPassword,
                request.getTaxId(),
                ZonedDateTime.now(),
                request.getAddresses()
        );

        System.out.println("\nEntidad de dominio (User):");
        System.out.println(user);

        // 4. Probar descifrado (solo para verificación en esta prueba rápida)
        String decryptedPassword = Aes256Util.decrypt(encryptedPassword);
        System.out.println("\nContraseña descifrada (verificación): " + decryptedPassword);

        // 5. Convertir el modelo en DTO de salida
        UserResponseDto response = UserResponseDto.from(user);
        System.out.println("\nDTO de salida (UserResponseDto):");
        System.out.println(response);

        // 6. Instanciar el repositorio
        UserRepository repo = new UserRepository();

        System.out.println("\nUsuarios semilla en el repositorio:");
        repo.findAll().forEach(System.out::println);

        // 7. Guardar el nuevo usuario creado desde el DTO
        repo.save(user);
        System.out.println("\nUsuarios después de insertar a Milton:");
        repo.findAll().forEach(System.out::println);

        // 8. Buscar por RFC
        System.out.println("\nBuscar por RFC SAMM000229XYZ:");
        repo.findByTaxId("SAMM000229XYZ")
            .ifPresentOrElse(
                u -> System.out.println("Encontrado: " + u),
                () -> System.out.println("No se encontró usuario con ese RFC")
            );

        // 9. Eliminar por ID
        UUID idMilton = user.getId();
        repo.deleteById(idMilton);
        System.out.println("\nUsuarios después de eliminar a Milton:");
        repo.findAll().forEach(System.out::println);
    }
}