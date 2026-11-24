## Qué cambia

<!-- Resumen corto del cambio y por qué. -->

## RF / tarea

<!-- RF implementado y código de tarea (ej. RF03, tarea D2). -->

## Cómo probarlo

<!-- Pasos para verificar el cambio. -->

## Capturas

<!-- Si toca UI: capturas o video. Si no, borrar la sección. -->

## Checklist (Definition of Done)

- [ ] Compila y pasan Lint y los tests en CI.
- [ ] App: respeta la regla de capas (`domain` sin Android; el ViewModel no importa nada de `data`).
- [ ] App: la UI lee de Room (Flow); las escrituras siguen el flujo `isSync` + WorkManager.
- [ ] App: la pantalla resuelve cargando, contenido, vacío, error y sin conexión (o el PR explica por qué alguno no aplica).
- [ ] App: sin textos hardcodeados ni colores fuera del tema; íconos accionables con `contentDescription`.
- [ ] App: errores manejados con `AppResult`; no hay `!!` ni `catch (e: Exception)` que se trague la cancelación.
- [ ] Backend: el controller solo delega; las reglas están en el Service; no se devuelven entidades JPA.
- [ ] Backend: si cambia el contrato, `docs/API.md` está actualizado en el mismo PR.
- [ ] Hay tests para la lógica nueva.
- [ ] Corrí `/code-review` sobre mi rama y resolví o respondí los hallazgos.
- [ ] Puedo explicar cada línea, incluida la generada con IA (§13).
