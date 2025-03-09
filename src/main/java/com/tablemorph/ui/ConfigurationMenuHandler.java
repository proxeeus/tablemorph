package com.tablemorph.ui;

import com.tablemorph.config.GeneratorConfig;
import com.tablemorph.service.ServiceFactory;

/**
 * Handles the configuration menu and settings.
 */
public class ConfigurationMenuHandler {
    
    /**
     * Displays the configuration menu and handles user interaction.
     */
    public void displayConfigMenu() {
        while (true) {
            TextUI.displayHeader("CONFIGURATION");
            
            String[] menuItems = {
                "1. Wavetable Settings",
                "2. Single-Cycle Settings",
                "3. Morphing Settings",
                "4. Vital Integration",
                "5. Sound File Cache",
                "6. Back to Main Menu"
            };
            
            for (String item : menuItems) {
                System.out.println("║  " + item + TextUI.getSpaces(30 - item.length()) + "║");
            }
            
            System.out.println("╚════════════════════════════════════╝");
            
            System.out.print("\nEnter your choice > ");
            String choice = TextUI.readLine();
            
            switch (choice) {
                case "1":
                    configureWavetableSettings();
                    break;
                case "2":
                    configureSingleCycleSettings();
                    break;
                case "3":
                    configureMorphSettings();
                    break;
                case "4":
                    configureVitalIntegration();
                    break;
                case "5":
                    configureSoundFileCache();
                    break;
                case "6":
                    return;
                default:
                    System.out.println("\n[ERROR] Invalid choice. Please try again.");
            }
        }
    }
    
    /**
     * Configures wavetable generation settings.
     */
    private void configureWavetableSettings() {
        while (true) {
            TextUI.displayHeader("WAVETABLE SETTINGS");
            
            System.out.println("\nCurrent Settings:");
            System.out.println("1. Frame Count: " + GeneratorConfig.getWavetableFrames());
            System.out.println("2. Sample Count: " + GeneratorConfig.getWavetableSamples());
            System.out.println("3. Modulation Probability: " + 
                String.format("%.1f", GeneratorConfig.getWavetableModulationProbability() * 100) + "%");
            System.out.println("4. Back to Configuration Menu");
            
            System.out.print("\nEnter your choice (1-4) > ");
            String choice = TextUI.readLine();
            
            switch (choice) {
                case "1":
                    System.out.println("\nThe frame count determines how many frames are in each wavetable.");
                    System.out.println("More frames means smoother morphing but larger files.");
                    System.out.println("Common values: 64, 128, 256");
                    
                    int frames = TextUI.readInt("\nEnter new frame count (16-256): ", 
                                             16, 256, GeneratorConfig.getWavetableFrames());
                    GeneratorConfig.setWavetableFrames(frames);
                    System.out.println("[OK] Frame count updated to " + frames);
                    break;
                    
                case "2":
                    System.out.println("\nThe sample count determines the resolution of each frame.");
                    System.out.println("Higher values create more detailed waveforms but result in larger files.");
                    System.out.println("Common values: 2048, 4096");
                    
                    int samples = TextUI.readInt("\nEnter new sample count (256-8192, must be a power of 2): ", 
                                             256, 8192, GeneratorConfig.getWavetableSamples());
                    
                    // Check if it's a power of 2
                    if ((samples & (samples - 1)) == 0) {
                        GeneratorConfig.setWavetableSamples(samples);
                        System.out.println("[OK] Sample count updated to " + samples);
                    } else {
                        System.out.println("[ERROR] Sample count must be a power of 2 (256, 512, 1024, etc.)");
                    }
                    break;
                    
                case "3":
                    System.out.println("\nThe modulation probability determines how often modulation is applied");
                    System.out.println("to the waveforms during generation. Higher values create more complex sounds.");
                    
                    double probability = TextUI.readDouble("\nEnter new probability (0-100%): ", 
                                                       0, 100, 
                                                       GeneratorConfig.getWavetableModulationProbability() * 100);
                    GeneratorConfig.setWavetableModulationProbability(probability / 100.0);
                    System.out.println("[OK] Modulation probability updated to " + probability + "%");
                    break;
                    
                case "4":
                    return;
                    
                default:
                    System.out.println("\n[ERROR] Invalid choice. Please try again.");
            }
        }
    }
    
