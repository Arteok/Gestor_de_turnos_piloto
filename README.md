# Gestor de turnos piloto corregido

Proyecto académico de Metodología de Sistemas II.

Desarrollamos un prototipo para gestionar turnos en un consultorio de podología. El paciente selecciona un servicio, una fecha y un horario. El profesional puede consultar la agenda, cancelar una reserva o reprogramarla.

## Versiones del proyecto

### Gestor de turnos piloto

Primera implementación del sistema. La clase `App.java` recibía las solicitudes HTTP, validaba los horarios y guardaba los turnos.

### Gestor de turnos piloto corregido

Versión refactorizada del piloto. Mantiene el mismo comportamiento externo, pero separa las responsabilidades en varias clases.

## Reglas del sistema

- La agenda atiende de 09:00 a 18:00.
- Una consulta podológica dura 30 minutos.
- Un tratamiento podológico dura 60 minutos.
- No se permiten turnos superpuestos.
- Un turno puede comenzar cuando termina el turno anterior.
- Se pueden reservar, consultar, cancelar y reprogramar turnos.
- Los turnos se guardan en `data/turnos.txt`.

## Tecnologías utilizadas

- Java SE
- HTML
- CSS
- JavaScript
- Persistencia en archivo de texto
- HTTP Server de Java

## Estructura del proyecto

```text
src/
├── ApiController.java
├── ArchivoTurnoRepository.java
├── App.java
├── EstadoTurno.java
├── Paciente.java
├── ReservaRequest.java
├── Servicio.java
├── Turno.java
└── TurnoService.java

web/
├── app.js
├── index.html
└── style.css

tests/
└── ContratoHTTP.java

data/
└── turnos.txt

docs/
├── diagnostico.md
├── resultados_pruebas.txt
└── code_review.md
```

## Responsabilidades principales

- `App.java`: inicia el servidor y conecta las dependencias.
- `ApiController.java`: recibe y responde las solicitudes HTTP.
- `TurnoService.java`: aplica las reglas del negocio.
- `Turno.java`: representa un turno y calcula si se superpone con otro.
- `ArchivoTurnoRepository.java`: lee y guarda los turnos.
- `ReservaRequest.java`: agrupa los datos de una solicitud.
- `Servicio.java`: define los servicios y sus duraciones.
- `EstadoTurno.java`: define los estados posibles de un turno.

## Compilación

Desde la carpeta principal del proyecto ejecutamos:

```powershell
mkdir out -Force
javac -encoding UTF-8 -d out src/*.java
```

## Ejecución del servidor

```powershell
java -cp out App
```

La interfaz web se encuentra en la carpeta `web`.

## Ejecución de las pruebas

Compilamos el código junto con el contrato HTTP:

```powershell
javac -encoding UTF-8 -d out src/*.java tests/ContratoHTTP.java
```

Ejecutamos las comprobaciones:

```powershell
java -cp out ContratoHTTP
```

El resultado esperado es:

```text
RESULTADO: 26/26 comprobaciones aprobadas.
```

Las mismas 26 comprobaciones se ejecutaron sobre el piloto y sobre la versión corregida.

## Refactorizaciones realizadas

1. **Extract Method:** centralizamos la validación de disponibilidad.
2. **Move Method:** trasladamos el cálculo de superposición a `Turno`.
3. **Introduce Parameter Object:** agrupamos los datos de la reserva en `ReservaRequest`.
4. **Extract Class:** separamos la persistencia en `ArchivoTurnoRepository`.

## Problemas detectados en el piloto

Durante el análisis identificamos ocho problemas de mantenibilidad:

1. Clase con varias responsabilidades.
2. Método extenso.
3. Código duplicado.
4. Datos sin tipos propios.
5. Valores repetidos.
6. Datos de reserva dispersos.
7. Acceso al archivo acoplado.
8. Errores de lectura silenciados.

## Pruebas realizadas

Comprobamos:

- Interfaz y disponibilidad inicial.
- Datos de entrada inválidos.
- Reservas de consultas y tratamientos.
- Rechazo de turnos superpuestos.
- Aceptación de turnos consecutivos.
- Reprogramación válida y con conflicto.
- Cancelación de turnos.
- Persistencia después de reiniciar.
- Conservación del identificador siguiente.
- Detección de archivos dañados.

## Revisión de código

La revisión del equipo se encuentra en:

```text
docs/code_review.md
```

En ese archivo registramos los controles realizados, los cambios revisados, los responsables y los commits relacionados.

## Git

Las etapas principales del proyecto fueron:

- `estado-inicial`
- `etapa-diagnostico`
- `etapa-refactoring`
- `estadorefactorizado`
- `etapa-pruebas`
- `etapa-docs`

## Resultado final

La versión corregida conserva las operaciones del piloto y aprobó las mismas 26 comprobaciones del contrato HTTP.

Las refactorizaciones permitieron reducir la duplicación, separar responsabilidades y facilitar el mantenimiento futuro del sistema.
