# Soundboard

A personal, offline soundboard web app. Record short voice clips once, then tap
big colourful buttons to play them back — no more repeating yourself.

## Use it

Open `index.html` in a mobile or desktop browser. That's the whole app — a
single self-contained HTML file, no build step and no server required.

On a phone you can **Add to Home Screen** so it opens fullscreen like a native app:

- **iPhone (Safari):** Share icon → *Add to Home Screen*
- **Android (Chrome):** menu (⋮) → *Add to Home screen*

## How it works

1. Tap **＋ Record**, record a clip (up to 30 seconds), and give it a label.
2. The clip becomes a coloured button — tap to play, tap again to stop.
3. The **⋯** on any button lets you rename, re-record, recolour, or delete it.

## Your data stays with you

- Recordings are stored **locally in your browser** (IndexedDB). Nothing is
  uploaded anywhere.
- Because the data lives in the browser, clearing your browser data will erase
  the clips. Use **⋯ → Back up to a file** to save a copy, and **Restore from a
  file** to bring it back or move it to another device.

## Hosting (optional)

Since it's just static files, you can host it for free with GitHub Pages:
enable Pages for this repo and point it at the branch root — `index.html` will
be served as the site.

## Tech notes

- Pure HTML/CSS/JS, no dependencies.
- Uses the `MediaRecorder` API to record and `IndexedDB` to store audio blobs.
- Requires microphone permission the first time you record.
