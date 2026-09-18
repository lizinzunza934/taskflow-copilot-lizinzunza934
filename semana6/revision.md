# Copilot CLI Session

> [!NOTE]
> - **Session ID:** `38d57325-9f6d-4dd9-a57f-6aeaaefef122`  
> - **Started:** 17/9/2026, 4:46:00 p.m.  
> - **Duration:** 2m 25s  
> - **Exported:** 17/9/2026, 4:48:26 p.m.  

---

<sub>2s</sub>

### User

Revisa semana6/proyecto-final.diff contra la especificación specs/search.md.

---

<sub>9s</sub>

### Copilot

Leeré el diff pedido y la especificación para compararlos. Ejecutando lecturas en paralelo de semana6/proyecto-final.diff y specs/search.md para extraer archivos modificados y criterios a verificar. Luego abriré los archivos del repo que aparecen en el diff.

---

<sub>9s</sub>

### `view`

**view the file at C:\Users\User\taskflow-copilot-lizinzunza934\semana6\proyecto-final.diff.**

C:\Users\User\taskflow-copilot-lizinzunza934\semana6\proyecto-final.diff

<details>
<summary>178 lines</summary>

```diff
diff --git a/src/main/java/com/taskflow/controller/TaskController.java b/src/main/java/com/taskflow/controller/TaskController.java
index e4b53e3..e00c97b 100644
--- a/src/main/java/com/taskflow/controller/TaskController.java
+++ b/src/main/java/com/taskflow/controller/TaskController.java
@@ -98,6 +98,14 @@ public class TaskController {
         return tareas.stream().map(TaskMapper::aResponse).toList();
     }
 
+    @Operation(summary = "Busca tareas por título",
+            description = "Busca en todos los proyectos tareas cuyo título contiene el parámetro 'q' (case-insensitive). Devuelve 400 si 'q' falta o queda vacío.")
+    @GetMapping("/tasks/search")
+    public List<TaskResponse> searchTasks(@RequestParam(name = "q", required = false) String q) throws TaskValidationException {
+        List<Task> tareas = taskService.buscarPorTitulo(q);
+        return tareas.stream().map(TaskMapper::aResponse).toList();
+    }
+
     /**
      * POST /projects/{projectId}/tasks — crea una tarea bajo un proyecto. 201 + header Location
      * apuntando a donde el recurso SE LEE (/tasks/{id}), no a la URL de creación. @Valid dispara Bean
diff --git a/src/main/java/com/taskflow/service/TaskService.java b/src/main/java/com/taskflow/service/TaskService.java
index 3fe2b2a..a5024a2 100644
--- a/src/main/java/com/taskflow/service/TaskService.java
+++ b/src/main/java/com/taskflow/service/TaskService.java
@@ -142,4 +142,20 @@ public class TaskService {
                 .sorted(TaskOrders.POR_FECHA)
                 .toList();
     }
+
+    /**
+     * Busca tareas cuyo título contiene la cadena q (case-insensitive).
+     * q se trimea con strip(); si es null o queda vacío se lanza TaskValidationException
+     * con el mensaje requerido por la especificación.
+     */
+    public List<Task> buscarPorTitulo(String q) throws TaskValidationException {
+        if (q == null || q.strip().isEmpty()) {
+            throw new TaskValidationException("El parámetro 'q' es obligatorio.");
+        }
+        String trimmed = q.strip();
+        List<Task> encontrados = repository.findByTitleContainingIgnoreCase(trimmed);
+        return encontrados.stream()
+                .sorted(TaskOrders.POR_TITULO)
+                .toList();
+    }
 }
diff --git a/src/test/java/com/taskflow/slice/BusquedaTareasControllerTest.java b/src/test/java/com/taskflow/slice/BusquedaTareasControllerTest.java
new file mode 100644
index 0000000..87cb074
--- /dev/null
+++ b/src/test/java/com/taskflow/slice/BusquedaTareasControllerTest.java
@@ -0,0 +1,68 @@
+package com.taskflow.slice;
+
+import com.taskflow.controller.TaskController;
+import com.taskflow.model.Task;
+import com.taskflow.model.Priority;
+import com.taskflow.model.TaskStatus;
+import com.taskflow.security.JwtAuthenticationFilter;
+import com.taskflow.service.ProjectService;
+import com.taskflow.service.TaskService;
+import org.junit.jupiter.api.Test;
+import org.springframework.beans.factory.annotation.Autowired;
+import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
+import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
+import org.springframework.test.context.bean.override.mockito.MockitoBean;
+import org.springframework.test.web.servlet.MockMvc;
+
+import java.time.LocalDate;
+import java.util.List;
+import java.util.Optional;
+
+import static org.mockito.Mockito.when;
+import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
+import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
+import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
+
+/**
+ * Slice web de GET /tasks/search: solo la capa HTTP; la lógica se prueba en el unit.
+ */
+@WebMvcTest(TaskController.class)
+@AutoConfigureMockMvc(addFilters = false)
+class BusquedaTareasControllerTest {
+
+    @Autowired
+    private MockMvc mockMvc;
+
+    @MockitoBean
+    private TaskService taskService;
+
+    @MockitoBean
+    private ProjectService projectService;
+
+    @MockitoBean
+    private JwtAuthenticationFilter jwtAuthenticationFilter;
+
+    @Test
+    void getSearch_con_q_devuelve200YJson() throws Exception {
+        Task t1 = new Task(9L, "Documentar la API con Swagger", "d", TaskStatus.TODO, Priority.MED, 1L, null, LocalDate.now().plusDays(10));
+        Task t2 = new Task(5L, "Optimizar consultas de la API", "d", TaskStatus.TODO, Priority.MED, 1L, null, LocalDate.now().plusDays(5));
+
+        when(taskService.buscarPorTitulo("api")).thenReturn(List.of(t1, t2));
+
+        mockMvc.perform(get("/tasks/search").param("q", "api"))
+                .andExpect(status().isOk())
+                .andExpect(jsonPath("$[0].id").value(9))
+                .andExpect(jsonPath("$[0].title").value("Documentar la API con Swagger"))
+                .andExpect(jsonPath("$[1].id").value(5))
+                .andExpect(jsonPath("$[1].title").value("Optimizar consultas de la API"));
+    }
+
+    @Test
+    void getSearch_sin_q_servicioLanzaValidation_devuelve400() throws Exception {
+        when(taskService.buscarPorTitulo((String) null)).thenThrow(new com.taskflow.exception.TaskValidationException("El parámetro 'q' es obligatorio."));
+
+        mockMvc.perform(get("/tasks/search"))
+                .andExpect(status().isBadRequest())
+                .andExpect(jsonPath("$.message").value("El parámetro 'q' es obligatorio."));
+    }
+}
diff --git a/src/test/java/com/taskflow/unit/BusquedaTareasServiceTest.java b/src/test/java/com/taskflow/unit/BusquedaTareasServiceTest.java
new file mode 100644
index 0000000..384b711
--- /dev/null
+++ b/src/test/java/com/taskflow/unit/BusquedaTareasServiceTest.java
@@ -0,0 +1,55 @@
+package com.taskflow.unit;
+
+import com.taskflow.exception.TaskValidationException;
+import com.taskflow.model.Priority;
+import com.taskflow.model.Task;
+import com.taskflow.model.TaskStatus;
+import com.taskflow.repository.TaskRepository;
+import com.taskflow.service.TaskService;
+import org.junit.jupiter.api.Test;
+import org.junit.jupiter.api.extension.ExtendWith;
+import org.mockito.InjectMocks;
+import org.mockito.Mock;
+import org.mockito.junit.jupiter.MockitoExtension;
+
+import java.time.LocalDate;
+import java.util.List;
+
+import static org.junit.jupiter.api.Assertions.assertEquals;
+import static org.junit.jupiter.api.Assertions.assertThrows;
+import static org.mockito.Mockito.verify;
+import static org.mockito.Mockito.when;
+
+/**
+ * Unit de TaskService.buscarPorTitulo: repositorio mockeado, tareas reales.
+ */
+@ExtendWith(MockitoExtension.class)
+class BusquedaTareasServiceTest {
+
+    @Mock
+    private TaskRepository taskRepository;
+
+    @InjectMocks
+    private TaskService service;
+
+    @Test
+    void buscarPorTitulo_ordenaYTrimeaYVerificaLlamada() throws TaskValidationException {
+        // dos tareas devueltas en orden NO alfabético
+        Task t1 = new Task(5L, "Optimizar consultas de la API", "d", TaskStatus.TODO, Priority.MED, 1L, null, LocalDate.now().plusDays(5));
+        Task t2 = new Task(9L, "Documentar la API con Swagger", "d", TaskStatus.TODO, Priority.MED, 1L, null, LocalDate.now().plusDays(10));
+
+        when(taskRepository.findByTitleContainingIgnoreCase("api")).thenReturn(List.of(t1, t2));
+
+        List<Task> resultado = service.buscarPorTitulo("  api ");
+
+        // debe venir ordenado por título: Documentar... (id 9) antes que Optimizar... (id 5)
+        assertEquals(List.of(t2, t1), resultado);
+        verify(taskRepository).findByTitleContainingIgnoreCase("api");
+    }
+
+    @Test
+    void buscarPorTitulo_nullYLimpio_lanzanTaskValidationException() {
+        assertThrows(TaskValidationException.class, () -> service.buscarPorTitulo(null));
+        assertThrows(TaskValidationException.class, () -> service.buscarPorTitulo("   "));
+    }
+}
```

