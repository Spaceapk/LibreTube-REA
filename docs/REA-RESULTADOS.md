# Resultados ejecutados — LibreTube REA 1.0.0

Fecha: 29 de octubre de 2026 (America/Managua).

| Comprobación | Resultado |
| --- | --- |
| Base oficial, assembleDebug | Correcta tras instalar JDK completo y regenerar un recurso de empaquetado incompleto |
| Fork, assembleDebug + testDebugUnitTest | BUILD SUCCESSFUL, 59 tareas, 3 min 20 s |
| Suite JVM normal | 14 pruebas superadas; una prueba de red omitida por requerir activación explícita |
| Motor de ranking | 19 comprobaciones dentro de una prueba JUnit y ejecución independiente con javac/java |
| Caché / proveedor inyectado | 4 pruebas: reutilización y recuperación ante errores, límites y expulsión, timeout y cancelación |
| Proveedor real de búsqueda | Prueba activada por separado: 1 ejecutada y superada, 8,56 segundos; consulta de prueba sin datos personales |
| Firma del APK de entrega | Verificada con apksigner; esquemas v2 y v3; RSA 4096; un firmante propio del fork |
| Alineación y estructura ZIP | zipalign correcto y testzip sin entradas corruptas |
| Instalación, interfaz y reproducción Android | No ejecutadas: no había dispositivo conectado; no se afirma que se hayan superado |
| Persistencia tras reiniciar Android | No ejecutada en dispositivo; implementación mediante SharedPreferences |
| Memoria y rendimiento Android | No medidos; la medición JVM del motor no es un benchmark Android |
| Descubrimiento relacionado completo en Android | No ejecutado; verificados el código de integración y la caché con proveedores inyectados |
| Publicación del código en GitHub | Fork creado; carga de la rama bloqueada por HTTP 403 del conector y falta de credenciales git en el entorno |

La búsqueda real se verificó mediante NewPipeExtractor y el downloader original, sobre metadatos reales. No es una prueba de interfaz ni de todas las funciones del repositorio Android.

El APK entregado es una compilación de prueba (debug), firmada con la clave propia del fork, paquete `org.nexlibre.libretuberea.debug`, versión 1.0.0 (1), Android 8.0 o posterior. No sustituye ni actualiza LibreTube oficial.

Certificado SHA-256:
`ca79c14d6986c0c759581f8aa1f45d65f1a0fb966978070c8b390a75f6f4b68e`

APK SHA-256:
`2d83b40dbfc35f1c85ce44e682cb63fa46cf7a2d60a895995748f7ea78ceed8b`

## Limitaciones funcionales

La personalización actúa en el feed de suscripciones de Inicio y sobre cada página de videos de Búsqueda. Las demás secciones de Inicio mantienen el comportamiento original. Los filtros trabajan sobre candidatos descargados, no sobre todo YouTube. El descubrimiento opcional usa relacionados de una sola semilla por carga y no implementa similitud semántica global entre canales. Los intereses usan coincidencia de palabras. El descarte se introduce por IDs en Ajustes. El historial requiere consentimiento y solo lee la base local. Los ajustes se aplican al volver a cargar el feed o al iniciar una nueva búsqueda.

La clave está fuera del repositorio y se conserva en un respaldo privado. No publicar ese respaldo. No se ha modificado ni fusionado la rama principal de upstream o del fork.
