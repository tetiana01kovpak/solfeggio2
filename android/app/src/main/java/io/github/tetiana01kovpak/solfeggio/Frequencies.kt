package io.github.tetiana01kovpak.solfeggio

data class Frequency(val hz: Int, val label: String, val note: String? = null)

// Same labels as the web app (app.js); scripts/check-frequencies.mjs asserts they match.
val FREQUENCIES = listOf(
    Frequency(174, "Eases pain & stress"),
    Frequency(285, "Enhances healing & regeneration"),
    Frequency(396, "Releases fear & guilt"),
    Frequency(417, "Facilitates change & letting go"),
    Frequency(528, "Encourages healing & transformation"),
    Frequency(639, "Supports connection & harmony"),
    Frequency(728, "Claimed to destroy parasites in the body", note = "A sound tone cannot do this."),
    Frequency(852, "Fosters intuition & awareness"),
)

// One earthy hue per tone, warm to cool, dark enough to carry cream text.
val TONE_COLORS = longArrayOf(
    0xFF9C4A2F, // 174 rust
    0xFFA0612A, // 285 copper
    0xFF806519, // 396 ochre
    0xFF5E7A2E, // 417 moss
    0xFF2F6B4E, // 528 forest
    0xFF2A6570, // 639 teal
    0xFF3E5588, // 728 slate
    0xFF5E4A8A, // 852 violet
)
