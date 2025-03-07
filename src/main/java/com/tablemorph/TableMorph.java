package com.tablemorph;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

import com.tablemorph.service.WavetableGenerator;
import com.tablemorph.config.GeneratorConfig;

/**
 * TableMorph: A generator for creating random wavetables for the Vital synthesizer.
 * Creates complex multi-frame wavetables with various waveform types and morphing.
 * 
 * @author Proxeeus
 * @version 1.0
 */
public class TableMorph {
    
    /** Scanner for reading user input */
    private static final Scanner scanner = new Scanner(System.in);
    
    /** Service instance for wavetable generation */
    private static final WavetableGenerator wavetableGenerator = new WavetableGenerator();
    
    /** Random number generator */
    private static final Random random = new Random();
    
    /** Folder for input audio files */
    private static final String SOUNDS_DIRECTORY = "sounds";
    
    /** Folder for morphed wavetables */
    private static final String MORPHS_DIRECTORY = "morphs";

    /**
     * Main entry point for the application.
     * 
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        displayLogo();
        System.out.println("Welcome to TableMorph - Wavetable Generator for Vital!");
        
        try {
            // Create all required directories
            wavetableGenerator.createWavetableDirectory();
            createSoundsDirectory();
            createMorphsDirectory();
            
            while (true) {
                displayMenu();
            }
        } catch (Exception e) {
            System.out.println("\n❌ Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
    
    /**
     * Creates the sounds directory if it doesn't exist.
     */
    private static void createSoundsDirectory() {
        File soundsDir = new File(SOUNDS_DIRECTORY);
        if (!soundsDir.exists()) {
            soundsDir.mkdir();
            System.out.println("\n📁 Created 'sounds' directory for your audio samples");
            System.out.println("   Add WAV files to this folder to use them in morphed wavetables");
        }
    }
    
    /**
     * Creates the morphs directory if it doesn't exist.
     */
    private static void createMorphsDirectory() {
        File morphsDir = new File(MORPHS_DIRECTORY);
        if (!morphsDir.exists()) {
            morphsDir.mkdir();
            System.out.println("\n📁 Created 'morphs' directory for morphed wavetables");
        }
    }

    /**
     * Displays the ASCII art logo of the application.
     */
    private static void displayLogo() {
        String logo = 
            "\n" +
            "████████╗ █████╗ ██████╗ ██╗     ███████╗███╗   ███╗ ██████╗ ██████╗ ██████╗ ██╗  ██╗\n" +
            "╚══██╔══╝██╔══██╗██╔══██╗██║     ██╔════╝████╗ ████║██╔═══██╗██╔══██╗██╔══██╗██║  ██║\n" +
            "   ██║   ███████║██████╔╝██║     █████╗  ██╔████╔██║██║   ██║██████╔╝██████╔╝███████║\n" +
            "   ██║   ██╔══██║██╔══██╗██║     ██╔══╝  ██║╚██╔╝██║██║   ██║██╔══██╗██╔═══╝ ██╔══██║\n" +
            "   ██║   ██║  ██║██████╔╝███████╗███████╗██║ ╚═╝ ██║╚██████╔╝██║  ██║██║     ██║  ██║\n" +
            "   ╚═╝   ╚═╝  ╚═╝╚═════╝ ╚══════╝╚══════╝╚═╝     ╚═╝ ╚═════╝ ╚═╝  ╚═╝╚═╝     ╚═╝  ╚═╝\n" +
            "                                                                                      \n" +
            "               Wavetable Generator for the Vital Synthesizer                          \n" +
            "                                                                                      \n";
        System.out.println(logo);
    }