</details>


---

<sub>9s</sub>

### `view`

**view the file at C:\Users\User\taskflow-copilot-lizinzunza934\specs\search.md.**

C:\Users\User\taskflow-copilot-lizinzunza934\specs\search.md

<details>
<summary>69 lines</summary>

````
# Especificación — `GET /tasks/search?q=`

## Qué

Un endpoint que busca tareas por su título, en todos los proyectos, para encontrar una tarea sin
recorrer la lista completa. «api» encuentra «Documentar la API con Swagger».

```
GET /tasks/search?q=api
Authorization: ******
```

## Reglas

1. Coincide toda tarea cuyo `title` **contiene** `q`, sin distinguir mayúsculas de minúsculas. La
   consulta ya existe en el repositorio: `TaskRepository.findByTitleContainingIgnoreCase(String)`.
   Úsala; no escribas SQL, `@Query` ni un filtro sobre `findAll()`.
2. Antes de buscar, `q` se recorta con `strip()`: `"  api "` busca `"api"`.
3. `q` es obligatorio. Si falta, o si después de recortarlo queda vacío, responde **`400`** con el
   `ErrorResponse` uniforme de siempre y el mensaje `El parámetro 'q' es obligatorio.`. Para eso el
   servicio lanza `TaskValidationException`, que el `GlobalExceptionHandler` ya convierte en 400.
4. Orden: alfabético por título, con el comparador que ya existe, `TaskOrders.POR_TITULO`.
5. Respuesta: `200` con una lista de `TaskResponse` (el mismo DTO que `GET /tasks`, con el mapper que
   ya existe, `TaskMapper.aResponse`). Si nada coincide, `200` con `[]`.
6. Seguridad: igual que el resto de `/tasks` (sin token → `401`). No cambies `SecurityConfig`.

## Dónde

- **Sin DTO ni mapper nuevos**: la respuesta es una lista del `TaskResponse` que ya existe.
- Método nuevo en `TaskService`:
  `public List<Task> buscarPorTitulo(String q) throws TaskValidationException`.
- Endpoint nuevo en `TaskController`: `@GetMapping("/tasks/search")`, con
  `@RequestParam(name = "q", required = false) String q`. El `required = false` es a propósito: así
  el «falta `q`» llega al servicio y sale con el mismo 400 uniforme que el «`q` vacío».
