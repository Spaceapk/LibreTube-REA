# LibreTube REA 1.0.0

Fork independiente de LibreTube 32.1. Conserva la interfaz y añade clasificación local en el feed de Inicio y en las páginas de Búsqueda.

## Uso

En Ajustes → General → LibreTube REA se puede desactivar la personalización, activar el uso del historial local o el descubrimiento relacionado, escribir intereses y ajustar los pesos. Historial y descubrimiento comienzan desactivados. Los filtros de suscripciones, fecha y duración se aplican a los videos recibidos de la búsqueda; no son una búsqueda exhaustiva de YouTube.

Para ver recomendaciones de tus suscripciones, suscríbete a los canales dentro del fork o importa tus suscripciones con las funciones originales. Esta instalación usa sus propios datos y no puede leer automáticamente la base privada de la aplicación oficial. Inicio mantiene las secciones y la configuración originales.

No se añaden cuentas, rastreadores ni servidores de perfilado. El motor usa reglas locales y coincidencia de palabras, no un modelo de IA entrenado. Ver [auditoría](docs/REA-AUDITORIA.md) para alcance y limitaciones.

## Compilación y pruebas

Requiere un JDK completo (verificado con JDK 21), SDK Android 36 y las dependencias de Gradle/JitPack del proyecto original.

```
./gradlew assembleDebug testDebugUnitTest
scripts/test-local-ranker.sh
```

Paquete de prueba: `org.nexlibre.libretuberea.debug`; release: `org.nexlibre.libretuberea`.
No mezclar claves de la aplicación oficial y del fork. `scripts/sign-apk.sh` firma un APK con la clave propia guardada fuera del repositorio.

## Licencia y autoría

LibreTube REA deriva del trabajo de los autores de [LibreTube](https://github.com/libre-tube/LibreTube). Se conservan README, LICENSE, atribuciones y código originales. Modificaciones REA: GPL-3.0-o-posterior. Este fork no es una versión oficial ni está avalado por los desarrolladores originales.
