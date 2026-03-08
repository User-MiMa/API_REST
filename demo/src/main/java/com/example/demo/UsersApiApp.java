package com.example.demo;

import com.example.demo.model.Address;
import com.example.demo.model.User;
import com.example.demo.dto.UserRequestDto;
import com.example.demo.dto.UserResponseDto;
import com.example.demo.util.Aes256Util;
import com.example.demo.util.ValidationUtil;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.UserService;


import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Set;
import java.util.Map;

public class UsersApiApp {
    public static void main(String[] args) {
        // DTO de entrada
        UserRequestDto request = UserRequestDto.builder()
                .email("milton@example.com")
                .name("Milton Sandoval Masson")
                .phone("+525512345678")
                .password("secreto123")
                .taxId("SAMM000229XYZ")
                .addresses(List.of(new Address(null, "Casa", "Av. Reforma 123", "MX")))
                .build();

        // Instanciar repositorio y servicio
        UserRepository repo = new UserRepository();
        UserService service = new UserService(repo);

        // Crear usuario
        UserResponseDto created = service.createUser(request);
        System.out.println("Usuario creado: " + created);

        // Filtrar y ordenar
        service.getUsers(null, "name+co+milton").forEach(System.out::println);
        service.getUsers("email", null).forEach(System.out::println);

        // Actualizar parcialmente
        Map<String, Object> updates = Map.of("phone", "+525511112222");
        UserResponseDto updated = service.updateUser(created.getId(), updates);
        System.out.println("Usuario actualizado: " + updated);

        // Login
        try {
            UserResponseDto loggedIn = service.login("SAMM000229XYZ", "secreto123");
            System.out.println("Login exitoso: " + loggedIn);
        } catch (IllegalArgumentException e) {
            System.out.println("Login fallido: " + e.getMessage());
        }

        // Eliminar
        service.deleteUser(created.getId());
        System.out.println("Usuarios después de eliminar:");
        service.getUsers(null, null).forEach(System.out::println);
    }
}
