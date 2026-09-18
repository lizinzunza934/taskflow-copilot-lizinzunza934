# Proyecto final · Semana 6 · GitHub Copilot

**Alumno:** Lizbeth Joseline Inzunza Pereyra · **Usuario de GitHub:** lizinzunza934

## 1. Qué construí

| | Feature | Especificación |
|---|---|---|
| [x] | `GET /tasks/search?q=` — buscar tareas por título | [`specs/search.md`](../specs/search.md) |

## 2. El pull request

- **URL del PR (mergeado):** https://github.com/lizinzunza934/taskflow-copilot-lizinzunza934/pull/5
- **Commit del merge en `main`:** `7b1fabd (HEAD -> main, origin/main) Merge pull request #5 from lizinzunza934/feature/search`
- **Comentarios de Copilot code review:** 0 (se me fue y el PR se fusionó antes de que la IA emitiera la revisión automática).

## 3. Cómo lo hice

| Paso | Qué hice | Evidencia |
|---|---|---|
| Rama y spec | `git switch -c feature/search` y copié la spec a `specs/` | `git log --oneline main..feature/search` (antes del merge) |
| Implementación | `copilot -p "/crear-endpoint-taskflow …"` con `gpt-5-mini` | `semana6/sesion-implementacion.md` (tiene la línea `Skill "crear-endpoint-taskflow" loaded successfully`) |
| Revisión | agente `revisor` sobre `semana6/proyecto-final.diff` | `semana6/revision.md` (termina con `Veredicto:`) |
| Tests | `mvn test` en verde | 81 |
| Comprobación REST | `verificar.ps1` con `casos-search.ps1` | sección 5 de este documento |
| Code review | Copilot en el PR | la pestaña *Files changed* del PR |

## 4. Qué hizo el agente y qué corregí yo

| # | Qué hizo mal el agente (archivo) | Quién lo detectó | Cómo quedó corregido |
|---|---|---|---|
| 1 | Faltaba `verify(..., never())` en el test unitario (`BusquedaTareasServiceTest.java`) | agente `revisor` | Corregido automáticamente ejecutando el prompt de corrección (`copilot -p`) con los hallazgos de la tabla. |
| 2 | Dejó un import sin usar en el controlador de pruebas slice (`BusquedaTareasControllerTest.java`) | agente `revisor` | Corregido automáticamente con el mismo prompt de corrección del paso anterior. |

**Lo que el agente hizo bien a la primera:** La lógica de filtrado insensible a mayúsculas/minúsculas, el DTO y el manejo de excepciones (HTTP 400) se generó correctamente en `src/main` sin requerir modificaciones.

## 5. Comprobaciones REST

```text
Repositorio: C:\Users\User\taskflow-copilot-lizinzunza934
URL de la app: [http://127.0.0.1:8080](http://127.0.0.1:8080)
Empaquetando con Maven (mvn -q package -DskipTests), tarda unos segundos...
App arrancando (PID 27080). Esperando a que /info responda...
App lista en 18 s.
[OK]    GET /tasks/overdue devuelve solo la tarea 7
[OK]    GET /tasks/unassigned devuelve las tareas 4 y 6
[OK]    GET /projects/1/summary
[OK]    GET /projects/2/summary
[OK]    GET /projects/3/summary
[OK]    GET /projects/99/summary responde 404
[OK]    GET /projects/1/summary sin token responde 401
[OK]    GET /tasks/search?q=api devuelve 9 y 5, en ese orden
[OK]    GET /tasks/search?q=API devuelve lo mismo (no distingue mayúsculas)
[OK]    GET /tasks/search?q=zzz devuelve 200 con []
[OK]    GET /tasks/search?q=(espacios) responde 400 con el mensaje de la spec
[OK]    GET /tasks/search sin q responde 400 con el mensaje de la spec
[OK]    GET /tasks/search sin token responde 401
App detenida (PID 27080).
[OK]    App apagada: el puerto 8080 ya no responde
RESULTADO: 14/14 OK

## 6. Créditos de la semana

| Qué | AI credits |
|---|---|
| Usados en septiembre según github.com | 154 |
| Implementación con la skill (`AI Credits` del PF-2) | 23.7 |
| Revisión del `revisor` (`AI Credits` del PF-3) | 2.7 |
| Correcciones del PF-4 y del PF-6, si hubo (`AI Credits`) | 3.27 |