- Hoy `GET /tasks/search` responde `400` con «El parámetro 'id' tiene un valor ilegible»: lo atrapa
  `GET /tasks/{id}`. Con el endpoint nuevo, Spring prefiere la ruta literal `/tasks/search` sin
  importar en qué orden declares los métodos. No escribas un comentario que diga lo contrario.

> **Por qué no se reutiliza `ReportService.buscarPorTitulo`**, que hace casi lo mismo: habría que
> inyectar `ReportService` en `TaskController`, y el slice `TaskControllerTest` (que no puedes tocar)
> dejaría de arrancar. Medido el 12-sep: sus 10 tests fallan con `No qualifying bean of type
> 'com.taskflow.service.ReportService'`. Es la regla «no agregues dependencias nuevas al constructor
> de un controller que ya existe» de la skill `crear-endpoint-taskflow`.

## Tests que deben existir al terminar

Los dos en **clases nuevas** (las que ya existen no se tocan):

- **Unit** `src/test/java/com/taskflow/unit/BusquedaTareasServiceTest.java`, con
  `@Mock TaskRepository` e `@InjectMocks TaskService`:
  - el repositorio devuelve dos tareas en orden **no** alfabético y el servicio las regresa ordenadas
    por título (el test falla si quitas el orden);
  - `"  api "` llama al repositorio con `"api"` (`verify`);
  - `null` y `"   "` lanzan `TaskValidationException` y el repositorio **nunca** se llama.
- **Slice** `src/test/java/com/taskflow/slice/BusquedaTareasControllerTest.java`, con
  `@WebMvcTest(TaskController.class)`, `@AutoConfigureMockMvc(addFilters = false)` y `@MockitoBean`
  de `TaskService`, `ProjectService` (el constructor de `TaskController` recibe los dos) y
  `JwtAuthenticationFilter`:
  - `GET /tasks/search?q=api` → `200` y el JSON trae los ids y títulos que devolvió el servicio, en
    ese orden;
  - `GET /tasks/search` sin `q`, con el servicio lanzando `TaskValidationException` → `400` y
    `$.message` = `El parámetro 'q' es obligatorio.`.

## Resultado esperado con la semilla

Con la app arrancada con el perfil `h2` y el token de `ana`:

| Petición | Resultado |
|---|---|
| `GET /tasks/search?q=api` | `200`, ids `[9, 5]`: «Documentar la API con Swagger» y «Optimizar consultas de la API», en ese orden |
| `GET /tasks/search?q=API` | lo mismo: `[9, 5]` |
| `GET /tasks/search?q=zzz` | `200` con `[]` |
| `GET /tasks/search?q=%20%20` | `400` con `message` = `El parámetro 'q' es obligatorio.` |
| `GET /tasks/search` | `400` con el mismo mensaje |
| `GET /tasks/search?q=api` sin token | `401` |

## Restricciones para el agente

- No modifiques, borres ni desactives ningún test existente.
- No toques archivos fuera de `TaskService.java`, `TaskController.java` y las dos clases de test
  nuevas.
- Al terminar, `mvn -q test` tiene que pasar completo.

El pull request del proyecto final lleva además `semana6/`, `.github/skills/verificar-taskflow/casos-search.ps1`
y una línea nueva en `verificar.ps1`. Esos archivos los agregas tú en el proceso (PF-2 a PF-7 de la guía),
no el agente: no cuentan contra esta restricción.
````

</details>


---

<sub>19s</sub>

### Copilot

Leyendo archivos relevantes de tests slice existentes para comparar convenciones y anotaciones, abriendo en paralelo un slice test de ejemplo y GlobalExceptionHandler.

---

<sub>20s</sub>

### `view`

**view the file at C:\Users\User\taskflow-copilot-lizinzunza934\src\test\java\com\taskflow\slice\TaskControllerTest.java.**

C:\Users\User\taskflow-copilot-lizinzunza934\src\test\java\com\taskflow\slice\TaskControllerTest.java

<details>
<summary>194 lines</summary>

