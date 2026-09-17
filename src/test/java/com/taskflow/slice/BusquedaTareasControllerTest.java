package com.taskflow.slice;

import com.taskflow.controller.TaskController;
import com.taskflow.model.Task;
import com.taskflow.model.Priority;
import com.taskflow.model.TaskStatus;
import com.taskflow.security.JwtAuthenticationFilter;
import com.taskflow.service.ProjectService;
import com.taskflow.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice web de GET /tasks/search: solo la capa HTTP; la lógica se prueba en el unit.
 */
@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
class BusquedaTareasControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getSearch_con_q_devuelve200YJson() throws Exception {
        Task t1 = new Task(9L, "Documentar la API con Swagger", "d", TaskStatus.TODO, Priority.MED, 1L, null, LocalDate.now().plusDays(10));
        Task t2 = new Task(5L, "Optimizar consultas de la API", "d", TaskStatus.TODO, Priority.MED, 1L, null, LocalDate.now().plusDays(5));

        when(taskService.buscarPorTitulo("api")).thenReturn(List.of(t1, t2));

        mockMvc.perform(get("/tasks/search").param("q", "api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9))
                .andExpect(jsonPath("$[0].title").value("Documentar la API con Swagger"))
                .andExpect(jsonPath("$[1].id").value(5))
                .andExpect(jsonPath("$[1].title").value("Optimizar consultas de la API"));
    }

    @Test
    void getSearch_sin_q_servicioLanzaValidation_devuelve400() throws Exception {
        when(taskService.buscarPorTitulo((String) null)).thenThrow(new com.taskflow.exception.TaskValidationException("El parámetro 'q' es obligatorio."));

        mockMvc.perform(get("/tasks/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El parámetro 'q' es obligatorio."));
    }
}
