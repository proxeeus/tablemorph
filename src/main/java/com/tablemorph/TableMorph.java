package com.tablemorph;

import com.tablemorph.config.GeneratorConfig;
import com.tablemorph.service.ServiceFactory;
import com.tablemorph.ui.MenuHandler;
import com.tablemorph.ui.TextUI;
import com.tablemorph.util.DirectoryUtil;

/**
 * TableMorph: A wavetable generator for the Vital synthesizer.
 * Supports:
 * - Multi-frame complex wavetables with various waveform types
 * - Single-cycle oscillator waveforms (sine, triangle, saw, square, noise, FM, additive, formant)
 * - Experimental waveforms with advanced algorithms
 * - Morphing with audio samples
 * - Batch generation of all wavetable types
 *
 * @author Proxeeus
 * @version 1.1
 */
public class TableMorph {
    
    /**
     * Application entry point.
     * 
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        // Initialize directories
        initializeDirectories();
        
        // Initialize sound file cache
        initializeSoundFileCache();
        
        // Display logo
        TextUI.displayLogo();
        
        // Start the menu handler
        MenuHandler menuHandler = new MenuHandler();
        menuHandler.displayMenu();
    }
    
    /**
     * Creates the necessary directories if they don't exist.
     */
    private static void initializeDirectories() {
        DirectoryUtil.createWavetableDirectory();
        DirectoryUtil.createSingleCycleDirectory();
        DirectoryUtil.createSoundsDirectory();
        DirectoryUtil.createMorphsDirectory();
    }
    
    /**
     * Initializes the sound file cache.
     */
    private static void initializeSoundFileCache() {
        if (GeneratorConfig.getCacheEnabled()) {
            ServiceFactory.getInstance().getSoundFileCacheManager().initializeCache();
        }
    }
} 