package com.taskflow.unit;

import com.taskflow.exception.TaskValidationException;
import com.taskflow.model.Priority;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.repository.TaskRepository;
import com.taskflow.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit de TaskService.buscarPorTitulo: repositorio mockeado, tareas reales.
 */
@ExtendWith(MockitoExtension.class)
class BusquedaTareasServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService service;

    @Test
    void buscarPorTitulo_ordenaYTrimeaYVerificaLlamada() throws TaskValidationException {
        // dos tareas devueltas en orden NO alfabético
        Task t1 = new Task(5L, "Optimizar consultas de la API", "d", TaskStatus.TODO, Priority.MED, 1L, null, LocalDate.now().plusDays(5));
        Task t2 = new Task(9L, "Documentar la API con Swagger", "d", TaskStatus.TODO, Priority.MED, 1L, null, LocalDate.now().plusDays(10));

        when(taskRepository.findByTitleContainingIgnoreCase("api")).thenReturn(List.of(t1, t2));

        List<Task> resultado = service.buscarPorTitulo("  api ");

        // debe venir ordenado por título: Documentar... (id 9) antes que Optimizar... (id 5)
        assertEquals(List.of(t2, t1), resultado);
        verify(taskRepository).findByTitleContainingIgnoreCase("api");
    }

    @Test
    void buscarPorTitulo_nullYLimpio_lanzanTaskValidationException() {
        assertThrows(TaskValidationException.class, () -> service.buscarPorTitulo(null));
        assertThrows(TaskValidationException.class, () -> service.buscarPorTitulo("   "));
    }
}
