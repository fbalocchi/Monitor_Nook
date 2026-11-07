# Validación de la revisión pública 1.1.3

Se usa Madrid como ubicación de ejemplo del clima. La zona horaria del calendario
se conserva en America/Santiago. La configuración guardada por cada usuario se
sigue leyendo desde la carpeta privada de la app.

La versión pública anterior fue compilada y probada en el host, y el usuario
confirmó en su BNRV500 las funciones de la app 1.1.2. La revisión 1.1.3 cambia los
valores predeterminados de ubicación y las etiquetas de versión; no se ha probado
esta revisión en hardware durante su preparación.

Se recompiló para Java 6/API 7, DEX 035, usando la misma identidad de firma de la
versión 1.1.2. Se ejecutaron las regresiones existentes de calendario, horario de
Chile, límites de días, diagnóstico, validación TLS y WakeLock. Se verificó la
firma v1/JAR del APK para API 7, sin errores.

Se revisaron los archivos públicos y el contenido expandido del APK para confirmar
la ausencia de la ubicación predeterminada anterior, sus constantes numéricas,
claves privadas, serie del lector y correos personales conocidos. Las coordenadas
de ejemplo y la ciudad son editables desde Ajustes.

La compilación del host y sus pruebas no sustituyen una prueba de estabilidad o
autonomía en un lector. El paquete está preparado sin historial Git para publicarse en un repositorio nuevo.