```
package com.taskflow.slice;

import com.taskflow.controller.TaskController;
import com.taskflow.exception.TaskNotFoundException;
import com.taskflow.exception.TaskStateException;
import com.taskflow.exception.TaskValidationException;
import com.taskflow.model.Priority;
import com.taskflow.model.Project;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.security.JwtAuthenticationFilter;
import com.taskflow.service.ProjectService;
import com.taskflow.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TaskControllerTest — SLICE web (S3D1, MP-6). La promesa de S2D3 pagada: los @SpringBootTest lentos
 * del CRUD regresan a @WebMvcTest. Aquí NO se levanta JPA, ni el seeder, ni la seguridad real: solo la
 * capa web (controller + advice + Jackson + validación). El service es un @MockitoBean.
 *
 * Las 3 anotaciones CANÓNICAS de la migración (bloque de T6/MP-6):
 *   1. @WebMvcTest(TaskController.class) -> levanta SOLO la capa web de este controller.
 *   2. @MockitoBean del/los service(s) -> el colaborador ausente; el músculo de S2D2 (when(...)) vuelve.
 *   3. @AutoConfigureMockMvc(addFilters = false) -> la seguridad NO se prueba en el slice: se prueba
 *      donde vive, en integration/. Sin esto, el slice aplica la seguridad default de Boot y TODO da 401.
 *
 * Tropiezo canónico (a): el slice ESCANEA los Filter @Component, y JwtAuthenticationFilter arrastra
 * JwtService/JpaUserDetailsService al contexto -> "No qualifying bean of type 'JwtService'". Solución:
 * @MockitoBean JwtAuthenticationFilter (se mockea el filtro para que el slice no cargue sus dependencias).
 *
 * @Mock vs @MockitoBean (dolor #1): aquí HAY contexto Spring -> @MockitoBean (sustituye un bean). En
 * unit/ NO hay Spring -> @Mock. Regla mecánica: ¿la clase tiene una anotación @...Test de Spring? -> bean.
 *
 * El token DESAPARECIÓ del test: ya no hay filtro que lo exija (addFilters = false).
 */
@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private ProjectService projectService;   // TaskController también inyecta ProjectService (cableo Task<->Project)

    // Sin esto: el slice intenta construir el Filter @Component real -> No qualifying bean 'JwtService'.
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ==================== GET (contra el stub del service, no una semilla) ====================

    @Test
    void getTasks_sinFiltro_retorna200YLista() throws Exception {
        when(taskService.listar()).thenReturn(List.of(
                tarea(1L, "Primera", TaskStatus.TODO),
                tarea(2L, "Segunda", TaskStatus.IN_PROGRESS)));

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Primera"));
    }

    @Test
    void getTaskPorId_existente_retorna200ConElTitulo() throws Exception {
        when(taskService.buscarPorId(1L)).thenReturn(Optional.of(tarea(1L, "Diseñar esquema de BD", TaskStatus.TODO)));

        mockMvc.perform(get("/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Diseñar esquema de BD"))
                .andExpect(jsonPath("$.projectId").value(1));
    }

    @Test
    void getTaskPorId_inexistente_retorna404() throws Exception {
        // El service dice "no está" (Optional vacío); el controller lanza -> el advice (SÍ vive en el slice) responde 404.
        when(taskService.buscarPorId(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/tasks/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ==================== Escritura (la validación y el advice SÍ viven en el slice) ====================

    @Test
    void postTask_valida_devuelve201ConLocation() throws Exception {
        when(projectService.buscarPorId(1L)).thenReturn(Optional.of(new Project(1L, "P", "d", 1L, null)));
        when(taskService.crear(any(), eq(1L)))
                .thenReturn(tareaCon(10L, "Redactar el informe trimestral", TaskStatus.TODO, 1L));

        String body = """
                {
                  "title": "Redactar el informe trimestral",
                  "description": "Con las métricas del quarter",
                  "priority": "HIGH",
                  "assigneeId": 1,
                  "dueDate": "2099-12-31"
                }
                """;

        mockMvc.perform(post("/projects/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/tasks/\\d+")))
                .andExpect(jsonPath("$.title").value("Redactar el informe trimestral"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    /**
     * Frontera de la API, ESPEJO del parametrizado de dominio (integrador, refactor b): títulos que
     * violan @NotBlank/@Size -> 400. La validación de forma vive en el slice; ni siquiera se llama al
     * service. 0 -> vacío; 1,2 -> cortos; 121 -> largo.
     */
    @ParameterizedTest(name = "título de longitud {0} → 400")
    @ValueSource(ints = {0, 1, 2, 121})
    void postTask_tituloInvalido_devuelve400(int longitud) throws Exception {
        String titulo = "a".repeat(longitud);
        String body = """
                { "title": "%s", "priority": "LOW" }
                """.formatted(titulo);

        mockMvc.perform(post("/projects/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchStatus_sinAssignee_devuelve422() throws Exception {
        // El service (mockeado) traduce la regla a TaskStateException; el advice la mapea a 422.
        when(taskService.cambiarStatus(6L, TaskStatus.DONE))
                .thenThrow(new TaskStateException("No se puede marcar DONE sin responsable."));

        mockMvc.perform(patch("/tasks/6/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"DONE\" }"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void deleteTask_inexistente_devuelve404() throws Exception {
        // eliminar es void -> doThrow(...).when(...) para simular el "no existe".
        doThrow(new TaskNotFoundException(999L)).when(taskService).eliminar(999L);

        mockMvc.perform(delete("/tasks/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getOverdue_soloVencidas_retorna200YListaEnOrden() throws Exception {
        try {
            Task vencida = new Task(7L, "Corregir bug de fechas", "desc", TaskStatus.IN_PROGRESS, Priority.MED, 1L, 1L, java.time.LocalDate.now().minusDays(1));
            when(taskService.vencidas()).thenReturn(List.of(vencida));

            mockMvc.perform(get("/tasks/overdue"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(7))
                    .andExpect(jsonPath("$[0].title").value("Corregir bug de fechas"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void getUnassigned_retorna200YAssigneeNull() throws Exception {
        try {
            Task t4 = new Task(4L, "Escribir tests MockMvc", "desc", TaskStatus.IN_PROGRESS, Priority.MED, 1L, null, java.time.LocalDate.now().plusDays(7));
            when(taskService.sinResponsable()).thenReturn(List.of(t4));

            mockMvc.perform(get("/tasks/unassigned"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(4))
                    .andExpect(jsonPath("$[0].assigneeId").value(nullValue()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ---- helpers de datos (reales, no mocks) ----

    private Task tarea(Long id, String title, TaskStatus status) {
        return tareaCon(id, title, status, 1L);
    }

    private Task tareaCon(Long id, String title, TaskStatus status, Long projectId) {
        try {
            return new Task(id, title, "desc", status, Priority.MED, projectId, 1L, null);
        } catch (TaskValidationException e) {
            throw new IllegalStateException("dato de prueba inválido", e);
        }
    }
}
```

</details>


---

<sub>28s</sub>

### Copilot

Leyendo GlobalExceptionHandler, TaskRepository y TaskOrders en paralelo para validar manejo de TaskValidationException, existencia del repo y del comparador de orden por título.

---

<sub>28s</sub>

### `view` — Failed

**view the file at C:\Users\User\taskflow-copilot-lizinzunza934\src\main\java\com\taskflow/exception/GlobalExceptionHandler.java.**

C:\Users\User\taskflow-copilot-lizinzunza934\src\main\java\com\taskflow/exception/GlobalExceptionHandler.java

```
Path does not exist
```


---

<sub>36s</sub>

### Copilot

Abriré en paralelo GlobalExceptionHandler, TaskRepository y TaskOrders para comprobar que TaskValidationException se mapea a 400, existe findByTitleContainingIgnoreCase y el comparador POR_TITULO. Ejecutando lecturas en paralelo.

---

<sub>36s</sub>

### `view` — Failed

