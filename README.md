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
