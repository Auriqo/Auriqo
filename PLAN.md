# Auriqo — Plan de desarrollo y estado

## Estado del repositorio

- Rama de integración: `main`
- Licencia: GPL-3.0
- Distribución: el repositorio continúa privado; no se asume una URL pública, descarga, soporte público ni publicación externa.
- Candidato preparado: `versionName 1.0.0`, `versionCode 527`, paquete `com.auriqo.music`.

La información sensible, tokens locales, rutas personales, material de firma y datos de cuentas no se documentan en este archivo ni se versionan.

## Fases completadas

### Fase 0 — Rebrand ✅

- La identidad, namespace y application ID son `com.auriqo.music`.
- Se reemplazó la marca heredada en recursos, pantallas, strings, paquetes y documentación activa.
- Se eliminaron referencias activas a financiación, canales y credenciales personales heredadas.

### Fase 1 — Iconos y recursos visuales ✅

- Se actualizaron los recursos de launcher, notificación, splash y temas de Auriqo.
- Se preparó un feature graphic versionado como material candidato de distribución; no se publicó en una tienda.

### Fase 2 — Limpieza de features heredadas ✅

- Se retiraron los flujos de financiación, uptime, contribución lossless y canvas obsoleto, junto con sus referencias de navegación/settings.
- El módulo `:auriqocanvas` ya no participa en `settings.gradle.kts` ni en las dependencias de la app.

### Fase 3 — Settings y límites de integraciones ✅

- Se ordenaron las pantallas de configuración después de la limpieza de features.
- Las integraciones opcionales mantienen límites explícitos entre preferencias, datos locales y solicitudes a terceros.
- Listen Together quedó configurable por despliegue, sin endpoint de origen por defecto.

### Fase 4 — Strings y locales ✅

- Se actualizaron strings y recursos de Auriqo en los locales versionados.
- Las nuevas cadenas de privacidad, licencias y estados de producto se integraron en los recursos de la aplicación.

### Fase 5 — UI y tema propio ✅

- Se aplicaron recursos visuales, tema, launcher, pantallas de bienvenida y ajustes de interfaz de Auriqo.
- Los cambios de player, widgets y navegación se acompañan de políticas o tests focalizados donde corresponde.

### Fase 6 — Features existentes ✅

- Letras: fallback determinista y caché acotada, cubiertos por tests unitarios.
- Descargas: política explícita para estados completado, fallido, detenido y removido.
- Reconocimiento: mapeo consistente para permiso denegado, sin coincidencia y fallos del proveedor.
- Listen Together: los valores de servidor e invitación son opcionales, vacíos por defecto y validados como `wss://`/`https://`.
- Integraciones y widgets: se añadieron tests focalizados para límites de Discord/Last.fm/Spotify y política de layout de widgets.

### Fase 7 — Build y dependencias ✅

- Se conserva la matriz FOSS/GMS y ABI; CI valida Universal FOSS y Universal GMS sin secretos.
- El build usa JDK 21, compile/target SDK 36 y min SDK 26.
- La firma de release es explícita: `checkReleaseSigning` comprueba keystore y los tres valores de entorno sin versionarlos.
- Las credenciales opcionales se leen con valores vacíos seguros y se escapan antes de entrar en BuildConfig.
- Se retiraron los captures huérfanos y la dummy key de Flow Neuro versionada; no se reemplazaron por secretos embebidos.

### Fase 8 — CI/CD ✅

- CI ejecuta build/test/lint Universal FOSS y Universal GMS sin secretos en `main`, pull requests, tags y dispatch; además ejecuta el smoke Compose de superficies de producción FOSS en el Gradle Managed Device Pixel 2/API 30/`aosp-atd`, con límite de 30 minutos y reportes de instrumentación siempre cargados.
- El release preparado es un único APK FOSS universal; se limita a 200 MiB y se registra en `SHA256SUMS` antes de una publicación autorizada.
- CodeQL apunta a Actions y Java/Kotlin sólo cuando GitHub admite code scanning: repositorios públicos o privados con Advanced Security habilitado. Mientras este repositorio privado no tenga esa capacidad, no se declara un análisis ejecutado.
- La publicación sólo puede seguir un tag `v*` o dispatch explícito, y exige inputs de firma, tag, notas coincidentes y ausencia de marcadores `PENDING:`.
- La sincronización de `player_configs.json` valida JSON y abre un pull request de revisión; no escribe directamente en `main`.