**view the file at C:\Users\User\taskflow-copilot-lizinzunza934\src\main\java\com\taskflow\exception\GlobalExceptionHandler.java.**

C:\Users\User\taskflow-copilot-lizinzunza934\src\main\java\com\taskflow\exception\GlobalExceptionHandler.java

```
Path does not exist
```


---

<sub>36s</sub>

### `view`

**view the file at C:\Users\User\taskflow-copilot-lizinzunza934\src\main\java\com\taskflow\repository\TaskRepository.java.**

C:\Users\User\taskflow-copilot-lizinzunza934\src\main\java\com\taskflow\repository\TaskRepository.java

<details>
<summary>36 lines</summary>

```
package com.taskflow.repository;

import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * TaskRepository — EL MOMENTO NARRATIVO DEL DÍA (MP-6). Ayer era una interfaz que redeclaraba
 * save/findById/findAll/deleteById, implementada por InMemoryTaskRepository. HOY extiende
 * JpaRepository&lt;Task, Long&gt; y esos 4 métodos VIENEN GRATIS (+ count/existsById/etc.): NO se
 * implementa nadie, un PROXY de runtime lo hace (la misma "magia" de DI de S2D1).
 *
 * La paga de la interfaz extraída en S1D5: TaskService/ReportService dependían de ESTE contrato desde
 * entonces, y sus firmas (Optional&lt;Task&gt; findById, void deleteById) se alinearon a Spring Data
 * desde S2D1 — por eso hoy el swap memoria->BD NO toca el servicio. "git diff de TaskService = vacío".
 * InMemoryTaskRepository dejó de compilar y SE ELIMINÓ (git lo recuerda; borrar código es sano
 * cuando la interfaz sobrevive).
 *
 * DERIVED QUERIES: el NOMBRE del método es la query. Gramática findBy + propiedad + And/Not/
 * Containing/IgnoreCase/OrderBy, y countBy. Se validan AL ARRANCAR: un nombre con una propiedad que
 * no existe (p.ej. findByEstado) lanza QueryCreationException y la app NO levanta — error de arranque,
 * no de runtime. Estas seis se prueban en TaskRepositoryTest (@DataJpaTest).
 */
public interface TaskRepository extends JpaRepository<Task, Long> {

    /** Tareas en un estado dado. La consume ProjectService/ReportService y las vistas por estado. */
    List<Task> findByStatus(TaskStatus status);

    /** Tareas de un proyecto (por la columna project_id). La consume ProjectService.tareasDe (MP-6). */
    List<Task> findByProjectId(Long projectId);

    /** Combina dos criterios con And: tareas de un asignado en un estado. La consume el stretch de D5. */
    List<Task> findByAssigneeIdAndStatus(Long assigneeId, TaskStatus status);

    /** LIKE case-insensitive: título que CONTIENE el fragmento (la BD hace el filtrado). */
    List<Task> findByTitleContainingIgnoreCase(String fragment);

    /** El complemento de findByStatus: todas MENOS las de ese estado (Not). Sirve a "pendientes". */
    List<Task> findByStatusNot(TaskStatus status);

    /** Cuenta en la BD (no trae filas para contarlas): conteo por estado. */
    long countByStatus(TaskStatus status);
}
```

</details>


---

<sub>36s</sub>

### `view`

**view the file at C:\Users\User\taskflow-copilot-lizinzunza934\src\main\java\com\taskflow\service\TaskOrders.java.**

C:\Users\User\taskflow-copilot-lizinzunza934\src\main\java\com\taskflow\service\TaskOrders.java

<details>
<summary>45 lines</summary>

```
package com.taskflow.service;

import com.taskflow.model.Task;

import java.util.Comparator;

/**
 * TaskOrders — catálogo de ESTRATEGIAS de ordenamiento de tareas (MP-3, patrón Strategy).
 *
 * Cada constante es una estrategia empaquetada como VALOR (un {@link Comparator}) que se PASA a
 * quien ordena (el contexto: {@code sorted(...)} de los streams). Es el mismo patrón que ya usaron
 * en S1D3/D4 sin nombre: un Comparator es una estrategia, y sort/sorted es el contexto que la
 * recibe sin saber cuál le tocó. Aquí les damos NOMBRE y las juntamos en un solo lugar reutilizable.
 *
 * Se reutiliza tal cual en S2: {@code TaskService.listar()} de la API ordena con POR_URGENCIA.
 *
 * Clase de utilidades: constructor privado (no se instancia — solo se usan sus constantes).
 */
public final class TaskOrders {

    private TaskOrders() {
        // no instanciable: es un catálogo de constantes
    }

    /**
     * POR_FECHA — la estrategia "de siempre" (S1D4): dueDate ascendente, con las tareas SIN fecha
     * (dueDate null) al FINAL. nullsLast evita el NullPointerException al comparar una fecha null.
     */
    public static final Comparator<Task> POR_FECHA =
            Comparator.comparing(Task::getDueDate,
                    Comparator.nullsLast(Comparator.naturalOrder()));

    /** POR_TITULO — orden alfabético por título, sin distinguir mayúsculas/minúsculas. */
    public static final Comparator<Task> POR_TITULO =
            Comparator.comparing(Task::getTitle, String.CASE_INSENSITIVE_ORDER);

    /**
     * POR_URGENCIA — la estrategia NUEVA de MP-3, encadenando comparing/thenComparing de S1D3:
     *   1) las VENCIDAS primero (estaVencida() == true antes que false),
     *   2) a igualdad, prioridad de mayor a menor (HIGH -> MED -> LOW),
     *   3) a igualdad, dueDate ascendente con las sin fecha al final.
     *
     * Detalle: el orden natural del enum Priority es LOW, MED, HIGH (LOW el menor); por eso
     * reverseOrder() deja HIGH primero. Y reverseOrder() sobre el boolean de estaVencida() deja
     * el true (vencida) antes que el false.
     */
    public static final Comparator<Task> POR_URGENCIA =
            Comparator.comparing(Task::estaVencida, Comparator.reverseOrder())
                    .thenComparing(Task::getPriority, Comparator.reverseOrder())
                    .thenComparing(Task::getDueDate,
                            Comparator.nullsLast(Comparator.naturalOrder()));
}
```

