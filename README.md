# MSJ Sticker Studio

**Video → WhatsApp sticker · Music sticker · Ringtone — created by M.S.J**

Turn any video into:

- **Animated WebP stickers** — registered straight into WhatsApp's sticker keyboard (pack + tray icon + emojis)
- **GIF stickers** — classic looping GIF, also registered as a WhatsApp sticker pack
- **Music stickers** — WebM (VP9 video + Opus audio) that keeps the clip's sound, ready to share
- **MP3 ringtones** — soundtrack extracted from any video at 192 kbps

Dark nebula UI with animated gradient background, glass cards, and full control over size / FPS / quality / emojis / pack name.

## Tech stack

| Piece | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Video engine | FFmpeg (ffmpeg-kit, audio build) |
| WhatsApp integration | Official sticker content-provider API (`com.whatsapp.stickerapp.provider`) |
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

1. `FfmpegUtils` converts the picked video to a 512px animated WebP (`palettegen` + `paletteuse`, Bayer dithering, loop flag) or GIF.
2. A 96×96 WebP tray icon is rendered from the first frame.
3. `WhatsAppHandler` registers the pack via the WhatsApp content provider and inserts every sticker with its emoji list.
4. `StickerContentProvider` serves the WebP data back to WhatsApp over `content://`.
5. Music stickers go through VP9+Opus encoding instead, then out via share intents.

## Notes

- Sticker packs accept 1–30 stickers; each image asset stays small and fast thanks to the palette pipeline.
- Min SDK 26 (Android 8.0+), target SDK 34.
- Creator credit: **M.S.J** — design, code, and FFmpeg pipeline.

---
*MSJ Sticker Studio v1.0.0 — made with cold coffee.*
