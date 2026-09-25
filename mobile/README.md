# mobile/ — Client Kotlin Multiplatform (Android + iOS)

App mòbil de CineCat: catàleg, fitxa i valoració (anònima o amb àlies). Consumeix **exactament la mateixa API** que el web (`docs/ESPECIFICACIO.md` §4). No té res d'admin (§8).

## Què es comparteix i què és natiu

Tot el codi viu a `shared/`; les apps `androidApp/` i `iosApp/` només l'arrenquen.

| Compartit (`shared/src/commonMain`) | Natiu (`androidMain` / `iosMain` i les apps) |
|---|---|
| Models `@Serializable` que calquen el JSON de l'API | **Motor HTTP**: OkHttp a Android, Darwin (NSURLSession) a iOS |
| Client de l'API (`CineCatApi`, Ktor) i errors `{error}` | **URL de l'API en local**: `10.0.2.2` a l'emulador, `localhost` al simulador |
| Sessió (`SessionManager`) i caducitat del token | **Emmagatzematge segur del token**: Android Keystore / iOS Keychain |
| ViewModels (estat de cada pantalla) | **Xarxa en clar només en debug**: `network_security_config` / `NSAllowsLocalNetworking` |
| **UI sencera** amb Compose Multiplatform | Punts d'entrada: `MainActivity` / `MainViewController` + SwiftUI |
| Format català (`7,8`, `1.243`, `1h 52min`) | Icona i configuració de cada app |

La frontera es declara amb `expect`/`actual`: vegeu `shared/src/commonMain/.../data/Platform.kt`.

## Requisits

- **JDK 17 o superior.** El que porta Android Studio serveix:
  ```bash
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  ```
- **Android:** l'Android SDK (`ANDROID_HOME`, normalment `~/Library/Android/sdk`) i un emulador.
- **iOS:** Xcode amb el simulador d'iOS.
- El **backend corrent a `:8080`** amb dades (vegeu el [README arrel](../README.md)).

## Arrencar

### Android (emulador)

```bash
cd mobile
./gradlew :androidApp:installDebug
```

L'app es connecta a `http://10.0.2.2:8080`, que és el teu ordinador vist des de l'emulador.

### iOS (simulador)

Obre `iosApp/iosApp.xcodeproj` amb Xcode i prem ▶︎, o bé des del terminal:

```bash
cd mobile
xcodebuild -project iosApp/iosApp.xcodeproj -target iosApp -sdk iphonesimulator -arch arm64 SYMROOT=$PWD/iosApp/build
xcrun simctl install booted iosApp/build/Debug-iphonesimulator/CineCat.app
xcrun simctl launch booted cat.cinecat.app
```

La compilació d'Xcode crida Gradle (`:shared:embedAndSignAppleFrameworkForXcode`) per generar el framework `Shared`. L'app es connecta a `http://localhost:8080`.

### Tests

```bash
./gradlew :shared:allTests   # JVM (Android) i simulador d'iOS
```

## Estructura

```
shared/src/
├── commonMain/kotlin/cat/cinecat/
│   ├── App.kt              ← navegació + barra de pestanyes (Catàleg / Cerca / Perfil)
│   ├── data/               ← models, CineCatApi, SessionManager, TokenStorage (interfície), expect
│   ├── ui/                 ← tema, components (MovieCard, RatingSelector...) i pantalles
│   └── util/Format.kt      ← format català
├── commonMain/composeResources/  ← fonts Geist i Geist Mono (OFL, vegeu files/Geist-OFL.txt)
├── androidMain/            ← OkHttp, 10.0.2.2, KeystoreTokenStorage
└── iosMain/                ← Darwin, localhost, KeychainTokenStorage, MainViewController
```

Versions de les eines: `gradle/libs.versions.toml`. És la combinació que manté compatible la plantilla oficial [Kotlin/KMP-App-Template](https://github.com/Kotlin/KMP-App-Template), d'on també prové l'esquelet del projecte Xcode.
