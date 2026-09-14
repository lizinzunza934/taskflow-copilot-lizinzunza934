# Arquitectura de TaskFlow

Esta guía rápida ayuda a un desarrollador nuevo a entender la estructura y las decisiones clave del proyecto TaskFlow.

---

## Capas y paquetes

El proyecto sigue una arquitectura en capas con raíz de paquete `com.taskflow`:

- Capa de entrada (HTTP): controladores en `src/main/java/com/taskflow/controller`
  - Ejemplo: `TaskController` o `ProjectController` (`src/main/java/com/taskflow/controller/TaskController.java`, `src/main/java/com/taskflow/controller/ProjectController.java`).
  - Reciben `DTO` (siempre `record` con validación `@Valid`) y devuelven respuestas `DTO`.

- Capa de aplicación / casos de uso: servicios en `src/main/java/com/taskflow/service`
  - Ejemplo: `TaskService` (`src/main/java/com/taskflow/service/TaskService.java`).
  - Orquesta repositorios, validaciones de caso de uso y transacciones.

- Capa de infraestructura: repositorios Spring Data JPA en `src/main/java/com/taskflow/repository`
  - Ejemplo: `TaskRepository`, `ProjectRepository` (`src/main/java/com/taskflow/repository/TaskRepository.java`).
  - Acceso a la base de datos; mapeo ORM.

- Capa de dominio / modelo: entidades y lógica de negocio en `src/main/java/com/taskflow/model`
  - Ejemplo: `Task` (`src/main/java/com/taskflow/model/Task.java`).
  - Las reglas de negocio principales viven en las entidades y métodos de fábrica.

- Mappers y DTOs: `src/main/java/com/taskflow/mapper` y `src/main/java/com/taskflow/dto`
  - Ejemplo: `TaskMapper` (`src/main/java/com/taskflow/mapper/TaskMapper.java`) con método `TaskMapper.aResponse`.
  - Los controladores usan mappers para convertir entre `DTO` y entidades; nunca exponen entidades directas.

- Config y utilidades: `src/main/java/com/taskflow/config` y `src/main/java/com/taskflow/security`.
  - `DataSeeder` (`src/main/java/com/taskflow/config/DataSeeder.java`) puebla datos con el perfil `h2`.
  - Seguridad y filtros JWT en `src/main/java/com/taskflow/security`.

- Manejo de errores: `GlobalExceptionHandler` (`src/main/java/com/taskflow/advice/GlobalExceptionHandler.java`) transforma excepciones a respuestas REST uniformes.

---

## Recorrido de `POST /projects/{projectId}/tasks`

1. Petición HTTP llega al controlador:
   - Endpoint declarado en `TaskController` o `ProjectController` (`src/main/java/com/taskflow/controller/TaskController.java`).
   - El método recibe un `@PathVariable("projectId")` y un `@RequestBody @Valid` con un `record` DTO (p. ej. `src/main/java/com/taskflow/dto/TaskRequest.java`).

2. Validación automática de Bean Validation:
   - Si el DTO viola restricciones, `GlobalExceptionHandler` retorna `400 Bad Request`.

3. Mapeo DTO → entidad:
   - `TaskMapper` transforma el `TaskRequest` en los parámetros necesarios para crear la entidad.

4. Llamada al servicio de aplicación:
   - El controlador invoca `TaskService.crear(request, projectId)` (`src/main/java/com/taskflow/service/TaskService.java`).
   - El servicio abre la transacción (si aplica), carga el `Project` con `ProjectRepository` para comprobar existencia y permisos.

5. Reglas de negocio y fábrica de entidad:
   - `TaskService` delega la creación al constructor/fábrica de dominio: `Task.crear(...)` (`src/main/java/com/taskflow/model/Task.java`).
   - Reglas como "no crear tareas con fecha pasada" o estado inicial `TODO` están implementadas en `Task` (ej.: `Task.estaVencida()` y validaciones en `Task.crear(...)`).
   - Si una regla falla, `Task` lanza una excepción de dominio que `GlobalExceptionHandler` convierte a un código apropiado (p. ej. `422 Unprocessable Entity`).

6. Persistencia:
   - `TaskService` usa `TaskRepository.save(task)` para persistir la entidad (`src/main/java/com/taskflow/repository/TaskRepository.java`).

7. Respuesta:
   - Tras guardar, `TaskMapper.aResponse` construye el DTO de salida.
   - El controlador devuelve `201 Created` con cabecera `Location` apuntando a la nueva tarea y el cuerpo con el DTO.

