package com.example.demo;

import com.example.demo.model.Address;
import com.example.demo.model.User;
import com.example.demo.dto.UserRequestDto;
import com.example.demo.dto.UserResponseDto;

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
                .name("Milton Sandoval")
                .phone("+525512345678")
                .password("contraseñadebil")
                .taxId("ABCD998877XYZ")
                .addresses(List.of(new Address(null, "Casa", "Av. Reforma 123", "MX")))
                .build();

                  // Validar manualmente
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        Set<ConstraintViolation<UserRequestDto>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            System.out.println("Errores de validación:");
            violations.forEach(v -> System.out.println("- " + v.getPropertyPath() + ": " + v.getMessage()));
        } else {
            System.out.println("Request válido");
        }

        System.out.println("DTO de entrada (UserRequestDto):");
        System.out.println(request);

        // 2. Convertir el DTO de entrada en un modelo de dominio User
        User user = new User(
                UUID.randomUUID(),
                request.getEmail(),
                request.getName(),
                request.getPhone(),
                request.getPassword(), // aquí normalmente se cifraría
                request.getTaxId(),
                ZonedDateTime.now(),
                request.getAddresses()
        );

        System.out.println("\nEntidad de dominio (User):");
        System.out.println(user);

        // 3. Convertir el modelo User en un DTO de salida
        UserResponseDto response = UserResponseDto.from(user);

        System.out.println("\nDTO de salida (UserResponseDto):");
        System.out.println(response);
    }
}
