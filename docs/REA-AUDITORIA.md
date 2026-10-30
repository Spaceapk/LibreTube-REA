# LibreTube REA 1.0.0 — auditoría e implementación

Base oficial: https://github.com/libre-tube/LibreTube
Commit base: `9380ab045535ba0aba38a7f07eb09024459a7b47`, rama master, versión declarada 32.1.
Fork: LibreTube REA, paquete release `org.nexlibre.libretuberea`, debug con sufijo `.debug`.
Android mínimo 8.0 (API 26), objetivo/compilación API 36. Se conserva namespace interno para evitar cambios innecesarios.
La licencia original y las atribuciones se mantienen. Las modificaciones REA se distribuyen bajo GPL-3.0-o-posterior.

## Arquitectura observada

- `app`: aplicación Android Kotlin con vistas XML, ViewModels, Room, Paging y Media3.
- `baselineprofile`: pruebas y generación de perfiles del proyecto original.
- `MediaServiceRepository.instance` usa actualmente `NewPipeMediaServiceRepository`, que extrae información de YouTube mediante NewPipeExtractor. No es una API oficial de YouTube.
- `UserDataRepositoryHelper` elige datos locales o sincronización Piped/LibreTube Sync. No se cambia esta selección.
- El feed local persiste en Room. Su refresco automático conserva el comportamiento de upstream, incluyendo la ventana de un día y la antigüedad máxima original.
- `HomeViewModel.tryLoadFeed`: punto de integración después de los filtros originales.
- `SearchPagingSource`: integración después de recibir cada página, conservando los tokens de paginación originales.
- Reproductor, descargas, listas y controles no se modifican. Conservar el código no equivale a haber validado todas sus funciones en un dispositivo.

## Datos reales y límites

Los modelos ofrecen identificador, título, canal, fecha en milisegundos, duración, vistas y descripción. Algunos campos pueden faltar o ser negativos. REA no inventa fechas, vistas ni videos.

La fuente ofrece videos relacionados al consultar los detalles de un video. REA usa hasta 40 candidatos de un video del feed suscrito para descubrimiento opcional. Esto es una relación proporcionada por la fuente; no es una medida universal de similitud entre canales ni un modelo semántico entrenado.

La búsqueda se reclasifica por página recibida. No realiza búsquedas exhaustivas ni promete una ordenación global sobre resultados todavía no descargados. Un filtro estricto puede dejar páginas vacías; el siguiente token original se conserva. Los filtros por fecha excluyen metadatos desconocidos. Canales/listas no se muestran al activar filtros de video. Se eliminan duplicados de videos durante una instancia de paginación.

## Ranking local

Puntuación: suscripción × peso + actualidad exponencial (14 días) × peso + coincidencia de intereses × peso + afinidad con canales del historial consentido + coincidencia con palabras del título de la consulta × 8 + popularidad logarítmica limitada a 0,3.

Los intereses usan coincidencia de palabras normalizadas, sin tildes. No hay embeddings ni IA generativa. Pesos de suscripciones, actualidad e intereses configurables entre 0 y 10. La diversidad intercala canales dentro de una ventana de tres posiciones cuando hay alternativas. Los videos antiguos siguen siendo candidatos. Se procesan como máximo 2.000 candidatos y Inicio devuelve como máximo 1.500 para limitar consumo. La clasificación no excluye automáticamente los videos vistos; respeta el filtro original de ocultar vistos en Inicio.

Los descartes se guardan como IDs separados por comas en Ajustes. Es un control básico, sin rediseñar menús de videos.

## Privacidad y errores

El historial para personalización está desactivado inicialmente y usa como máximo 100 entradas de la base local al activarlo. Desactivar el consentimiento deja de leerlo; no se crea una copia de perfil. Intereses y ajustes se guardan en SharedPreferences.

No se añaden SDK de analítica, rastreadores ni servidor de perfilado. El descubrimiento está desactivado inicialmente porque añade una consulta de metadatos a la fuente existente. Su caché en memoria tiene un TTL de 15 minutos y ocho semillas como máximo; ante fallo puede reutilizar una entrada anterior. El feed conserva su caché Room original. La cancelación de corrutinas se propaga.

## Verificación

Consultar los registros de la entrega para los comandos realmente ejecutados. Las comprobaciones JVM del motor no sustituyen las pruebas de interfaz, proveedores reales, memoria Android o reproducción en dispositivo. No declarar estas últimas superadas sin ejecutarlas.
