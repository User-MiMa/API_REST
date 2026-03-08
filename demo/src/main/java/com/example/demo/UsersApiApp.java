package com.example.demo;

import com.example.demo.model.Address;
import com.example.demo.model.User;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.List;

public class UsersApiApp {
    public static void main(String[] args) {
        // Crear direcciones
        Address casa = new Address(null, "Casa", "Av. Reforma 123", "mx");
        Address oficina = new Address(UUID.randomUUID(), "Oficina", "Insurgentes 456", "US");

        // Crear usuario con datos básicos
        User usuario = new User(
                UUID.randomUUID(),
                "milton@example.com",
                "Milton Sandoval",
                "+52 5512345678",
                "contraseñadebil", 
                "MHE010195XYZ",
                ZonedDateTime.now(ZoneId.of("UTC+3")),
                List.of(casa, oficina)
        );

        // Mostrar datos del usuario
        System.out.println("=== Usuario ===");
        System.out.println(usuario);

        // Mostrar direcciones asociadas
        System.out.println("\n=== Direcciones ===");
        usuario.getAddresses().forEach(System.out::println);

    }

}