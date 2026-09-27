# Solfeggio Frequencies

A static page that plays pure sine tones at the eight Solfeggio frequencies (174–852 Hz) with the Web Audio API. There's no build step, no backend and no external assets.

The labels are traditional associations. They are not scientifically established.

## Run

```sh
docker compose up -d --build
```

Open http://localhost:8080. The health check is at `/healthz`.

## Use

- Click a card to play its tone and click it again to stop. Only one tone plays at a time.
- Keys `1`–`8` toggle a tone and `Space` stops it.
- Volume starts at 20%. Start quietly on headphones.
- The session timer (off, 5, 10 or 20 min) fades the tone out when it ends.

## Check

```sh
node --check app.js && node scripts/check-frequencies.mjs
```

## Android app

`android/` is a native Kotlin and Jetpack Compose app with the same tones and labels. Tones keep playing with the screen off, and the notification has a Stop action. Unplugging headphones stops the tone.

```sh
cd android && ./gradlew assembleRelease
```

Without signing variables the release build uses the debug key. For a signed build set `SOLFEGGIO_KEYSTORE`, `SOLFEGGIO_KEYSTORE_PASSWORD`, `SOLFEGGIO_KEY_ALIAS` and `SOLFEGGIO_KEY_PASSWORD`.

## Publishing

Every push to `main` runs `.github/workflows/publish.yml`. It checks the web app, builds the signed APK and publishes the page plus `solfeggio.apk` to the `gh-pages` branch, which GitHub Pages serves. The signing key comes from the `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` repository secrets.