### Fase 9 — Testing ✅

- Hay tests unitarios para letras, descargas, reconocimiento, Listen Together, consentimiento, OSS, integraciones y widgets. El historial de esquemas Room de Auriqo v1–44 se valida estáticamente; no se declara un test de migración ejecutado.
- Existe cobertura smoke Compose instrumentada sobre superficies reales de `MainActivity` (Home, Search y Settings) y el componente de transporte de producción. CI la ejecuta mediante `:app:auriqoApi30UniversalFossDebugAndroidTest`; una corrida registrada del GMD Pixel 2/API 30 el 2026-08-05 aprobó los 4 tests. Esa evidencia es sólo smoke: la validación candidata del APK universal restante pasa a GitHub.
- El comando de CI incluye `:app:assembleUniversalFossDebug`, `:app:testUniversalFossDebugUnitTest`, `:app:lintUniversalFossDebug`, `:app:assembleUniversalGmsDebug`, `:app:testUniversalGmsDebugUnitTest`, `:app:lintUniversalGmsDebug` y `:app:auriqoApi30UniversalFossDebugAndroidTest`.

### Fase 10 — Documentación ✅

- `README.md`, `SETUP.md`, `BUILD.md`, `ARCHITECTURE.md`, `CONTRIBUTING.md` y la documentación de release describen la arquitectura y el proceso actual.
- No se afirma disponibilidad pública mientras el repositorio sea privado.

### Fase 11 — Preparación de distribución ✅

- Se prepararon metadata F-Droid y listing Fastlane/Play para `com.auriqo.music`, versión 1.0.0/527.
- La receta F-Droid está deshabilitada y la documentación de Play no declara envío, aprobación ni disponibilidad.

### Fase 12 — Privacidad, consentimiento y OSS ✅

- FOSS no contiene Firebase ni presenta control de telemetría.
- En GMS, Analytics y Crashlytics parten desactivados; sólo se activan tras aceptación explícita registrada y pueden cambiarse en Privacy settings.
- La pantalla de licencias usa `oss-licenses.tsv` local y versionado; no descarga atribuciones en runtime.
- `PRIVACY_POLICY.md` y `SECURITY.md` describen hechos de código/configuración y no prometen cumplimiento legal.

### Fase 13 — Performance ✅

- Se agregaron marcadores de cold start para Perfetto/Logcat y la creación del image loader evita bloquear el hilo con una lectura de DataStore.
- La caché de Coil en memoria/disco, la caché de resultados de letras y las solicitudes de artwork de widgets tienen límites/políticas de caché explícitos; también se reforzaron cachés y estados de reproducción/descargas donde correspondía.

## Fase 14 — Release v1.0.0

### Preparado ✅

- `app/build.gradle.kts` declara 1.0.0/527.
- `CHANGELOG.md`, `RELEASE_INFO.md`, la guardia de CI y la metadata F-Droid/Play fueron preparadas para el candidato.
- Las notas de release usan marcadores `PENDING:` y la CI rechaza publicación mientras existan.

### Pendiente: autoridad y acciones externas ⏳

- Ejecutar y registrar en GitHub la validación candidata Universal FOSS + Universal GMS desde la revisión autorizada; conservar el resultado GMD 4/4 como evidencia smoke y no como evidencia de release.
- El candidato universal FOSS sigue pendiente de validación en GitHub; el APK universal firmado sigue pendiente de material de firma aprobado.
- Proporcionar material de firma aprobado, generar el APK FOSS universal y registrar la huella del firmante y `SHA256SUMS`.
- Autorizar y crear el tag inmutable `v1.0.0` y, recién entonces, una GitHub Release.
- Habilitar fuente pública/revisable y prerrequisitos de reproducibilidad antes de enviar F-Droid.
- Completar la ficha Data safety, URL pública de privacidad/contacto y autorización de propietario antes de enviar Play.
- No se creó APK firmado, tag, GitHub release, publicación F-Droid/Play, dominio ni anuncio como parte de este trabajo.

## Convenciones vigentes

- Leer completamente cada archivo antes de modificarlo y evitar reemplazos masivos a ciegas.
- Mantener separados los source sets FOSS/GMS cuando una conducta es específica de variante.
- No versionar `local.properties`, `google-services.json`, keystores, tokens, archivos generados ni datos privados.
- Preservar la condición privada del repositorio hasta que un mantenedor autorice explícitamente otra cosa.
