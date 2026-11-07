# Revisión de código del gestor de turnos

**Equipo:** Pablo Mariasch y Bruno Ludueña  
**Versión revisada:** `estadorefactorizado`

Comparamos el piloto con la versión corregida y revisamos un cambio por cada refactorización. Nos repartimos los controles para que cada uno pudiera seguir en el código los cambios que había implementado su compañero.

## Controles que revisamos

| Quién revisó | Commit que revisamos | Qué comprobamos |
|---|---|---|
| Pablo Mariasch | Extract Method: centralizar la validación de disponibilidad | Seguimos las operaciones de reserva y reprogramación en `TurnoService.java`. Comprobamos que ambas llaman a `validarDisponibilidad()`. En los casos 12 y 16, la agenda rechazó con HTTP 409 los dos intentos de ocupar las 10:30 durante un tratamiento de 10:00 a 11:00. |
| Pablo Mariasch | Introduce Parameter Object: agrupar los datos de la reserva en ReservaRequest | Seguimos el recorrido del formulario desde `ApiController` hasta `TurnoService.reservar()`. Comprobamos que el nombre, el teléfono, el servicio, la fecha y la hora llegan juntos en `ReservaRequest`. En el caso 7, un teléfono con letras dio HTTP 400. En el caso 11, una reserva válida dio HTTP 201. |
| Bruno Ludueña | Move Method: trasladar el cálculo de superposición a Turno | Revisamos la comparación de horarios en `Turno.seSolapa()`. Con un tratamiento reservado de 10:00 a 11:00, comprobamos que la consulta de las 10:30 se rechazó y que la de las 11:00 se aceptó. Así verificamos que un turno puede empezar justo cuando termina el anterior. |
| Bruno Ludueña | Extract Class: separar la persistencia de los turnos | Revisamos cómo `ArchivoTurnoRepository` lee la agenda. En una prueba manual iniciamos el sistema con un archivo temporal que tenía una fila incompleta. La carga se interrumpió y vimos el mensaje «Datos corruptos en línea 1». |

## Observación y conclusión

En el piloto encontramos la misma validación en dos partes de `App.java`. Si cambiábamos el horario de cierre en una sola, reservar y reprogramar podían dar respuestas distintas. Con Extract Method reunimos esa regla en un método y comprobamos que las dos operaciones lo usan.

También observamos que el piloto podía omitir una fila incompleta al recuperar la agenda. Al separar la persistencia, incorporamos el número de línea al mensaje de error. La prueba con el archivo temporal nos permitió verificar esa corrección por separado.

Aceptamos los cuatro cambios para el alcance del piloto. Las dos versiones aprobaron las 26 comprobaciones del contrato HTTP y la versión corregida identificó la fila dañada en la prueba manual.