    /**
     * Configures single-cycle wavetable settings.
     */
    private void configureSingleCycleSettings() {
        while (true) {
            TextUI.displayHeader("SINGLE-CYCLE SETTINGS");
            
            System.out.println("\nCurrent Settings:");
            System.out.println("1. Sample Count: " + GeneratorConfig.getSingleCycleSamples());
            System.out.println("2. Experimental Waveform Probability: " + 
                String.format("%.1f", GeneratorConfig.getExperimentalWaveformProbability() * 100) + "%");
            System.out.println("3. Back to Configuration Menu");
            
            System.out.print("\nEnter your choice (1-3) > ");
            String choice = TextUI.readLine();
            
            switch (choice) {
                case "1":
                    System.out.println("\nThe sample count determines the resolution of each single-cycle wavetable.");
                    System.out.println("Higher values create more detailed waveforms but result in larger files.");
                    System.out.println("Common values: 1024, 2048, 4096");
                    
                    int samples = TextUI.readInt("\nEnter new sample count (256-8192, must be a power of 2): ", 
                                             256, 8192, GeneratorConfig.getSingleCycleSamples());
                    
                    // Check if it's a power of 2
                    if ((samples & (samples - 1)) == 0) {
                        GeneratorConfig.setSingleCycleSamples(samples);
                        System.out.println("[OK] Sample count updated to " + samples);
                    } else {
                        System.out.println("[ERROR] Sample count must be a power of 2 (256, 512, 1024, etc.)");
                    }
                    break;
                
                case "2":
                    System.out.println("\nThe experimental waveform probability determines how often standard waveforms");
                    System.out.println("are replaced with more complex, experimental algorithms.");
                    System.out.println("Higher values result in more unique and surprising timbres.");
                    
                    double probability = TextUI.readDouble("\nEnter new probability (0-100%): ", 
                                                       0, 100, 
                                                       GeneratorConfig.getExperimentalWaveformProbability() * 100);
                    GeneratorConfig.setExperimentalWaveformProbability(probability / 100.0);
                    System.out.println("[OK] Experimental waveform probability updated to " + probability + "%");
                    break;
                    
                case "3":
                    return;
                    
                default:
                    System.out.println("\n[ERROR] Invalid choice. Please try again.");
            }
        }
    }
    
    /**
     * Configures morphing settings.
     */
    private void configureMorphSettings() {
        while (true) {
            TextUI.displayHeader("MORPHING SETTINGS");
            
            System.out.println("\nCurrent Settings:");
            System.out.println("1. Maximum Morph Samples: " + GeneratorConfig.getMaxMorphSamples());
            System.out.println("2. Full Sample Probability: " + 
                String.format("%.1f", GeneratorConfig.getFullSampleProbability() * 100) + "%");
            System.out.println("3. Back to Configuration Menu");
            
            System.out.print("\nEnter your choice (1-3) > ");
            String choice = TextUI.readLine();
            
            switch (choice) {
                case "1":
                    System.out.println("\nThe maximum morph samples setting limits how many sound files");
                    System.out.println("are used in each morphing operation.");
                    
                    int maxSamples = TextUI.readInt("\nEnter new maximum (1-20): ", 
                                                 1, 20, GeneratorConfig.getMaxMorphSamples());
                    GeneratorConfig.setMaxMorphSamples(maxSamples);
                    System.out.println("[OK] Maximum morph samples updated to " + maxSamples);
                    break;
                    
                case "2":
                    System.out.println("\nThe full sample probability determines how often complete samples");
                    System.out.println("are used instead of extracting a small section.");
                    System.out.println("Higher values result in more complete sample usage but may result in less variety.");
                    
                    double probability = TextUI.readDouble("\nEnter new probability (0-100%): ", 
                                                       0, 100, 
                                                       GeneratorConfig.getFullSampleProbability() * 100);
                    GeneratorConfig.setFullSampleProbability(probability / 100.0);
                    System.out.println("[OK] Full sample probability updated to " + probability + "%");
                    break;
                    
                case "3":
                    return;
                    
                default:
                    System.out.println("\n[ERROR] Invalid choice. Please try again.");
            }
        }
    }
    
