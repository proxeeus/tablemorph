package com.tablemorph.model;

/**
 * Enumeration of available single-cycle waveform types.
 */
public enum WaveformType {
    SINE(0, "sine"),
    TRIANGLE(1, "triangle"),
    SAW(2, "saw"),
    SQUARE(3, "square"),
    NOISE(4, "noise"),
    FM(5, "fm"),
    ADDITIVE(6, "additive"),
    FORMANT(7, "formant"),
    CUSTOM(8, "custom"),
    EXPERIMENTAL(9, "experimental");
    
    private final int id;
    private final String name;
    
    WaveformType(int id, String name) {
        this.id = id;
        this.name = name;
    }
    
    public int getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * Gets a WaveformType by its ID.
     * 
     * @param id The ID to look up
     * @return The corresponding WaveformType or SINE if not found
     */
    public static WaveformType getById(int id) {
        for (WaveformType type : values()) {
            if (type.getId() == id) {
                return type;
            }
        }
        return SINE; // Default to sine wave
    }
    
    /**
     * Gets a random waveform type.
     * 
     * @param includeExperimental Whether to include experimental waveforms
     * @return A random waveform type
     */
    public static WaveformType getRandom(boolean includeExperimental) {
        int max = includeExperimental ? values().length : EXPERIMENTAL.getId();
        return getById((int)(Math.random() * max));
    }
} 