</details>


---

<sub>45s</sub>

### `view`

**view the file at C:\Users\User\taskflow-copilot-lizinzunza934\src\main\java\com\taskflow\controller\TaskController.java.**

C:\Users\User\taskflow-copilot-lizinzunza934\src\main\java\com\taskflow\controller\TaskController.java (lines 1-200)

<details>
<summary>148 lines</summary>

```
package com.taskflow.controller;

import com.taskflow.dto.TaskRequest;
import com.taskflow.dto.TaskResponse;
import com.taskflow.dto.TaskStatusUpdateRequest;
import com.taskflow.exception.ProjectNotFoundException;
import com.taskflow.exception.TaskNotFoundException;
import com.taskflow.exception.TaskValidationException;
import com.taskflow.mapper.TaskMapper;
import com.taskflow.model.Priority;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.service.ProjectService;
import com.taskflow.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * TaskController — la PUERTA HTTP de las tareas. HOY se completa el CRUD y la entidad Task deja de
 * salir por la puerta: toda firma habla en DTOs (TaskRequest / TaskResponse).
 *
 * Contrato de responsabilidades (T6): el controller traduce HTTP <-> dominio y delega. Novedad de
 * hoy: además de TaskService, inyecta ProjectService para el cableo Task↔Project (validar que el
 * proyecto exista al crear una tarea bajo su path).
 *
 * La checked TaskValidationException se declara con throws y SUBE al GlobalExceptionHandler (que la
 * mapea a 400). No se atrapa aquí: contraste con la IOException de S1D5 (aquélla se manejaba en su
 * capa porque nadie más podía; ésta tiene un handler global esperándola).
 */
@RestController
@Tag(name = "Tasks", description = "CRUD de tareas: crear bajo un proyecto, leer, reemplazar, cambiar estado y borrar.")
public class TaskController {

    private final TaskService taskService;
    private final ProjectService projectService;

    public TaskController(TaskService taskService, ProjectService projectService) {
        this.taskService = taskService;
        this.projectService = projectService;
    }

    /** GET /tasks — todas o filtradas por ?status= / ?priority= (stretch). Devuelve TaskResponse. */
    @Operation(summary = "Lista tareas",
            description = "Todas las tareas, o filtradas por ?status= / ?priority= (stretch). Devuelve TaskResponse.")
    @GetMapping("/tasks")
    public List<TaskResponse> getTasks(
            @RequestParam(name = "status", required = false) TaskStatus status,
            @RequestParam(name = "priority", required = false) Priority priority) {
        List<Task> tareas;
        if (status != null) {
            tareas = taskService.porEstado(status);
        } else if (priority != null) {                 // STRETCH: mismo patrón que ?status=
            tareas = taskService.porPrioridad(priority);
        } else {
            tareas = taskService.listar();
        }
        return tareas.stream().map(TaskMapper::aResponse).toList();
    }

    /** GET /tasks/{id} — 200 con TaskResponse, o 404 uniforme (orElseThrow -> advice). */
    @Operation(summary = "Obtiene una tarea por id",
            description = "200 con el TaskResponse; 404 uniforme si el id no existe.")
    @GetMapping("/tasks/{id}")
    public TaskResponse getTaskPorId(@PathVariable("id") Long id) {
        Task task = taskService.buscarPorId(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        return TaskMapper.aResponse(task);
    }

    @Operation(summary = "Lista tareas vencidas",
            description = "Lista las tareas vencidas de todos los proyectos.")
    @GetMapping("/tasks/overdue")
    public List<TaskResponse> getOverdueTasks() {
        List<Task> tareas = taskService.vencidas();
        return tareas.stream().map(TaskMapper::aResponse).toList();
    }

    @Operation(summary = "Lista tareas sin responsable",
            description = "Lista las tareas sin responsable de todos los proyectos para repartirlas.")
    @GetMapping("/tasks/unassigned")
    public List<TaskResponse> getUnassignedTasks() {
        List<Task> tareas = taskService.sinResponsable();
        return tareas.stream().map(TaskMapper::aResponse).toList();
    }

    @Operation(summary = "Busca tareas por título",
            description = "Busca en todos los proyectos tareas cuyo título contiene el parámetro 'q' (case-insensitive). Devuelve 400 si 'q' falta o queda vacío.")
    @GetMapping("/tasks/search")
    public List<TaskResponse> searchTasks(@RequestParam(name = "q", required = false) String q) throws TaskValidationException {
        List<Task> tareas = taskService.buscarPorTitulo(q);
        return tareas.stream().map(TaskMapper::aResponse).toList();
    }

    /**
     * POST /projects/{projectId}/tasks — crea una tarea bajo un proyecto. 201 + header Location
     * apuntando a donde el recurso SE LEE (/tasks/{id}), no a la URL de creación. @Valid dispara Bean
     * Validation (400 si falla). Cableo: si el proyecto no existe -> 404 (ProjectNotFoundException).
     */
    @Operation(summary = "Crea una tarea dentro de un proyecto",
            description = "El proyecto debe existir (404 si no). La tarea nace en estado TODO.")
    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<TaskResponse> createTask(@PathVariable("projectId") Long projectId,
                                                   @Valid @RequestBody TaskRequest request)
            throws TaskValidationException {
        projectService.buscarPorId(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
        Task creada = taskService.crear(request, projectId);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/tasks/{id}")
                .buildAndExpand(creada.getId())
                .toUri();
        return ResponseEntity.created(location).body(TaskMapper.aResponse(creada));
    }

    /** PUT /tasks/{id} — reemplazo COMPLETO. 200 con TaskResponse; 404 si no existe. */
    @Operation(summary = "Reemplaza una tarea por completo",
            description = "Semántica PUT: conserva id, status y projectId; el resto viene del cuerpo.")
    @PutMapping("/tasks/{id}")
    public TaskResponse updateTask(@PathVariable("id") Long id,
                                   @Valid @RequestBody TaskRequest request)
            throws TaskValidationException {
        Task actualizada = taskService.reemplazar(id, request);
        return TaskMapper.aResponse(actualizada);
    }

    /** DELETE /tasks/{id} — 204 No Content; 404 uniforme si no existe (advice). */
    @Operation(summary = "Borra una tarea",
            description = "204 No Content; 404 uniforme si la tarea no existe.")
    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable("id") Long id) {
        taskService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /tasks/{id}/status — cambia SOLO el estado. 200 con TaskResponse; 404 si no existe; y
     * 422 si la regla de estado lo prohíbe (DONE sin assignee), vía TaskStateException.
     */
    @Operation(summary = "Cambia el estado de una tarea",
            description = "TODO/IN_PROGRESS/DONE. Pasar a DONE sin responsable responde 422.")
    @PatchMapping("/tasks/{id}/status")
    public TaskResponse patchStatus(@PathVariable("id") Long id,
                                    @Valid @RequestBody TaskStatusUpdateRequest request) {
        Task actualizada = taskService.cambiarStatus(id, request.status());
        return TaskMapper.aResponse(actualizada);
    }
}
```

