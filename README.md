# MSJ Sticker Studio

**Video → WhatsApp sticker · Music sticker · Ringtone — created by M.S.J**

Turn any video into:

- **Animated WebP stickers** — registered into WhatsApp's sticker keyboard via the official `ENABLE_STICKER_PACK` flow (pack + tray icon + emojis)
- **GIF stickers** — classic looping GIF for sharing, plus an animated WebP twin registered in WhatsApp
- **Music stickers** — WebM (VP8 video + Vorbis audio) that keeps the clip's sound and plays it, ready to share
- **M4A ringtones** — AAC soundtrack extracted from any video at 192 kbps

Dark nebula UI with animated gradient background, glass cards, and full control over size / FPS / quality / emojis / pack name.

## Tech stack

| Piece | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Video engine | FFmpegKit maintained fork (`dev.ffmpegkit-maintained:ffmpeg-kit-video:6.0.4` — 16KB page support for Android 15+) |
| WhatsApp integration | Official third-party sticker API (`com.whatsapp.intent.action.ENABLE_STICKER_PACK` + self-hosted ContentProvider) |
| Build | Gradle 8.2, AGP 8.2.2, GitHub Actions |

## Build the APK on GitHub (no local Android SDK needed)

1. Push this repo to GitHub (or fork it).
2. Open the **Actions** tab → run the **"Build APK"** workflow (it also runs automatically on every push).
3. When the run finishes, scroll to **Artifacts** and download:
   - `MSJ-Sticker-Studio-debug.apk` — installable right away (debug-signed)
   - `MSJ-Sticker-Studio-release.apk` — signed with your keystore if you add the secrets, otherwise debug-signed fallback
4. Install the APK on your phone and allow "install unknown apps".

### Signing the release APK (optional, for Play Store)

Add these repository secrets under **Settings → Secrets and variables → Actions**:

| Secret | Value |
|---|---|
| `KEYSTORE_B64` | `base64 -w0 your-keystore.jks` |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | key alias |
| `KEY_PASSWORD` | key password |

Without the secrets the release job falls back to the debug keystore so the build always succeeds.

## Build locally (optional)

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## How the sticker pipeline works

1. `FfmpegUtils` converts the picked video to a 512px animated WebP (`palettegen` + `paletteuse`, Bayer dithering, loop flag) or GIF — auto-shrinking fps/quality/size to stay under WhatsApp's 1MB sticker limit.
2. A 96×96 WebP tray icon is rendered from the first frame.
3. `WhatsAppHandler` stages the pack behind `StickerContentProvider` and stores it in the local `PackStore` registry, then fires the official `ENABLE_STICKER_PACK` intent — WhatsApp shows its "Add pack?" dialog and pulls metadata + sticker WebPs from our provider.
4. Music stickers encode as WebM (VP8 + Vorbis) and go out via share intents; ringtones encode as M4A (AAC) with the native encoder.

## Notes

- Sticker packs accept 1–30 stickers; each image asset stays small and fast thanks to the palette pipeline.
- Min SDK 26 (Android 8.0+), target SDK 34.
- Creator credit: **M.S.J** — design, code, and FFmpeg pipeline.

---
*MSJ Sticker Studio v1.0.0 — made with cold coffee.*
