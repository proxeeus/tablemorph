package com.tablemorph.model;

/**
 * Enumeration of available sound preset types.
 * Each type represents a different category of synthesizer sound with specific parameter ranges.
 *
 * @author Proxeeus
 */
public enum SoundPreset {
    /** Bass sounds, typically low frequency with strong presence */
    BASS("Bass", "Bass"),
    /** Lead sounds, typically monophonic and prominent in the mix */
    LEAD("Lead", "Lead"),
    /** Pad sounds, typically atmospheric and evolving */
    PAD("Pad", "Pad"),
    /** Pluck sounds, typically short attack and decay */
    PLUCK("Pluck", "Pluck"),
    /** Keys sounds, typically piano-like or organ-like */
    KEYS("Keys", "Keys"),
    /** Atmospheric sounds, typically evolving and textural */
    ATMOSPHERE("Atmosphere", "Atmosphere"),
    /** Drum sounds, typically percussive with short envelopes */
    DRUM("Drum", "Drum");

    private final String displayName;
    private final String style;

    SoundPreset(String displayName, String style) {
        this.displayName = displayName;
        this.style = style;
    }

    public String getName() {
        return displayName;
    }

    public String getStyle() {
        return style;
    }
} 