</details>


---

<sub>51s</sub>

### Copilot

Abrir com/taskflow/exception para localizar GlobalExceptionHandler y TaskValidationException (lecturas en paralelo).

---

<sub>51s</sub>

### `view`

**view the file at C:\Users\User\taskflow-copilot-lizinzunza934\src\main\java\com\taskflow\exception.**

C:\Users\User\taskflow-copilot-lizinzunza934\src\main\java\com\taskflow\exception (lines 1-200)

```
ProjectNotFoundException.java
TaskNotFoundException.java
TaskStateException.java
TaskValidationException.java
UsernameAlreadyExistsException.java
```


---

<sub>54s</sub>

### `view`

**view the file at C:\Users\User\taskflow-copilot-lizinzunza934\src\main\java\com\taskflow\exception\TaskValidationException.java.**

C:\Users\User\taskflow-copilot-lizinzunza934\src\main\java\com\taskflow\exception\TaskValidationException.java

<details>
<summary>16 lines</summary>

```
package com.taskflow.exception;

/**
 * TaskValidationException — excepción CHECKED (extiende Exception) para reglas de
 * negocio violadas por input del usuario: algo esperable y recuperable en el CLI.
 *
 * Al ser checked, el compilador OBLIGA al menú a decidir qué hacer (avisar y volver
 * al menú) en vez de dejar que el programa reviente.
 *
 * Nombre y paquete canónicos de S1 (no cambian en toda la semana):
 * com.taskflow.exception.TaskValidationException.
 */
public class TaskValidationException extends Exception {

    public TaskValidationException(String mensaje) {
        super(mensaje);
    }
}
```

</details>


---

<sub>1m 24s</sub>

### `view`

**view the file at C:\Users\User\taskflow-copilot-lizinzunza934\src\main\java\com\taskflow/service\TaskService.java.**

C:\Users\User\taskflow-copilot-lizinzunza934\src\main\java\com\taskflow/service\TaskService.java (lines 1-400)

<details>
<summary>144 lines</summary>

