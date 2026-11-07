# Gestor de turnos piloto

Prototipo para un consultorio de podología. Permite reservar, consultar, cancelar y reprogramar turnos. Las consultas duran 30 minutos y los tratamientos, 60. La atención es de 09:00 a 18:00.

## Compilar en Windows

Con un JDK 17 o superior, abrí PowerShell en la carpeta del proyecto y ejecutá:

```powershell
mkdir out -Force
javac -encoding UTF-8 -d out src/App.java tests/ContratoHTTP.java
```

## Iniciar el piloto

```powershell
java -cp out App
```

Abrí http://127.0.0.1:8080 en el navegador. La primera reserva crea data/turnos.txt. Usá datos ficticios. Para detener el servidor, presioná Ctrl+C.

## Ejecutar las pruebas

Después de compilar, desde la misma carpeta:

```powershell
java -cp out ContratoHTTP
```

Las 26 comprobaciones usan un archivo temporal para las reservas.