Resumen rápido de archivos clave en este flujo:
- `src/main/java/com/taskflow/controller/TaskController.java` (entrada REST)
- `src/main/java/com/taskflow/dto/TaskRequest.java` (request DTO)
- `src/main/java/com/taskflow/mapper/TaskMapper.java` (mapeos)
- `src/main/java/com/taskflow/service/TaskService.java` (caso de uso)
- `src/main/java/com/taskflow/model/Task.java` (reglas de dominio)
- `src/main/java/com/taskflow/repository/TaskRepository.java` (persistencia)
- `src/main/java/com/taskflow/advice/GlobalExceptionHandler.java` (tratamiento de errores)

---

## Dónde viven las reglas de negocio

- Las reglas de dominio se alojan prioritariamente en las entidades del paquete `model`.
  - Ejemplo: `Task.crear(...)` valida invarianzas de la entidad y fija estado inicial.
  - Comportamientos relacionados (p. ej. `Task.estaVencida()`, `Task.setAssigneeId(...)`) forman parte de `Task`.

- `Service` contiene reglas de orquestación y políticas de aplicación (p. ej. comprobar que el `Project` existe, reglas transaccionales y coordinación entre repositorios).

- No duplicar validaciones: reutilizar las reglas del dominio desde los servicios y controladores; los controladores solo deben validar la estructura del input (Bean Validation).

---

## Seguridad con JWT

- Autenticación y autorización sin estado usando JWT.
- Rutas públicas: `/auth/**`, `/info`, Swagger y consola H2; todo lo demás requiere token.
- Componentes principales:
  - `SecurityConfig` (`src/main/java/com/taskflow/config/SecurityConfig.java`) configura filtros y reglas HTTP.
  - Un filtro JWT (p. ej. `JwtAuthenticationFilter`) extrae el token, lo valida y carga `Authentication` en el `SecurityContext`.
  - Servicio de usuarios y `UserDetails` para validación del token y roles.
  - Autorización declarativa con `@PreAuthorize` y componentes de seguridad específicos como `ProjectSecurity` (`src/main/java/com/taskflow/security/ProjectSecurity.java`) para reglas finas (ej.: "solo el dueño o ADMIN puede borrar un proyecto").

- Flujo de petición con JWT:
  1. El cliente obtiene token desde `POST /auth/login`.
  2. En cada petición segura, el `Authorization: Bearer <token>` llega al servidor.
  3. El filtro JWT valida firma y fecha, construye `Authentication` y la inyecta en el contexto.
  4. Spring Security aplica políticas (`@PreAuthorize`, restricciones de rutas) usando roles del `Authentication`.

---

## Organización de tests

- Unit tests:
  - Ubicación: `src/test/java/com/taskflow/...`
  - Herramientas: JUnit 5 y Mockito. Prueban lógica de servicios y del dominio (`model`) sin arrancar Spring.
  - Ejemplo: `src/test/java/com/taskflow/unit/TaskServiceTest.java`.

- Slice tests:
  - `@WebMvcTest` para controladores y `@DataJpaTest` para repositorios.
  - Ejecutan solo la porción necesaria del contexto Spring.

- Integration tests:
  - `@SpringBootTest` con perfil `test`.
  - Testcontainers se usan para pruebas que requieren DB real; los tests integrales que usan Testcontainers se nombran `*IT.java` y están desactivados por defecto (no corren con `mvn test` salvo `-Ddocker.tests=true`).

- Comandos comunes:
  - Ejecutar suite normal: `mvn -q test`.
  - Ejecutar una clase de test: `mvn -q test "-Dtest=TaskServiceTest"`.
  - Levantar la app con H2 de ejemplo: `mvn spring-boot:run "-Dspring-boot.run.profiles=h2"` (usa `DataSeeder`).

---

## Buenas prácticas para contribuir

- No exponer entidades en respuestas REST; usar `DTO` y `TaskMapper`.
- Mantener reglas de negocio en `model` y orquestación en `service`.
- Respetar convenciones de errores (`GlobalExceptionHandler`) y códigos HTTP (`201 Created` con `Location`, `204 No Content` en deletes).
- Evitar cambios en tests existentes para que pasen; arreglar código si un test falla.

---

Si se desea, se puede ampliar este archivo con diagramas o ejemplos de petición/respuesta. Para preguntas concretas sobre rutas o clases, revisar los archivos en `src/main/java/com/taskflow`.
