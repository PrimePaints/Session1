package app.repeatless.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Repeatless brand palette ────────────────────────────────────────────────
// Warm "drum-machine console" identity (see docs/BRAND.md). Dark is the
// signature theme; light is a warm-paper companion, not a naive inversion.

// Console Dark (signature)
val GroundDark = Color(0xFF1A1620)      // warm near-black ground
val PanelDark = Color(0xFF241E2B)       // sheets, bars
val Panel2Dark = Color(0xFF2E2735)      // inputs, raised rows
val LineDark = Color(0xFF3A3242)        // borders / dividers
val TextCream = Color(0xFFF4ECE2)       // primary text on dark
val TextMutedDark = Color(0xFFA495AD)   // secondary text on dark

// Console Light (warm paper)
val GroundLight = Color(0xFFFAF6F0)     // warm paper ground
val PanelLight = Color(0xFFFFFDFA)      // cards / sheets
val Panel2Light = Color(0xFFF1EAE1)     // inputs, raised rows
val LineLight = Color(0xFFE4DCD2)       // borders / dividers
val TextDark = Color(0xFF2A2430)        // primary text on light
val TextMutedLight = Color(0xFF6F6478)  // secondary text on light

// Accents
val RecRed = Color(0xFFFF5147)          // coral — record / primary action (dark theme)
val RecRedDeep = Color(0xFFE0453C)      // deepened coral for light-theme contrast
val AmberAccent = Color(0xFFFFB020)     // amber — confirm / highlights / Pro
val BentoBlue = Color(0xFF2DD4BF)       // brand teal (legacy name kept for callers)
val BentoLightCard = Color(0xFFF1EAE1)  // legacy name kept for callers

// ─── Pad palette ─────────────────────────────────────────────────────────────
// Punchy, distinguishable-at-a-glance pad colours, assigned cyclically to new
// pads. Dark ink (#1A1620) reads well on all of them.
val PadColors = listOf(
    Color(0xFFFF6B61), // coral
    Color(0xFFFFB020), // amber
    Color(0xFF2DD4BF), // teal
    Color(0xFFA78BFA), // violet
    Color(0xFFA3E635), // lime
    Color(0xFFF472B6), // pink
    Color(0xFF38BDF8), // sky
    Color(0xFFFB923C), // orange
    Color(0xFFF9D94A), // sun
    Color(0xFF5EEAD4)  // mint
)

val PadHexStrings = listOf(
    "#ff6b61",
    "#ffb020",
    "#2dd4bf",
    "#a78bfa",
    "#a3e635",
    "#f472b6",
    "#38bdf8",
    "#fb923c",
    "#f9d94a",
    "#5eead4"
)
