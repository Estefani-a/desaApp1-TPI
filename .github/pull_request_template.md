## Qué cambia

<!-- Una línea con el objetivo del PR y, debajo, una viñeta por cambio importante:
     qué archivo o pieza se agrega o cambia y para qué. -->

-
-

## RF / tarea

<!-- Código de tarea de DIVISION_TRABAJO.md y RF, si corresponde (ej. "E1 (D1). No implementa un RF"
     o "RF03 · tarea N4 (D2)"). Si destraba a alguien, decí a quién. -->

## Cómo probarlo

<!-- Pasos concretos: comando, pantalla, datos de prueba, modo avión... -->

1. Desde `mobile/`: `./gradlew assembleDebug lintDebug testDebugUnitTest`.
2.

## Capturas

<!-- Solo si toca UI: capturas o video. Si no, borrá la sección. -->

## Decisiones

<!-- Opcional. Lo que se aparta de la preentrega o de CONVENCIONES, o que alguien podría
     preguntar en la revisión: qué se decidió y por qué. Si no hay, borrá la sección. -->

## Notas para el grupo

<!-- Qué tiene que saber o hacer cada uno a partir de este PR: imports que cambian,
     conflictos esperables, tareas que se destraban, algo que queda pendiente.
     Nombrá a la persona. Si no hay nada, borrá la sección. -->

- **Nombre:**
## Checklist (Definition of Done)

<!-- Borrá los ítems que no aplican a este PR (por ejemplo, los de Backend en un PR de la app).
     Los que quedan sin tildar se tildan antes de pedir la aprobación. -->

**General**
- [ ] Compila y pasan Lint y los tests en CI.
- [ ] Hay tests para la lógica nueva.
- [ ] Corrí `/code-review` sobre mi rama y resolví o respondí los hallazgos.
- [ ] Puedo explicar cada línea, incluida la generada con IA.

**App**
- [ ] Respeta la regla de capas (`domain` sin Android; el ViewModel no importa nada de `data`).
- [ ] La UI lee de Room (Flow); las escrituras siguen el flujo `isSync` + WorkManager.
- [ ] La pantalla resuelve cargando, contenido, vacío, error y sin conexión (o explico en Decisiones por qué alguno no aplica).
- [ ] Sin textos hardcodeados ni colores fuera del tema; íconos accionables con `contentDescription`.
- [ ] Errores manejados con `AppResult`; no hay `!!` ni `catch (e: Exception)` que se trague la cancelación.

**Backend**
- [ ] El controller solo delega; las reglas están en el Service; no se devuelven entidades JPA.
- [ ] Si cambia el contrato, `docs/API.md` está actualizado en este mismo PR.