    /**
     * Configures Vital integration settings.
     */
    private void configureVitalIntegration() {
        TextUI.displayHeader("VITAL INTEGRATION");
        
        // Check if OS is supported
        if (!GeneratorConfig.isOSSupported()) {
            System.out.println("\n[WARNING] Your operating system is not currently supported for");
            System.out.println("automatic Vital integration. You will need to manually copy wavetables");
            System.out.println("to your Vital installation.");
            TextUI.waitForEnter(null);
            return;
        }
        
        // Display current settings
        System.out.println("\nCurrent Settings:");
        System.out.println("1. Save to Vital: " + (GeneratorConfig.getSaveToVital() ? "Enabled" : "Disabled"));
        System.out.println("2. Vital Wavetables Directory: " + GeneratorConfig.getVitalWavetablesDirectory());
        System.out.println("3. Reset to Default Directory");
        System.out.println("4. Back to Configuration Menu");
        
        System.out.print("\nEnter your choice (1-4) > ");
        String choice = TextUI.readLine();
        
        switch (choice) {
            case "1":
                boolean currentSetting = GeneratorConfig.getSaveToVital();
                boolean newSetting = TextUI.readBoolean("\nEnable automatic saving to Vital?", currentSetting);
                GeneratorConfig.setSaveToVital(newSetting);
                System.out.println("[OK] Vital integration " + (newSetting ? "enabled" : "disabled"));
                break;
                
            case "2":
                System.out.println("\nEnter the full path to your Vital wavetables directory:");
                System.out.print("> ");
                String directory = TextUI.readLine();
                
                if (directory.isEmpty()) {
                    System.out.println("[ERROR] Directory cannot be empty");
                } else {
                    GeneratorConfig.setVitalWavetablesDirectory(directory);
                    System.out.println("[OK] Vital wavetables directory updated");
                }
                break;
                
            case "3":
                String defaultDir = GeneratorConfig.getDefaultVitalDirectory();
                GeneratorConfig.setVitalWavetablesDirectory(defaultDir);
                System.out.println("[OK] Reset to default: " + defaultDir);
                break;
                
            case "4":
                return;
                
            default:
                System.out.println("\n[ERROR] Invalid choice. Please try again.");
        }
        
        TextUI.waitForEnter(null);
    }
    
    /**
     * Configures sound file cache settings.
     */
    private void configureSoundFileCache() {
        TextUI.displayHeader("SOUND FILE CACHE");
        
        // Display current settings
        boolean cacheEnabled = GeneratorConfig.getCacheEnabled();
        int cacheLifetime = GeneratorConfig.getCacheLifetimeMinutes();
        
        System.out.println("\nCurrent Settings:");
        System.out.println("1. Cache Enabled: " + (cacheEnabled ? "Yes" : "No"));
        System.out.println("2. Cache Lifetime: " + cacheLifetime + " minutes");
        System.out.println("3. View Cache Statistics");
        System.out.println("4. Invalidate Cache");
        System.out.println("5. Back to Configuration Menu");
        
        System.out.print("\nEnter your choice (1-5) > ");
        String choice = TextUI.readLine();
        
        switch (choice) {
            case "1":
                boolean newValue = TextUI.readBoolean("\nEnable sound file cache?", cacheEnabled);
                GeneratorConfig.setCacheEnabled(newValue);
                System.out.println("[OK] Cache " + (newValue ? "enabled" : "disabled"));
                break;
                
            case "2":
                System.out.println("\nThe cache lifetime determines how long the cache is valid");
                System.out.println("before it needs to be refreshed (in minutes).");
                
                int minutes = TextUI.readInt("\nEnter new cache lifetime (1-1440 minutes): ", 
                                           1, 1440, cacheLifetime);
                GeneratorConfig.setCacheLifetimeMinutes(minutes);
                System.out.println("[OK] Cache lifetime updated to " + minutes + " minutes");
                break;
                
            case "3":
                System.out.println("\nCache Statistics:");
                String stats = ServiceFactory.getInstance().getSoundFileCacheManager().getCacheStats();
                System.out.println(stats);
                break;
                
            case "4":
                boolean confirm = TextUI.readBoolean("\nAre you sure you want to invalidate the cache?", false);
                if (confirm) {
                    ServiceFactory.getInstance().getSoundFileCacheManager().invalidateCache();
                    System.out.println("[OK] Cache invalidated");
                } else {
                    System.out.println("[OK] Cache invalidation cancelled");
                }
                break;
                
            case "5":
                return;
                
            default:
                System.out.println("\n[ERROR] Invalid choice. Please try again.");
        }
        
        TextUI.waitForEnter(null);
    }
} 