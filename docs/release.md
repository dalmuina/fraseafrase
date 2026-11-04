# Publicación de `release`

Cómo firmar localmente la variante `release` de `:app` con el keystore de producción. Ver
`openspec/changes/configure-release-signing/design.md` para el contexto completo de esta decisión.

## 1. Generar o ubicar el keystore de producción

Si todavía no existe un keystore de producción, genera uno **fuera del repositorio** (por ejemplo en
`~/keys/fraseafrase/` o en tu carpeta de backups habitual, nunca dentro de la carpeta del proyecto):

```bash
keytool -genkeypair -v \
  -keystore /ruta/fuera/del/repo/fraseafrase.jks \
  -alias fraseafrase \
  -keyalg RSA -keysize 2048 -validity 10000
```

`keytool` pedirá la contraseña del almacén, la contraseña de la clave y los datos del certificado
(nombre, organización, etc.). Guarda esas contraseñas: no hay forma de recuperarlas si se pierden, y
sin ellas no se puede volver a firmar `release` con la misma identidad.

Si el keystore ya existe (por ejemplo, generado en otra máquina o restaurado desde una copia de
seguridad), sitúalo en cualquier ruta local accesible desde esta máquina; no es necesario que esté
dentro del repositorio.

## 2. Rellenar `keystore.properties`

En la raíz del proyecto, copia la plantilla versionada y rellénala con los datos reales:

```bash
cp keystore.properties.example keystore.properties
```

Edita `keystore.properties` con:

- `storeFile`: ruta absoluta (o relativa a la raíz del proyecto) al `.jks` del paso 1.
- `storePassword`: contraseña del almacén.
- `keyAlias`: alias de la clave (`fraseafrase` en el ejemplo anterior).
- `keyPassword`: contraseña de la clave.

`keystore.properties` está en `.gitignore`: nunca se commitea. Cada máquina de desarrollo mantiene
su propia copia local apuntando a su propia ruta del keystore.

## 3. Generar el artefacto firmado

```bash
./gradlew :app:assembleRelease   # APK firmado en app/build/outputs/apk/release/
./gradlew :app:bundleRelease     # AAB firmado en app/build/outputs/bundle/release/
```

Si `keystore.properties` no existe o le falta alguna de las cuatro claves, y no hay variables de
entorno equivalentes (`RELEASE_KEYSTORE_PATH`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`,
`RELEASE_KEY_PASSWORD`) definidas, la build falla en la fase de configuración con un
`GradleException` que recuerda estos pasos, en vez de firmar `release` con la clave de depuración.

`./gradlew :app:assembleDebug` y `./gradlew check` no requieren ninguna de estas credenciales: solo
se exigen al ejecutar una tarea de la variante `release`.

## 4. Verificar la firma del artefacto

```bash
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

Comprueba que el firmante mostrado corresponde al alias configurado en `keystore.properties`
(`keyAlias`), no a la clave de depuración de Android.

## Alternativa: variables de entorno (CI)

Si no hay fichero `keystore.properties` disponible (por ejemplo, en un futuro pipeline de CI), las
mismas credenciales pueden definirse como variables de entorno antes de invocar Gradle:

```bash
export RELEASE_KEYSTORE_PATH=/ruta/al/fraseafrase.jks
export RELEASE_KEYSTORE_PASSWORD=...
export RELEASE_KEY_ALIAS=fraseafrase
export RELEASE_KEY_PASSWORD=...
./gradlew :app:assembleRelease
```

El fichero tiene prioridad sobre las variables de entorno si ambos están presentes.
