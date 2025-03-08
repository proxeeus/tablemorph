package com.tablemorph.util;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.tablemorph.config.GeneratorConfig;

/**
 * Utility class for directory operations.
 */
public class DirectoryUtil {
    private static final String WAVETABLE_DIRECTORY = "wavetables";
    private static final String SOUNDS_DIRECTORY = "sounds";
    private static final String MORPHS_DIRECTORY = "morphs";
    
    /**
     * Creates the wavetable directory if it doesn't exist.
     */
    public static void createWavetableDirectory() {
        createDirectoryIfNotExists(WAVETABLE_DIRECTORY);
    }
    
    /**
     * Creates the single-cycle wavetable directory if it doesn't exist.
     */
    public static void createSingleCycleDirectory() {
        createDirectoryIfNotExists(GeneratorConfig.getSingleCycleDirectory());
    }
    
    /**
     * Creates the sounds directory if it doesn't exist.
     */
    public static void createSoundsDirectory() {
        createDirectoryIfNotExists(SOUNDS_DIRECTORY);
    }
    
    /**
     * Creates the morphs directory if it doesn't exist.
     */
    public static void createMorphsDirectory() {
        createDirectoryIfNotExists(MORPHS_DIRECTORY);
    }
    
    /**
     * Creates a directory if it doesn't exist.
     * 
     * @param directoryPath The path of the directory to create
     */
    public static void createDirectoryIfNotExists(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdir();
        }
    }
    
    /**
     * Gets the absolute path of the wavetable directory.
     * 
     * @return The wavetable directory path
     */
    public static String getWavetableDirectoryPath() {
        return new File(WAVETABLE_DIRECTORY).getAbsolutePath();
    }
    
    /**
     * Gets the absolute path of the single-cycle wavetable directory.
     * 
     * @return The single-cycle directory path
     */
    public static String getSingleCycleDirectoryPath() {
        return new File(GeneratorConfig.getSingleCycleDirectory()).getAbsolutePath();
    }
    
    /**
     * Gets the absolute path of the sounds directory.
     * 
     * @return The sounds directory path
     */
    public static String getSoundsDirectoryPath() {
        return new File(SOUNDS_DIRECTORY).getAbsolutePath();
    }
    
    /**
     * Gets the absolute path of the morphs directory.
     * 
     * @return The morphs directory path
     */
    public static String getMorphsDirectoryPath() {
        return new File(MORPHS_DIRECTORY).getAbsolutePath();
    }
    
    /**
     * Ensures the Vital wavetables directory exists.
     * 
     * @return true if successful, false otherwise
     */
    public static boolean ensureVitalDirectory() {
        if (!GeneratorConfig.getSaveToVital() || !GeneratorConfig.isOSSupported()) {
            return false;
        }
        
        String vitalDir = GeneratorConfig.getVitalWavetablesDirectory();
        if (vitalDir.isEmpty()) {
            return false;
        }
        
        File vitalDirectory = new File(vitalDir);
        if (!vitalDirectory.exists()) {
            return vitalDirectory.mkdirs();
        }
        
        return true;
    }
    
    /**
     * Gets a path to save a file in the Vital wavetables directory.
     * 
     * @param filename The filename to save
     * @return The path, or null if Vital integration is disabled
     */
    public static Path getVitalPath(String filename) {
        if (!GeneratorConfig.getSaveToVital() || !GeneratorConfig.isOSSupported()) {
            return null;
        }
        
        String vitalDir = GeneratorConfig.getVitalWavetablesDirectory();
        if (vitalDir.isEmpty()) {
            return null;
        }
        
        return Paths.get(vitalDir, filename);
    }
} 