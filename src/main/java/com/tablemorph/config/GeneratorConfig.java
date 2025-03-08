package com.tablemorph.config;

import java.io.*;
import java.util.Properties;

/**
 * Configuration class for TableMorph.
 * Handles loading and saving of configuration settings.
 *
 * @author Proxeeus
 */
public class GeneratorConfig {
    private static final String CONFIG_FILE = "tablemorph_config.properties";
    private static final Properties properties = new Properties();
    
    // Default values
    private static final double DEFAULT_WAVETABLE_PROBABILITY = 0.3;
    public static final int DEFAULT_WAVETABLE_FRAMES = 64;
    public static final int DEFAULT_WAVETABLE_SAMPLES = 2048;
    private static final double DEFAULT_WAVETABLE_MODULATION_PROBABILITY = 0.5;
    private static final int DEFAULT_MAX_MORPH_SAMPLES = 10; // Default max number of sample files to use
    private static final double DEFAULT_FULL_SAMPLE_PROBABILITY = 0.3; // 30% chance of using complete samples
    
    // Single-cycle wavetable defaults
    private static final int DEFAULT_SINGLECYCLE_SAMPLES = 2048; // Default samples for single-cycle wavetable
    private static final String DEFAULT_SINGLECYCLE_DIRECTORY = "singlecycles"; // Directory for single-cycle wavetables
    
    // OS-specific default Vital wavetable directories
    private static final String DEFAULT_VITAL_WAVETABLES_MAC = System.getProperty("user.home") + 
        "/Music/Vital/User/Wavetables";
    private static final String DEFAULT_VITAL_WAVETABLES_WINDOWS = System.getenv("APPDATA") + 
        "\\Vital\\wavetables";
    
    // Whether to also save to Vital's directory
    private static final boolean DEFAULT_SAVE_TO_VITAL = true;
    
    static {
        loadConfig();
    }
    
