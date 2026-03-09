# 1. Imagen base con Java
FROM eclipse-temurin:17-jdk-alpine

# 2. Carpeta dentro del contenedor
WORKDIR /app

# 3. Copiar el jar generado
COPY target/*.jar app.jar

# 4. Puerto que usa la API
EXPOSE 8080

# 5. Comando para ejecutar la aplicación
ENTRYPOINT ["java","-jar","app.jar"]