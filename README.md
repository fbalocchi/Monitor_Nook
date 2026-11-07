# Panel NOOK

Un NOOK viejo convertido en un panel de clima y agenda para la casa.

Proyecto de Francisco, desarrollado con ayuda de IA y probado
en un **NOOK GlowLight BNRV500**, con Android 2.1 (API 7) y firmware 1.3.1.
Versión pública de la app: **1.1.3**.
La versión 1.1.3 usa Madrid como ejemplo; la zona del calendario sigue siendo
America/Santiago y la ubicación del clima se cambia desde Ajustes.

## Qué hace

- Hora de Chile en formato de 24 horas.
- Clima actual y pronóstico de tres días desde Open-Meteo.
- Calendario mensual y agenda con eventos de un Google Calendar, de solo lectura.
- Detalles de eventos, notas locales y ajustes de Wi-Fi/actualización.
- Puede elegirse como pantalla de inicio y abrirse al encender el lector.

Después de instalar y configurar, funciona con el NOOK y Wi-Fi. No requiere un
PC encendido, Cloudflare Workers, Google Sheets ni un servidor propio. El clima
y el calendario se descargan directamente; no es una actualización en tiempo real.
La frecuencia predeterminada es de 30 minutos y puede cambiarse.

## Estado real

El usuario comprobó clima, sincronización de Google Calendar, navegación por la
agenda, permanencia del panel durante casi 20 minutos, selección como inicio
predeterminado y arranque directo después de reiniciar. También confirmó que el
reloj se ve en 1.1.2. La revisión pública 1.1.3 cambia la ubicación de ejemplo;
no se ha instalado todavía en un lector durante esta revisión. Esto no equivale a una prueba de autonomía ni de estabilidad
durante semanas. No se ha validado en otros modelos.

**BNRV500 no es el NOOK Simple Touch with GlowLight.** Este proyecto corresponde
al GlowLight blanco BNRV500. No se deben intercambiar imágenes de arranque entre
modelos por tener nombres parecidos.

## Instalar en un lector ya preparado

Requisito: BNRV500 con Android 2.1 y ADB USB con acceso root ya habilitado.
Este repositorio **no instala root** ni modifica el arranque del lector.

1. Descarga el repositorio completo y extráelo.
2. Instala Android Platform Tools en tu PC y conecta el lector encendido.
3. En PowerShell, consulta su número de serie:

   ```powershell
   & "C:\ruta\platform-tools\adb.exe" devices -l
   ```

4. Desde la carpeta del proyecto, ejecuta:

   ```powershell
   powershell -NoProfile -ExecutionPolicy Bypass -File .\windows\instalar-inicio.ps1 -Serial "SERIE_DE_TU_NOOK" -AdbPath "C:\ruta\platform-tools\adb.exe"
   ```

El instalador verifica el SHA256 del APK publicado, exige API 7 y ADB root,
comprueba el reloj para HTTPS, instala la app y la abre. No copia configuración
privada ni notas. Los scripts públicos requieren indicar el lector explícitamente.

5. Conecta Wi-Fi. En **Ajustes → Google Calendar**, pega la dirección secreta
   iCal de tu calendario, no el texto del archivo ICS ni su ID.
   El formato admitido es una URL HTTPS de `calendar.google.com`, con ruta
   `/calendar/ical/.../basic.ics`. El enlace completo da acceso de lectura al
   calendario: no lo publiques ni lo pegues en un issue.
6. En **Ajustes → Ubicación del clima**, cambia nombre y coordenadas si corresponde.
   El valor inicial es Madrid, España (ubicación de ejemplo).
7. Para usarlo como inicio, pulsa el botón físico `n`, elige **Panel NOOK** y
   **Usar siempre**. Desde Ajustes puedes volver al sistema/lector original.

## APK y firma

`releases/Panel-NOOK-1.1.3.apk` es la revisión pública con ubicación de ejemplo,
compilada y verificada para API 7 con firma v1/JAR. Está firmada con la misma
identidad que la versión 1.1.2 utilizada en el lector. Su SHA256 está en `releases/SHA256SUMS.txt`.
No contiene el calendario ni las notas del usuario.

La clave privada de esa instalación no está en este repositorio. Si compilas
con tu propia clave, Android no permitirá actualizar encima del APK publicado
con `install -r`. Usa otro identificador de paquete o respalda tus datos antes de
reemplazar una instalación con firma distinta. Conserva tu propia clave para
firmar futuras actualizaciones.

## Compilar desde el código

En Linux x86-64, con Java 17+, Python 3 y OpenSSL:

```bash
python3 download_deps.py
python3 tools/create_signing_key.py
python3 build_official.py
python3 tools/check.py
```

El APK resultante queda en `official/Panel-NOOK.apk`. Las dependencias se descargan
desde las URLs de `dependencies.json` y se verifican mediante SHA256. La clave de
desarrollo se genera localmente en `signing/`, que está excluido de Git.

La compilación usa ECJ con destino Java 6, framework API 7, dx 1.7, DEX 035,
AAPT2 y Android apksig con firma v1. No necesita Gradle ni un Android SDK instalado.
La interfaz usa Canvas y vistas nativas, no una página web moderna.

Los scripts de Windows están fijados al APK de `releases/`. Para probar tu propia
compilación en un dispositivo con firma compatible:

```bash
adb -s SERIE_DE_TU_NOOK push -Z official/Panel-NOOK.apk /data/local/tmp/panel-nook.apk
adb -s SERIE_DE_TU_NOOK shell pm install -r /data/local/tmp/panel-nook.apk
adb -s SERIE_DE_TU_NOOK shell am start -n cl.balocchi.nookpanel/.MainActivity
```

## Datos y límites

- Un calendario Google de solo lectura; no crea ni modifica eventos.
- Enlace privado, caché y notas se guardan en el directorio privado de la app.
  Un dispositivo con root sigue permitiendo a quien tenga ese acceso leerlos.
- HTTPS con validación de certificados y nombre del servidor; no se desactiva TLS.
  Se incluyen raíces públicas de Mozilla. No se comprueba revocación en línea.
- Zona horaria `America/Santiago`; reglas de respaldo incluidas hasta 2050.
  Cambios futuros en las reglas o certificados pueden requerir actualizar la app.
- Feed iCal de hasta 4 MiB. Repeticiones y excepciones normales; no hay soporte
  específico para `RANGE=THISANDFUTURE`. La agenda mensual muestra hasta 200 entradas.
- Notas de hasta 8.000 caracteres, locales al lector.
- No hay medición de autonomía. Mantener pantalla activa y Wi-Fi consume batería.

## Créditos

La idea se nutre de proyectos de reutilización de lectores y del trabajo de la
comunidad NOOK. Referencias para explorar; no son instrucciones intercambiables:

- [Nook Weather Display](https://github.com/mrfunkyland/Nook-Weather-Display)
- [TRMNL NOOK Simple Touch](https://github.com/usetrmnl/trmnl-nook-simple-touch): referencia de integración TLS para Android antiguo.
- [NG Mods para BNRV500](https://xdaforums.com/t/app-ng-mods-and-custom-apps-for-the-nook-glowlight-bnrv500.4763333/)
- [Full root for NOOK GlowLight](https://xdaforums.com/t/full-root-for-nook-glowlight.2853056/)
- Open-Meteo, Google Calendar, ical4j, SpongyCastle, Mozilla, IANA y AOSP.

Código propio bajo licencia MIT. Las bibliotecas, certificados y fragmentos de
terceros conservan sus licencias, incluidas en `THIRD-PARTY.txt`.
