📖 Users API

API REST desarrollada en Java (JDK 17) con Maven 3.9.12, que permite gestionar usuarios y autenticación.
Incluye operaciones CRUD, filtrado y ordenamiento de datos, además de validaciones específicas.

🛠️ Tecnologías utilizadas

Java 17

Maven 3.9.12

Spring Boot

AES256 (GCM) para cifrado de contraseñas

Swagger/OpenAPI para documentación

Docker para empaquetado y despliegue

📌 Descripción del proyecto

La Users API permite:

Crear, leer, actualizar y eliminar usuarios.

Filtrado y ordenamiento dinámico de usuarios.

Validaciones de formato de teléfono (Estándar E.164)

Validaciones de taxId (RFC): Formato 13 caracteres válido, coincidente con nombre de usuario, único y de personas mayores a 18 años.

Registro de fecha de creación (createdAt) en zona horaria Madagascar (UTC+3).

🚀 Ejecución local

Clonar el repositorio:
git clone https://github.com/tuusuario/users-api.git (github.com in Bing)  
cd API_REST

Compilar y ejecutar:
mvn clean install
mvn spring-boot:run

La API estará disponible en:
http://localhost:8080
o
Swagger/OpenAPI en http://localhost:8080/swagger-ui/index.html#/

🐳 Ejecución con Docker

La aplicación se puede ejecutar en un contenedor Docker usando el .jar generado en target/demo-0.0.1-SNAPSHOT.

Usar comandos:

docker build -t users-api .
docker run -p 8080:8080 users-api

La API estará disponible en:
http://localhost:8080
o 
Swagger/OpenAPI en http://localhost:8080/swagger-ui/index.html#/

📑 Endpoints principales
Usuarios
GET /users?sortedBy=[campo] → Lista de usuarios ordenados

GET /users?filter=[campo]+[co|eq|sw|ew]+[valor] → Lista filtrada

POST /users → Crear usuario

PATCH /users/{id} → Actualizar usuario

DELETE /users/{id} → Eliminar usuario

Autenticación
POST /login → Autenticación usando taxId como username

🧪 Ejemplos de filtros
GET /users?filter=name+co+andres → Usuarios cuyo nombre contiene "andres"

GET /users?filter=email+ew+mail.com → Usuarios cuyo email termina en "email.com"

GET /users?filter=phone+sw+52 → Usuarios cuyo teléfono empieza con "52"

GET /users?filter=tax_id+eq=AARR990101XXX → Usuario con ese taxId

🔐 Autenticación

Endpoint:

POST /login

Request:
{
"tax_id": "AARR990101XXX",
"password": "mypassword"
}


📌 Notas extras

Contraseñas cifradas con AES256(GCM) y no se devuelven en el response

createdAt en zona horaria Madagascar (formato dd-MM-yyyy HH:mm)

Postman Collection disponible en carpeta PostmanCollection en raíz de proyecto

Unit Tests incluidos con JUnit /src/test


👨‍💻 Autor
Mi-Ma
