package com.tablemorph.model;

/**
 * Enumeration of available morphing algorithms.
 * <p>
 * Each algorithm implements a different approach to combining generated wavetables 
 * with sample data, producing unique sonic textures with varying characteristics.
 */
public enum MorphType {
    BLEND(1, "Simple Blending", "Non-linear blending of waveforms with dynamic intensity and phase modulation"),
    ADDITIVE(2, "Additive", "Combines harmonic content with selective scaling and phase manipulation"),
    HARMONIC(3, "Harmonic", "Uses sample data to modulate harmonic structure with frequency-dependent processing"),
    FOLD(4, "Wave Folding", "Applies adaptive waveshaping with multiple folding thresholds and saturation"),
    SPECTRAL(5, "Spectral", "Multi-band spectral processing with independent frequency transformations");
    
    private final int id;
    private final String name;
    private final String description;
    
    MorphType(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }
    
    public int getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets a MorphType by its ID.
     * 
     * @param id The ID to look up
     * @return The corresponding MorphType or BLEND if not found
     */
    public static MorphType getById(int id) {
        for (MorphType type : values()) {
            if (type.getId() == id) {
                return type;
            }
        }
        return BLEND; // Default to blend
    }
    
    /**
     * Gets a random morph type.
     * 
     * @return A random morph type
     */
    public static MorphType getRandom() {
        return values()[(int)(Math.random() * values().length)];
    }
} 