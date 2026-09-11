# KApp · Mobile (Kotlin / Android)

Native Android client, Kotlin and Jetpack Compose. This is the product, not a prototype — see
`AGENTS.md`.

## State

The **login screen** is built, as an interface only. It matches
[`docs/design/mobile/LoginAndroid.dc.html`](../../../../docs/design/mobile/LoginAndroid.dc.html)
and goes nowhere: there is no network layer, no `INTERNET` permission and no call to
`POST /auth/login`. Pressing **Ingresar** with both fields filled navigates to a placeholder Inicio.

Everything else is a stub. `HomeScreen` and `InvitationScreen` exist so the two exits of the login
have somewhere to land and the back stack can be checked.

## Running it

Requires JDK 17 or newer and the Android SDK with platform 36. Open the `kotlin/` folder in Android
Studio, or from the command line:

```bash
cd app/frontend/mobile/kotlin

./gradlew :app:assembleDebug        # build
./gradlew :app:testDebugUnitTest    # unit tests
./gradlew :app:installDebug         # onto a connected device or a running emulator
```

The `@Preview` in `LoginScreen.kt` renders the screen at 360x800, which is the size the mockup is
drawn at, so the two can be compared side by side without a device.

## Layout

```
app/src/main/java/co/edu/konradlorenz/kapp/
├── MainActivity.kt              edge-to-edge, hosts the NavHost
└── ui/
    ├── theme/                   the palette, the type scale, the Material scheme
    ├── navigation/              three routes, no arguments
    ├── login/                   LoginScreen + LoginViewModel
    ├── home/                    stub
    └── invitation/              stub
```

## Colour

Every colour comes from [`docs/K-COLORS.md`](../../../../docs/K-COLORS.md) through
[`Tokens.dc.html`](../../../../docs/design/mobile/Tokens.dc.html), and `ui/theme/Color.kt` uses the
names that sheet assigns. Two rules worth not rediscovering:

- **Pink `#D51A65` means "you can touch this".** Buttons, links, the active tab. Nothing else.
- **Never white on the green `#C9D329`** — 1.5:1, it disappears in sunlight. Purple goes on green.

## What is deliberately missing

| Missing | Why |
|---|---|
| Retrofit and `POST /auth/login` | Next task. Against the Prism mock on `10.0.2.2:4010` (`docker compose --profile mock up -d`) before the real gateway |
| The four states in `EstadosLogin.dc.html` | Sending, 401, 403 unverified and offline. All four are answers the server gives; there is nothing to render them from yet |
| `POST /auth/verify/resend` | Reached only from the 403 state above |
| Session persistence | "Mantener la sesión iniciada" holds interface state only. `auth.openapi.yaml` has no refresh token and `expiresIn` is one hour for everybody, so there is a backend decision to make first |
| Hilt | It earns its place when there are two implementations to swap, not before |
| A monochrome launcher icon | Themed icons need a single-colour version of the crest, which is a design asset we do not have |

## Versions

Pinned in `gradle/libs.versions.toml`. Android Studio will offer newer ones — take them through the
AGP Upgrade Assistant rather than by hand, so Gradle and the Kotlin compiler move together.