    /**
     * Loads the configuration from file or creates default if not exists.
     */
    private static void loadConfig() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                properties.load(fis);
            } catch (IOException e) {
                System.out.println("Warning: Could not load config file. Using defaults.");
                setDefaultValues();
            }
        } else {
            setDefaultValues();
            saveConfig();
        }
    }
    
    /**
     * Sets default configuration values.
     */
    private static void setDefaultValues() {
        properties.setProperty("wavetable.probability", String.valueOf(DEFAULT_WAVETABLE_PROBABILITY));
        properties.setProperty("wavetable.frames", String.valueOf(DEFAULT_WAVETABLE_FRAMES));
        properties.setProperty("wavetable.samples", String.valueOf(DEFAULT_WAVETABLE_SAMPLES));
        properties.setProperty("wavetable.modulation.probability", 
            String.valueOf(DEFAULT_WAVETABLE_MODULATION_PROBABILITY));
        properties.setProperty("wavetable.morph.max_samples", String.valueOf(DEFAULT_MAX_MORPH_SAMPLES));
        properties.setProperty("wavetable.morph.full_sample_probability", 
            String.valueOf(DEFAULT_FULL_SAMPLE_PROBABILITY));
        
        // Set single-cycle defaults
        properties.setProperty("singlecycle.samples", String.valueOf(DEFAULT_SINGLECYCLE_SAMPLES));
        properties.setProperty("singlecycle.directory", DEFAULT_SINGLECYCLE_DIRECTORY);
        
        // Set OS-specific Vital directory
        String osName = System.getProperty("os.name").toLowerCase();
        String defaultVitalDir = osName.contains("mac") ? DEFAULT_VITAL_WAVETABLES_MAC :
                                osName.contains("windows") ? DEFAULT_VITAL_WAVETABLES_WINDOWS :
                                "";
        
        properties.setProperty("vital.wavetables.directory", defaultVitalDir);
        properties.setProperty("vital.save_to_vital", String.valueOf(DEFAULT_SAVE_TO_VITAL));
    }
    
    /**
     * Saves the current configuration to file.
     */
    public static void saveConfig() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            properties.store(fos, "TableMorph Configuration");
        } catch (IOException e) {
            System.out.println("Error: Could not save config file: " + e.getMessage());
        }
    }
    
    /**
     * Gets the probability of using a wavetable in patch generation.
     */
    public static double getWavetableProbability() {
        return Double.parseDouble(properties.getProperty("wavetable.probability", 
            String.valueOf(DEFAULT_WAVETABLE_PROBABILITY)));
    }
    
    /**
     * Sets the probability of using a wavetable in patch generation.
     */
    public static void setWavetableProbability(double probability) {
        properties.setProperty("wavetable.probability", String.valueOf(probability));
        saveConfig();
    }
    
    /**
     * Gets the number of frames in generated wavetables.
     */
    public static int getWavetableFrames() {
        return Integer.parseInt(properties.getProperty("wavetable.frames", 
            String.valueOf(DEFAULT_WAVETABLE_FRAMES)));
    }
    
    /**
     * Sets the number of frames in generated wavetables.
     */
    public static void setWavetableFrames(int frames) {
        properties.setProperty("wavetable.frames", String.valueOf(frames));
        saveConfig();
    }
    
    /**
     * Gets the number of samples per frame in generated wavetables.
     */
    public static int getWavetableSamples() {
        return Integer.parseInt(properties.getProperty("wavetable.samples", 
            String.valueOf(DEFAULT_WAVETABLE_SAMPLES)));
    }
    
    /**
     * Sets the number of samples per frame in generated wavetables.
     */
    public static void setWavetableSamples(int samples) {
        properties.setProperty("wavetable.samples", String.valueOf(samples));
        saveConfig();
    }
    
    /**
     * Gets the probability of modulating wavetable position.
     */
    public static double getWavetableModulationProbability() {
        return Double.parseDouble(properties.getProperty("wavetable.modulation.probability", 
            String.valueOf(DEFAULT_WAVETABLE_MODULATION_PROBABILITY)));
    }
    
    /**
     * Sets the probability of modulating wavetable position.
     */
    public static void setWavetableModulationProbability(double probability) {
        properties.setProperty("wavetable.modulation.probability", String.valueOf(probability));
        saveConfig();
    }
    
    /**
     * Gets the maximum number of sample files to use during morphing.
     */
    public static int getMaxMorphSamples() {
        return Integer.parseInt(properties.getProperty("wavetable.morph.max_samples", 
            String.valueOf(DEFAULT_MAX_MORPH_SAMPLES)));
    }
    
    /**
     * Sets the maximum number of sample files to use during morphing.
     */
    public static void setMaxMorphSamples(int maxSamples) {
        properties.setProperty("wavetable.morph.max_samples", String.valueOf(maxSamples));
        saveConfig();
    }
    
    /**
     * Gets the probability of using a complete sample during morphing.
     * Higher values increase the chance that a full sample will be used instead of a section.
     */
    public static double getFullSampleProbability() {
        return Double.parseDouble(properties.getProperty("wavetable.morph.full_sample_probability", 
            String.valueOf(DEFAULT_FULL_SAMPLE_PROBABILITY)));
    }
    
    /**
     * Sets the probability of using a complete sample during morphing.
     * @param probability Value between 0.0 and 1.0
     */
    public static void setFullSampleProbability(double probability) {
        properties.setProperty("wavetable.morph.full_sample_probability", String.valueOf(probability));
        saveConfig();
    }
    
    /**
     * Gets whether to save wavetables to Vital's directory.
     */
    public static boolean getSaveToVital() {
        return Boolean.parseBoolean(properties.getProperty("vital.save_to_vital", 
            String.valueOf(DEFAULT_SAVE_TO_VITAL)));
    }
    
    /**
     * Sets whether to save wavetables to Vital's directory.
     */
    public static void setSaveToVital(boolean saveToVital) {
        properties.setProperty("vital.save_to_vital", String.valueOf(saveToVital));
        saveConfig();
    }
    
    /**
     * Gets Vital's wavetables directory.
     */
    public static String getVitalWavetablesDirectory() {
        return properties.getProperty("vital.wavetables.directory", getDefaultVitalDirectory());
    }
    
    /**
     * Sets Vital's wavetables directory.
     */
    public static void setVitalWavetablesDirectory(String directory) {
        properties.setProperty("vital.wavetables.directory", directory);
        saveConfig();
    }
    
    /**
     * Gets the default Vital wavetables directory based on OS.
     */
    public static String getDefaultVitalDirectory() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("mac") ? DEFAULT_VITAL_WAVETABLES_MAC :
               osName.contains("windows") ? DEFAULT_VITAL_WAVETABLES_WINDOWS :
               "";
    }
    
    /**
     * Checks if the current OS is supported for Vital integration.
     */
    public static boolean isOSSupported() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("mac") || osName.contains("windows");
    }
    
    /**
     * Gets the number of samples per single-cycle wavetable.
     */
    public static int getSingleCycleSamples() {
        return Integer.parseInt(properties.getProperty("singlecycle.samples", 
            String.valueOf(DEFAULT_SINGLECYCLE_SAMPLES)));
    }
    
    /**
     * Sets the number of samples per single-cycle wavetable.
     */
    public static void setSingleCycleSamples(int samples) {
        properties.setProperty("singlecycle.samples", String.valueOf(samples));
        saveConfig();
    }
    
    /**
     * Gets the directory for single-cycle wavetables.
     */
    public static String getSingleCycleDirectory() {
        return properties.getProperty("singlecycle.directory", DEFAULT_SINGLECYCLE_DIRECTORY);
    }
    
    /**
     * Sets the directory for single-cycle wavetables.
     */
    public static void setSingleCycleDirectory(String directory) {
        properties.setProperty("singlecycle.directory", directory);
        saveConfig();
    }
} 