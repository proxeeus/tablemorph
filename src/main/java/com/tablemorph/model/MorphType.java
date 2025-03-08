package com.tablemorph.model;

/**
 * Enumeration of available morphing algorithms.
 */
public enum MorphType {
    BLEND(1, "Simple Blending", "Simple blending between generated and sample data"),
    ADDITIVE(2, "Additive", "Add sample data to generated data"),
    HARMONIC(3, "Harmonic", "Use sample data to influence harmonic content"),
    FOLD(4, "Wave Folding", "Apply waveshaping to sample data"),
    SPECTRAL(5, "Spectral", "Mix in spectral domain");
    
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