Diagnóstico del Gestor de turnos piloto

Al ejecutar el piloto, las 26 comprobaciones del contrato HTTP pasaron. Al revisar la estructura interna, identificamos ocho problemas de mantenimiento en src/App.java.

Problema

Evidencia en App.java

Riesgo al modificarlo

Prioridad

1. Clase con varias tareas

Líneas 20–203. App recibe pedidos HTTP, aplica reglas de agenda y guarda turnos.

Cambiar el almacenamiento obliga a modificar la misma clase que atiende las reservas.

Alta

2. Método extenso

Líneas 62–183. atender() resuelve varias rutas dentro de un mismo método.

Cuesta identificar qué operaciones dependen de un bloque de código.

Alta

3. Código duplicado

Líneas 108–127 y 148–167. Reservar y reprogramar repiten la validación del horario y del solapamiento.

Si cambia una sola copia de la regla, las operaciones pueden dar respuestas distintas para el mismo horario.

Alta

4. Datos sin tipos propios

Líneas 32–36. Cada turno se representa con un mapa cuyas claves y valores son cadenas de texto.

Una clave mal escrita pasa la compilación y puede producir un error al consultar el turno.

Alta

5. Valores repetidos

Líneas 69–70 y 111. Las duraciones de 30 y 60 minutos se definen en el catálogo y se vuelven a indicar al reservar.

La duración publicada puede diferir de la que usa la agenda para calcular la disponibilidad.

Media

6. Datos de reserva dispersos

Líneas 99–103. Paciente, teléfono, servicio, fecha y hora se leen en cinco variables separadas.

Al trasladar la operación a otro método, es fácil omitir un campo.

Media

7. Acceso al archivo acoplado

Líneas 25–40 y 185–190. App conoce el orden de los campos y se encarga de leerlos y escribirlos.

Un cambio en el formato del archivo obliga a modificar App.

Alta

8. Errores silenciados

Líneas 31 y 38–39. Se omiten filas con una cantidad incorrecta de campos y se ignoran errores al interpretar el ID.

Una reserva dañada puede pasar inadvertida al iniciar la agenda.

Alta