package com.example.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración de la documentación OpenAPI (Swagger) para la API de Usuarios.
 *
 * <p>Genera la especificación OpenAPI 3.0 accesible en:</p>
 * <ul>
 *   <li>Interfaz visual: <a href="http://localhost:8080/swagger-ui.html">
 *       http://localhost:8080/swagger-ui.html</a></li>
 *   <li>JSON de la spec: <a href="http://localhost:8080/v3/api-docs">
 *       http://localhost:8080/v3/api-docs</a></li>
 * </ul>
 *
 * <p>Dependencia requerida en {@code pom.xml}:</p>
 * <pre>{@code
 * <dependency>
 *     <groupId>org.springdoc</groupId>
 *     <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
 *     <version>2.x.x</version>
 * </dependency>
 * }</pre>
 */
@Configuration
public class SwaggerConfig {

    /**
     * Define la configuración principal del documento OpenAPI.
     *
     * <p>Incluye metadatos de la API (título, descripción, versión, contacto) y
     * las etiquetas que documentan cada operación HTTP disponible en los controladores.</p>
     *
     * @return Instancia de {@link OpenAPI} con la especificación completa de la API.
     */
    @Bean
    public OpenAPI usersOpenAPI() {
        return new OpenAPI()
                .info(buildApiInfo())
                .tags(buildOperationTags());
    }

    // -------------------------------------------------------------------------
    // Métodos privados de construcción — separan la inicialización del bean
    // -------------------------------------------------------------------------

    /**
     * Construye los metadatos descriptivos de la API:
     * título, descripción general, versión y datos de contacto del autor.
     */
    private Info buildApiInfo() {
    return new Info()
            .title("API Usuarios")
            .description("API para gestión de usuarios con filtros, ordenamiento y autenticación")
            .version("1.0.0");
    }


    /**
     * Construye las etiquetas que agrupan y describen cada método HTTP de la API.
     *
     * <p>En Swagger UI, estas etiquetas aparecen como secciones que agrupan
     * los endpoints según la operación que realizan.</p>
     */
    private List<Tag> buildOperationTags() {
        return List.of(
                // Consulta de usuarios — soporta filtros y ordenamiento por atributo
                new Tag()
                        .name("GET")
                        .description("Obtiene registro de usuarios"),

                // Registro de usuario — aplica validaciones de RFC, teléfono y unicidad
                new Tag()
                        .name("POST")
                        .description("Crea un usuario"),

                // Autenticación de usuario — login con credenciales válidas
                new Tag()
                        .name("POST/login")
                        .description("Autentica un usuario y devuelve token de acceso"),

                // Modificación de usuario — actualización parcial o total de atributos
                new Tag()
                        .name("PUT")
                        .description("Actualiza un usuario"),

                // Baja de usuario — eliminación permanente del registro en el sistema
                new Tag()
                        .name("DELETE")
                        .description("Elimina un usuario")
        );
    }
}
