# Repeatless — Brand Guidelines

The single source of truth for how Repeatless looks, sounds, and behaves —
across the Android app, web demo, store listing, landing page, and social.

---

## 1. Identity

- **Name:** Repeatless (one word, capital R; never "RepeatLess" or "Repeat Less")
- **Tagline:** *Say it once. Tap it forever.*
- **Marketing hook:** *Stop sounding like a broken record.*
- **One-liner:** The app that answers your kids in your own voice — automatically —
  without your recordings ever leaving your phone.
- **Domain:** repeatless.app · **Android ID:** `app.repeatless`

## 2. Logo & mark

The mark is a **coral soundboard pad with a bold "1" followed by sound waves** —
"say it once, and it plays." Assets:

- Launcher (adaptive): `android/app/src/main/res/drawable/ic_launcher_foreground.xml`
  on ground `#1A1620`; themed/monochrome variant `ic_launcher_monochrome.xml`.
- Use the glyph ("1)))") alone as favicon/avatar; use pad + glyph elsewhere.
- Clear space: at least the width of the "1" stem on all sides. Don't rotate,
  outline, gradient-fill, or recolour the mark outside the palette.

## 3. Colour

Signature theme is **Console Dark** — a warm, tactile "drum-machine" feel.
Console Light is a warm-paper companion (never a naive inversion).

### Core (Console Dark)
| Token | Hex | Role |
|---|---|---|
| Ground | `#1A1620` | app background |
| Panel | `#241E2B` | bars, sheets, cards |
| Panel-2 | `#2E2735` | inputs, raised rows |
| Line | `#3A3242` | borders, dividers |
| Cream | `#F4ECE2` | primary text |
| Muted | `#A495AD` | secondary text |
| **Coral** | `#FF5147` | record, primary action — THE brand accent |
| **Amber** | `#FFB020` | confirm, highlights, PRO |
| Teal | `#2DD4BF` | tertiary/info accents |

### Console Light
Ground `#FAF6F0` · Panel `#FFFDFA` · Panel-2 `#F1EAE1` · Line `#E4DCD2` ·
Ink `#2A2430` · Muted `#6F6478` · Coral (deepened for contrast) `#E0453C` ·
Amber `#FFB020`.

### Pad palette (cyclical, dark-ink labels `#1A1620`)
`#FF6B61` coral · `#FFB020` amber · `#2DD4BF` teal · `#A78BFA` violet ·
`#A3E635` lime · `#F472B6` pink · `#38BDF8` sky · `#FB923C` orange ·
`#F9D94A` sun · `#5EEAD4` mint

Rules: coral is for actions, never for large passive surfaces; amber marks
PRO/confirm moments; semantic red (errors) may reuse coral in dark theme but
must be the deepened `#E0453C` on light. Pads are the only place the full
rainbow appears.

## 4. Typography

| Role | Face | Weights | Notes |
|---|---|---|---|
| Display / titles / pad labels / buttons | **Baloo 2** | 600 / 700 / 800 | chunky, rounded, friendly; slight negative tracking on big sizes |
| Body / UI text / captions | **Nunito** | 400 / 600 / 700 / 800 | highly readable rounded companion |

- Bundled at `android/app/src/main/res/font/` (SIL OFL 1.1 — `android/FONTS_LICENSE.md`);
  wired through Compose `Typography` in `ui/theme/Type.kt`.
- Web equivalents: load the same two families from Google Fonts, or fall back to
  `ui-rounded / system-ui`.
- Timer/counters use tabular numerals.

## 5. Voice & tone

Warm, wry, on the parent's side. We joke about the *repetition*, never about the
kids and never guilt-trip the parent.

- ✅ "Say it once. Tap it forever." / "Nag from the lock screen." / "It hears the
  whining. It answers. You don't."
- ❌ Robot-parent framing ("let AI raise them"), shame ("stop yelling at your
  kids"), corporate speak ("leverage voice automation").
- Buttons say exactly what they do ("Record", "Unlock PRO · R89,99").
- Privacy line, verbatim wherever data is mentioned: **"Your recordings never
  leave your phone."**

## 6. Product naming

- Features: **Auto-Parent AI** (the listening mode), **Boards**, **Pads/Buttons**,
  **Repeatless PRO** (the one-time unlock — always "one-time", never "premium
  subscription").
- "Soundboard" is a generic noun in-app (fine); the brand is only ever Repeatless.

## 7. Asset inventory

| Asset | Location |
|---|---|
| Android theme (colors/type/theme) | `android/app/src/main/java/com/example/ui/theme/` |
| Launcher icons | `android/app/src/main/res/drawable/` + `mipmap-anydpi-v26/` |
| Fonts | `android/app/src/main/res/font/` |
| Widget styling | `android/app/src/main/res/layout/` + `values/colors.xml` |
| Web demo | `index.html` (repo root) |
| Privacy policy page | `privacy.html` (repo root) |
| Store listing copy | `docs/LAUNCH_PLAYBOOK.md` §3 |

Still to produce (Play listing): 512×512 icon export, 1024×500 feature graphic,
6 phone screenshots (plan in the playbook). Legacy raster mipmaps for API 24–25
should be regenerated from the new vector via Android Studio's Image Asset tool.