    /**
     * Displays the main menu and handles user input.
     */
    private static void displayMenu() {
        while (true) {
            System.out.println("\n╔════════════════════════════════════╗");
            System.out.println("║            MAIN MENU              ║");
            System.out.println("╠════════════════════════════════════╣");
            System.out.println("║  1. 🎵 Generate Wavetable          ║");
            System.out.println("║  2. 🧬 Morph with Samples          ║");
            System.out.println("║  3. 📦 Batch Generate Wavetables   ║");
            System.out.println("║  4. 🧪 Batch Morph Wavetables      ║");
            System.out.println("║  5. ℹ️ Wavetable Info             ║");
            System.out.println("║  6. 🧙 Configuration               ║");
            System.out.println("║  7. 👋 Quit                        ║");
            System.out.println("╚════════════════════════════════════╝");
            System.out.print("Enter your choice > ");
            
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    generateRandomWavetable();
                    break;
                case "2":
                    generateMorphedWavetable();
                    break;
                case "3":
                    generateBatchWavetables();
                    break;
                case "4":
                    generateBatchMorphedWavetables();
                    break;
                case "5":
                    displayWavetableInfo();
                    break;
                case "6":
                    displayConfigMenu();
                    break;
                case "7":
                    System.out.println("\nGoodbye! Thanks for using TableMorph!");
                    System.exit(0);
                    break;
                default:
                    System.out.println("\n❌ Invalid option. Please try again.");
            }
        }
    }

    /**
     * Generates a random wavetable and saves it as a WAV file.
     */
    private static void generateRandomWavetable() {
        try {
            Path wavetablePath = wavetableGenerator.generateRandomWavetable();
            System.out.println("\n✅ New wavetable generated!");
            System.out.println("📂 Location: " + wavetablePath.toAbsolutePath());
            System.out.println("\nTo use this wavetable in VITAL:");
            System.out.println("1. Copy the .wav file to your VITAL wavetables folder");
            System.out.println("2. In VITAL, select an oscillator");
            System.out.println("3. Click the wavetable button");
            System.out.println("4. Choose 'Import' and select the .wav file");
        } catch (IOException e) {
            System.out.println("\n❌ Error generating wavetable: " + e.getMessage());
        }
    }
    
    /**
     * Generates a morphed wavetable by combining random generation with audio samples
     * from the sounds directory.
     */
    private static void generateMorphedWavetable() {
        // Get list of WAV files in the sounds directory
        List<File> soundFiles = getSoundFiles();
        
        if (soundFiles.isEmpty()) {
            System.out.println("\n❌ No WAV files found in the 'sounds' directory.");
            System.out.println("Please add some WAV files to the 'sounds' directory first.");
            return;
        }
        
        try {
            System.out.println("\n═══ Morphed Wavetable Generation ═══");
            System.out.println("This will create a wavetable that combines random synthesis with your audio samples.");
            
            // Choose a random morph type
            System.out.println("\nSelecting random morph type...");
            int morphType = random.nextInt(4);
            
            String morphDescription;
            switch (morphType) {
                case 0:
                    morphDescription = "Frame Splice - Alternating between generated and sample frames";
                    break;
                case 1:
                    morphDescription = "Spectral Hybrid - Combining frequency components of samples and synthesis";
                    break;
                case 2:
                    morphDescription = "Progressive Morph - Gradually blending from synthesis to sample";
                    break;
                default:
                    morphDescription = "Random Chaos - Unpredictable combinations of synthesis and samples";
            }
            
            System.out.println("🧬 Using morph type: " + morphDescription);
            
            // Generate the morphed wavetable
            Path morphedPath = wavetableGenerator.generateMorphedWavetable(
                morphType, 
                selectRandomSoundFiles(soundFiles, 1 + random.nextInt(3)), // Pick 1-3 random sound files
                MORPHS_DIRECTORY
            );
            
            System.out.println("\n✅ Morphed wavetable generated!");
            System.out.println("📂 Location: " + morphedPath.toAbsolutePath());
            System.out.println("\nTo use this wavetable in VITAL:");
            System.out.println("1. Copy the .wav file to your VITAL wavetables folder");
            System.out.println("2. In VITAL, select an oscillator");
            System.out.println("3. Click the wavetable button");
            System.out.println("4. Choose 'Import' and select the .wav file");
            
        } catch (IOException e) {
            System.out.println("\n❌ Error generating morphed wavetable: " + e.getMessage());
        }
    }
    
    /**
     * Generates a batch of morphed wavetables by combining random generation with audio samples
     * from the sounds directory.
     */
    private static void generateBatchMorphedWavetables() {
        // Get list of WAV files in the sounds directory
        List<File> soundFiles = getSoundFiles();
        
        if (soundFiles.isEmpty()) {
            System.out.println("\n❌ No WAV files found in the 'sounds' directory.");
            System.out.println("Please add some WAV files to the 'sounds' directory first.");
            return;
        }
        
        System.out.println("\n═══ Batch Morphed Wavetable Generation ═══");
        System.out.println("This will generate multiple morphed wavetables at once.");
        System.out.println("Each wavetable will combine random synthesis with your audio samples.");
        
        System.out.print("\nHow many morphed wavetables do you want to generate? (1-100): ");
        int count = 0;
        try {
            count = Integer.parseInt(scanner.nextLine().trim());
            if (count < 1) {
                System.out.println("\n❌ Invalid number. Using 1 instead.");
                count = 1;
            } else if (count > 100) {
                System.out.println("\n❌ Maximum is 100. Using 100 instead.");
                count = 100;
            }
        } catch (NumberFormatException e) {
            System.out.println("\n❌ Invalid number. Using 5 as default.");
            count = 5;
        }
        
        System.out.println("\n🧬 Generating " + count + " morphed wavetables...");
        
        // Generate wavetables with different morphing techniques for maximum chaos
        for (int i = 0; i < count; i++) {
            try {
                // Use a combination of system time, index, and random component for seed
                long seed = System.currentTimeMillis() + i * 1000 + random.nextInt(10000);
                
                // Choose a random morph type
                int morphType = random.nextInt(4);
                
                // Print progress
                System.out.print("Generating morphed wavetable " + (i+1) + "/" + count + "... ");
                
                // Generate the morphed wavetable with unique seed
                Path morphedPath = wavetableGenerator.generateMorphedWavetable(
                    morphType, 
                    selectRandomSoundFiles(soundFiles, 1 + random.nextInt(3)), // Pick 1-3 random sound files
                    MORPHS_DIRECTORY,
                    seed
                );
                
                System.out.println("✅ Done!");
                System.out.println("  📂 " + morphedPath.getFileName());
                
                // Brief pause between generations for better time-based seed uniqueness
                if (i < count - 1) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }
            } catch (IOException e) {
                System.out.println("❌ Failed: " + e.getMessage());
            }
        }
        
        System.out.println("\n✅ Batch morphing complete!");
        System.out.println("📂 All morphed wavetables are saved in the 'morphs' directory");
        
        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }
    
    /**
     * Gets a list of all WAV files in the sounds directory.
     * 
     * @return List of WAV files
     */
    private static List<File> getSoundFiles() {
        List<File> wavFiles = new ArrayList<>();
        File soundsDir = new File(SOUNDS_DIRECTORY);
        
        if (soundsDir.exists() && soundsDir.isDirectory()) {
            File[] files = soundsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".wav"));
            if (files != null) {
                for (File file : files) {
                    wavFiles.add(file);
                }
            }
        }
        
        return wavFiles;
    }
    
    /**
     * Selects a specified number of random sound files from the provided list.
     * 
     * @param soundFiles List of sound files to choose from
     * @param count Number of files to select
     * @return List of selected sound files
     */
    private static List<File> selectRandomSoundFiles(List<File> soundFiles, int count) {
        List<File> selectedFiles = new ArrayList<>();
        
        // If we don't have enough files, just use what we have
        if (soundFiles.size() <= count) {
            return new ArrayList<>(soundFiles);
        }
        
        // Make a copy we can modify
        List<File> availableFiles = new ArrayList<>(soundFiles);
        
        // Select random files
        for (int i = 0; i < count && !availableFiles.isEmpty(); i++) {
            int index = random.nextInt(availableFiles.size());
            selectedFiles.add(availableFiles.get(index));
            availableFiles.remove(index);
        }
        
        return selectedFiles;
    }
    
    /**
     * Generates multiple unique random wavetables in batch mode.
     * Each wavetable is guaranteed to be different from the others by using
     * different seed values and randomization approaches.
     */
    private static void generateBatchWavetables() {
        System.out.println("\n═══ Batch Wavetable Generation ═══");
        System.out.println("This will generate multiple unique wavetables at once.");
        System.out.println("Each wavetable will have completely different characteristics.");
        
        System.out.print("\nHow many wavetables do you want to generate? (1-100): ");
        int count = 0;
        try {
            count = Integer.parseInt(scanner.nextLine().trim());
            if (count < 1) {
                System.out.println("\n❌ Invalid number. Using 1 instead.");
                count = 1;
            } else if (count > 100) {
                System.out.println("\n❌ Maximum is 100. Using 100 instead.");
                count = 100;
            }
        } catch (NumberFormatException e) {
            System.out.println("\n❌ Invalid number. Using 5 as default.");
            count = 5;
        }
        
        System.out.println("\n📦 Generating " + count + " unique wavetables...");
        
        // Generate wavetables with different seeds for maximum uniqueness
        for (int i = 0; i < count; i++) {
            try {
                // Use a combination of system time, index, and random component for seed
                long seed = System.currentTimeMillis() + i * 1000 + random.nextInt(10000);
                
                // Print progress
                System.out.print("Generating wavetable " + (i+1) + "/" + count + "... ");
                
                // Generate wavetable with unique seed
                Path wavetablePath = wavetableGenerator.generateUniqueWavetable(seed);
                
                System.out.println("✅ Done!");
                System.out.println("  📂 " + wavetablePath.getFileName());
                
                // Brief pause between generations for better time-based seed uniqueness
                if (i < count - 1) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }
            } catch (IOException e) {
                System.out.println("❌ Failed: " + e.getMessage());
            }
        }
        
        System.out.println("\n✅ Batch generation complete!");
        System.out.println("📂 All wavetables are saved in the 'wavetables' directory");
        
        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }

    /**
     * Displays information about the wavetable generation.
     */
    private static void displayWavetableInfo() {
        int frames = GeneratorConfig.getWavetableFrames();
        int samples = GeneratorConfig.getWavetableSamples();
        
        // Create a formatted string for the frame and sample counts
        String frameInfo = String.format("Each generated wavetable contains %d frames of %d samples", frames, samples);
        
        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║                    WAVETABLE INFORMATION                      ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════╣");
        System.out.println("║ " + frameInfo + getSpaces(51 - frameInfo.length()) + "║");
        System.out.println("║ each. These are designed to be compatible with the Vital      ║");
        System.out.println("║ synthesizer's wavetable format.                               ║");
        System.out.println("║                                                               ║");
        System.out.println("║ Generation Modes:                                             ║");
        System.out.println("║                                                               ║");
        System.out.println("║ • Standard Wavetables: Pure synthesis using algorithms        ║");
        System.out.println("║   Saved in the 'wavetables' directory                         ║");
        System.out.println("║                                                               ║");
        System.out.println("║ • Morphed Wavetables: Combines synthesis with audio samples   ║");
        System.out.println("║   Place audio samples in the 'sounds' directory               ║");
        System.out.println("║   Morphed wavetables are saved in the 'morphs' directory      ║");
        System.out.println("║                                                               ║");
        System.out.println("║ The generator creates a variety of different waveform types:  ║");
        System.out.println("║  • Sine waves with phase variations                           ║");
        System.out.println("║  • Sawtooth waves with varying harmonics                      ║");
        System.out.println("║  • Square waves with varying pulse widths                     ║");
        System.out.println("║  • Triangle waves with varying slopes                         ║");
        System.out.println("║  • Noise with varying degrees of smoothing                    ║");
        System.out.println("║  • FM synthesis-like waveforms                                ║");
        System.out.println("║  • Ring modulation waveforms                                  ║");
        System.out.println("║  • Additive synthesis with inharmonic partials                ║");
        System.out.println("║  • Complex modulation with multiple carriers                  ║");
        System.out.println("║  • Spectral morphing waveforms                                ║");
        System.out.println("║                                                               ║");

        // Format directory paths to ensure they fit within the box width
        String wavetableDirPath = wavetableGenerator.getWavetableDirectoryPath();
        String soundsDirPath = new File(SOUNDS_DIRECTORY).getAbsolutePath();
        String morphsDirPath = new File(MORPHS_DIRECTORY).getAbsolutePath();

        // Ensure formatting will work by limiting path length if needed
        int maxPathLength = 50; // Maximum characters that can fit in the line
        if (wavetableDirPath.length() > maxPathLength) {
            wavetableDirPath = "..." + wavetableDirPath.substring(wavetableDirPath.length() - maxPathLength + 3);
        }
        if (soundsDirPath.length() > maxPathLength) {
            soundsDirPath = "..." + soundsDirPath.substring(soundsDirPath.length() - maxPathLength + 3);
        }
        if (morphsDirPath.length() > maxPathLength) {
            morphsDirPath = "..." + morphsDirPath.substring(morphsDirPath.length() - maxPathLength + 3);
        }

        System.out.println("║ Standard directory: " + wavetableDirPath + getSpaces(maxPathLength - wavetableDirPath.length()) + " ║");
        System.out.println("║ Samples directory: " + soundsDirPath + getSpaces(maxPathLength - soundsDirPath.length() + 1) + " ║");
        System.out.println("║ Morphs directory: " + morphsDirPath + getSpaces(maxPathLength - morphsDirPath.length() + 2) + " ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        
        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }
    
    /**
     * Helper method to generate spaces for proper alignment
     */
    private static String getSpaces(int count) {
        StringBuilder spaces = new StringBuilder();
        for (int i = 0; i < count; i++) {
            spaces.append(" ");
        }
        return spaces.toString();
    }
    
    /**
     * Displays and handles the configuration menu.
     */
    private static void displayConfigMenu() {
        while (true) {
            System.out.println("\n╔════════════════════════════════════╗");
            System.out.println("║         CONFIGURATION              ║");
            System.out.println("╠════════════════════════════════════╣");
            System.out.println("║  1. Wavetable Settings            ║");
            System.out.println("║  2. Morphing Settings             ║");
            System.out.println("║  3. Vital Integration             ║");
            System.out.println("║  4. Back to Main Menu             ║");
            System.out.println("╚════════════════════════════════════╝");
            
            System.out.print("Enter your choice > ");
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    configureWavetableSettings();
                    break;
                case "2":
                    configureMorphSettings();
                    break;
                case "3":
                    configureVitalIntegration();
                    break;
                case "4":
                    return;
                default:
                    System.out.println("\n❌ Invalid choice. Please try again.");
            }
        }
    }

    /**
     * Configures Vital integration settings.
     */
    private static void configureVitalIntegration() {
        System.out.println("\n╔════════════════════════════════════╗");
        System.out.println("║       VITAL INTEGRATION           ║");
        System.out.println("╠════════════════════════════════════╣");
        System.out.println("║  1. Enable/Disable Integration    ║");
        System.out.println("║  2. Set Vital Wavetables Path     ║");
        System.out.println("║  3. Reset to Default Directory    ║");
        System.out.println("║  4. Back to Configuration Menu    ║");
        System.out.println("╚════════════════════════════════════╝");
            
        // Check if OS is supported
        if (!GeneratorConfig.isOSSupported()) {
            System.out.println("\n❌ Your operating system is not currently supported for Vital integration.");
            System.out.println("Supported systems: macOS, Windows");
            System.out.println("\nPress Enter to continue...");
            scanner.nextLine();
            return;
        }
            
        while (true) {
            System.out.println("\nCurrent Settings:");
            System.out.println("1. Save to Vital's Directory: " + 
                (GeneratorConfig.getSaveToVital() ? "Enabled ✓" : "Disabled ✗"));
            System.out.println("2. Vital's Wavetables Directory: " + GeneratorConfig.getVitalWavetablesDirectory());
            
            System.out.print("\nEnter your choice (1-4) > ");
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    boolean currentValue = GeneratorConfig.getSaveToVital();
                    GeneratorConfig.setSaveToVital(!currentValue);
                    System.out.println("✓ Integration " + (!currentValue ? "enabled" : "disabled"));
                    break;
                    
                case "2":
                    System.out.println("\nEnter the path to Vital's wavetables directory:");
                    System.out.println("(Leave blank to cancel)");
                    System.out.print("> ");
                    String newPath = scanner.nextLine().trim();
                    
                    if (!newPath.isEmpty()) {
                        File directory = new File(newPath);
                        if (directory.exists() && directory.isDirectory()) {
                            GeneratorConfig.setVitalWavetablesDirectory(newPath);
                            System.out.println("✓ Path updated");
                        } else {
                            System.out.println("❌ Invalid directory path. Directory does not exist.");
                        }
                    }
                    break;
                    
                case "3":
                    String defaultDir = GeneratorConfig.getDefaultVitalDirectory();
                    GeneratorConfig.setVitalWavetablesDirectory(defaultDir);
                    System.out.println("✓ Reset to default directory: " + defaultDir);
                    break;
                    
                case "4":
                    return;
                    
                default:
                    System.out.println("\n❌ Invalid choice. Please try again.");
            }
        }
    }

    /**
     * Configures wavetable generation settings.
     */
    private static void configureWavetableSettings() {
        while (true) {
            System.out.println("\n╔════════════════════════════════════╗");
            System.out.println("║       WAVETABLE SETTINGS          ║");
            System.out.println("╠════════════════════════════════════╣");
            System.out.println("║  1. Frame Count                   ║");
            System.out.println("║  2. Sample Count                  ║");
            System.out.println("║  3. Back to Configuration Menu    ║");
            System.out.println("╚════════════════════════════════════╝");
            
            System.out.println("\nCurrent Settings:");
            System.out.println("1. Frame Count: " + GeneratorConfig.getWavetableFrames());
            System.out.println("2. Sample Count: " + GeneratorConfig.getWavetableSamples());
            
            System.out.print("\nEnter your choice (1-3) > ");
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    System.out.println("\nThe frame count determines how many different waveforms are in each wavetable.");
                    System.out.println("Higher values create more complex morphing wavetables but result in larger files.");
                    System.out.println("Common values: 16, 32, 64, 128");
                    System.out.print("\nEnter new frame count (4-256): ");
                    try {
                        int frames = Integer.parseInt(scanner.nextLine().trim());
                        if (frames >= 4 && frames <= 256) {
                            GeneratorConfig.setWavetableFrames(frames);
                            System.out.println("✓ Frame count updated to " + frames);
                        } else {
                            System.out.println("❌ Invalid value. Frame count must be between 4 and 256.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("❌ Invalid input. Please enter a number.");
                    }
                    break;
                    
                case "2":
                    System.out.println("\nThe sample count determines the resolution of each frame in the wavetable.");
                    System.out.println("Higher values create more detailed waveforms but result in larger files.");
                    System.out.println("Common values: 1024, 2048, 4096");
                    System.out.print("\nEnter new sample count (256-8192, must be a power of 2): ");
                    try {
                        int samples = Integer.parseInt(scanner.nextLine().trim());
                        if (samples >= 256 && samples <= 8192 && (samples & (samples - 1)) == 0) {
                            GeneratorConfig.setWavetableSamples(samples);
                            System.out.println("✓ Sample count updated to " + samples);
                        } else {
                            System.out.println("❌ Invalid value. Sample count must be a power of 2 between 256 and 8192.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("❌ Invalid input. Please enter a number.");
                    }
                    break;
                    
                case "3":
                    return;
                    
                default:
                    System.out.println("\n❌ Invalid choice. Please try again.");
            }
        }
    }

    /**
     * Configures morphing settings.
     */
    private static void configureMorphSettings() {
        while (true) {
            System.out.println("\n╔════════════════════════════════════╗");
            System.out.println("║       MORPHING SETTINGS           ║");
            System.out.println("╠════════════════════════════════════╣");
            System.out.println("║  1. Max Morph Samples             ║");
            System.out.println("║  2. Full Sample Probability       ║");
            System.out.println("║  3. Back to Configuration Menu    ║");
            System.out.println("╚════════════════════════════════════╝");
            
            System.out.println("\nCurrent Settings:");
            System.out.println("1. Max Morph Samples: " + GeneratorConfig.getMaxMorphSamples());
            System.out.println("2. Full Sample Probability: " + GeneratorConfig.getFullSampleProbability() + "%");
            
            System.out.print("\nEnter your choice (1-3) > ");
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    System.out.println("\nThis setting controls how many sample files from the sounds directory");
                    System.out.println("will be used during morphing (maximum). A higher number will include");
                    System.out.println("more variety but can slow down processing time.");
                    System.out.print("\nEnter new value (1-50): ");
                    try {
                        int maxSamples = Integer.parseInt(scanner.nextLine().trim());
                        if (maxSamples >= 1 && maxSamples <= 50) {
                            GeneratorConfig.setMaxMorphSamples(maxSamples);
                            System.out.println("✓ Max morph samples updated to " + maxSamples);
                        } else {
                            System.out.println("❌ Invalid value. Must be between 1 and 50.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("❌ Invalid input. Please enter a number.");
                    }
                    break;
                    
                case "2":
                    System.out.println("\nThis setting controls how often complete samples will be used as-is");
                    System.out.println("or fully incorporated into morphed wavetables, rather than just using");
                    System.out.println("small sections. A higher value means complete samples will be used more often.");
                    System.out.print("\nEnter new value (0-100%): ");
                    try {
                        int probability = Integer.parseInt(scanner.nextLine().trim());
                        if (probability >= 0 && probability <= 100) {
                            GeneratorConfig.setFullSampleProbability(probability / 100.0);
                            System.out.println("✓ Full sample probability updated to " + probability + "%");
                        } else {
                            System.out.println("❌ Invalid value. Must be between 0 and 100.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("❌ Invalid input. Please enter a number.");
                    }
                    break;
                    
                case "3":
                    return;
                    
                default:
                    System.out.println("\n❌ Invalid choice. Please try again.");
            }
        }
    }
} 