```
package com.taskflow.service;

import com.taskflow.dto.TaskRequest;
import com.taskflow.exception.TaskNotFoundException;
import com.taskflow.exception.TaskStateException;
import com.taskflow.exception.TaskValidationException;
import com.taskflow.mapper.TaskMapper;
import com.taskflow.model.Priority;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * TaskService — la capa de negocio de las tareas. Orquesta; las reglas viven en Task (título 3-120,
 * projectId obligatorio, dueDate no en el pasado al crear, no DONE sin assignee).
 *
 * Evolución de HOY (S2D3) sobre el estado de D2:
 *   - crear(...) ya NO usa un projectId demo (murió el 1L): recibe el projectId del PATH y arma la
 *     entidad con el TaskMapper (que pasa por la factory Task.crear).
 *   - nacen reemplazar (PUT), eliminar (DELETE) y cambiarStatus (PATCH); completar(...) de D1 queda
 *     DELEGANDO en cambiarStatus (refactor de MP-9).
 *   - cambiarStatus TRADUCE la checked TaskValidationException que lanza setStatus a la unchecked
 *     TaskStateException -> el advice la mapea a 422. Sin tocar el dominio.
 *
 * La existencia del proyecto al crear (404) se valida en el controller (contra ProjectService): este
 * service no depende del repositorio de proyectos, y así el filtrado por proyecto vive solo en
 * ProjectService.tareasDe (promesa de D4: "no tocar TaskService").
 */
@Service
public class TaskService {

    private final TaskRepository repository;

    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    /**
     * Crea una tarea a partir del DTO de entrada y el projectId del PATH. El TaskMapper pasa por la
     * factory Task.crear (status=TODO, id=null, regla "dueDate no en el pasado"); el repositorio le
     * asigna el id en save. La checked sube al advice si el título o la fecha no cumplen (-> 400).
     */
    public Task crear(TaskRequest request, Long projectId) throws TaskValidationException {
        Task nueva = TaskMapper.aEntidadNueva(request, projectId);
        return repository.save(nueva);
    }

    /**
     * Reemplazo COMPLETO (PUT): busca la tarea (404 si no existe), y arma una tarea rehidratada con el
     * MISMO id, conservando status y projectId actuales; el resto viene del request. save hace upsert.
     */
    public Task reemplazar(Long id, TaskRequest request) throws TaskValidationException {
        Task actual = repository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        Task reemplazo = TaskMapper.aReemplazo(id, actual.getStatus(), actual.getProjectId(), request);
        return repository.save(reemplazo);
    }

    /**
     * Elimina una tarea (DELETE): existe -> borra (204 en el controller); no existe -> TaskNotFound
     * -> el advice responde 404. El findById previo hace explícito el "no existe" (Optional de S1D4).
     */
    public void eliminar(Long id) {
        repository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        repository.deleteById(id);
    }

    /**
     * Cambia el estado (PATCH): busca la tarea (404 si no existe) y aplica setStatus, que puede lanzar
     * la CHECKED TaskValidationException por la regla "no DONE sin assignee". La CAPTURA y la TRADUCE
     * a la unchecked TaskStateException -> el advice la mapea a 422. El dominio (setStatus) no se toca.
     */
    public Task cambiarStatus(Long id, TaskStatus nuevoStatus) {
        Task task = repository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        try {
            task.setStatus(nuevoStatus);
        } catch (TaskValidationException e) {
            throw new TaskStateException(e.getMessage());
        }
        return repository.save(task);
    }

    /**
     * Completa una tarea (la pasa a DONE). Tras MP-9 queda DELEGANDO en cambiarStatus(id, DONE): un
     * solo camino para "cambiar de estado", que además hereda la traducción a 422.
     */
    public Task completar(Long id) {
        return cambiarStatus(id, TaskStatus.DONE);
    }

    /**
     * Lista todas las tareas ordenadas con la estrategia POR_URGENCIA (reuso LITERAL de la Strategy de
     * S1): vencidas primero, luego prioridad HIGH->LOW, luego fecha ascendente (nulls al final).
     */
    public List<Task> listar() {
        return repository.findAll().stream()
                .sorted(TaskOrders.POR_URGENCIA)
                .toList();
    }

    /** Filtra las tareas por estado (?status=). '==' entre enums es seguro. */
    public List<Task> porEstado(TaskStatus status) {
        return repository.findAll().stream()
                .filter(t -> t.getStatus() == status)
                .toList();
    }

    /** Lista las tareas vencidas según Task.estaVencida() y orden por fecha ascendente. */
    public List<Task> vencidas() {
        return repository.findAll().stream()
                .filter(Task::estaVencida)
                .sorted(TaskOrders.POR_FECHA)
                .toList();
    }

    /** Busca una tarea por id: delega en el repositorio y deja subir el Optional TAL CUAL. */
    public Optional<Task> buscarPorId(Long id) {
        return repository.findById(id);
    }

    /** STRETCH — filtro por prioridad (?priority=): MISMO patrón que porEstado. */
    public List<Task> porPrioridad(Priority priority) {
        return repository.findAll().stream()
                .filter(t -> t.getPriority() == priority)
                .toList();
    }

    /**
     * Lista las tareas sin responsable (assigneeId == null) y ordenadas por fecha ascendente
     * (TaskOrders.POR_FECHA). Reusa el predicado nombrado en ReportService.SIN_ASIGNAR — no reescribir
     * la lógica de null-check aquí.
     */
    public List<Task> sinResponsable() {
        return repository.findAll().stream()
                .filter(ReportService.SIN_ASIGNAR)
                .sorted(TaskOrders.POR_FECHA)
                .toList();
    }

    /**
     * Busca tareas cuyo título contiene la cadena q (case-insensitive).
     * q se trimea con strip(); si es null o queda vacío se lanza TaskValidationException
     * con el mensaje requerido por la especificación.
     */
    public List<Task> buscarPorTitulo(String q) throws TaskValidationException {
        if (q == null || q.strip().isEmpty()) {
            throw new TaskValidationException("El parámetro 'q' es obligatorio.");
        }
        String trimmed = q.strip();
        List<Task> encontrados = repository.findByTitleContainingIgnoreCase(trimmed);
        return encontrados.stream()
                .sorted(TaskOrders.POR_TITULO)
                .toList();
    }
}
```

</details>


---

<sub>2m 25s</sub>

### Copilot

Resumen breve: comparé el diff con specs/search.md y los archivos relevantes.

| # | Gravedad | Archivo:línea | Punto checklist | Qué pasa | Qué harías |
|---|---|---|---|---|---|
| 1 | Bloquea | -- | 1 / 4 (Contrato & handler) | No pude abrir/ localizar GlobalExceptionHandler (no aparece en src/main/java/com/taskflow/exception). La especificación depende de que TaskValidationException se mapee a 400. | Confirmar la existencia del advice que mapea TaskValidationException a 400. Si falta, agregar GlobalExceptionHandler que convierta TaskValidationException → 400 con ErrorResponse { message }. |
| 2 | Corregir | src/test/java/com/taskflow/unit/BusquedaTareasServiceTest.java | 7 (tests que deben existir) | El test verifica que se lance TaskValidationException para null/"" pero NO verifica que el repository nunca se llame (spec lo exige). | Añadir verify(taskRepository, never()).findByTitleContainingIgnoreCase(...) para los casos null y "   ". |
| 3 | Sugerencia | src/test/java/com/taskflow/slice/BusquedaTareasControllerTest.java | 5 / Convenciones de slice | Hay imports/variables no usados (Optional import) y uso directo de Task constructor en el slice — coherente con otros tests pero revisar limpieza. | Eliminar imports no usados; mantener patrón del slice. |
| 4 | Sugerencia | src/main/java/com/taskflow/controller/TaskController.java : ~98 | 4 (Contrato: ruta y @RequestParam) | Endpoint y @RequestParam(required=false) cumplen la spec; buena javadoc/Operation añadida. | Ninguna acción necesaria. |

Casos sin test:
- El unit test no comprueba explícitamente "el repositorio nunca se llama" para null/vacío (debiera estar).  
- Ninguno otro: slice y unit cubren los casos pedidos (ordenamiento, trim, 200 con body esperado, 400 cuando falta q).

Veredicto: CAMBIOS PEDIDOS — corregir el unit test y confirmar/añadir el GlobalExceptionHandler que mapea TaskValidationException a 400 con el mensaje "El parámetro 'q' es obligatorio.".

---

<sub>Generated by [GitHub Copilot CLI](https://github.com/features/copilot/cli)</sub>
