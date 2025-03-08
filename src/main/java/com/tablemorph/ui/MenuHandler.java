package com.tablemorph.ui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.tablemorph.config.GeneratorConfig;
import com.tablemorph.model.MorphType;
import com.tablemorph.model.WaveformType;
import com.tablemorph.service.ServiceFactory;
import com.tablemorph.service.MultiFrameWavetableGenerator;
import com.tablemorph.service.SingleCycleWaveformGenerator;
import com.tablemorph.service.WavetableMorphService;
import com.tablemorph.util.DirectoryUtil;

/**
 * Handles menu interaction and dispatches commands to the appropriate services.
 */
public class MenuHandler {
    private static final String SOUNDS_DIRECTORY = "sounds";
    private static final String MORPHS_DIRECTORY = "morphs";
    
    private final ServiceFactory serviceFactory;
    
    public MenuHandler() {
        this.serviceFactory = ServiceFactory.getInstance();
    }
    
    /**
     * Displays the main menu and handles user interaction.
     */
    public void displayMenu() {
        while (true) {
            TextUI.displayHeader("TABLEMORPH MENU");
            
            String[] menuItems = {
                "1. Generate Wavetable",
                "2. Morph with Samples",
                "3. Batch Generate Wavetables",
                "4. Batch Morph Wavetables",
                "5. Generate Single-Cycle Wave",
                "6. Batch Generate Single-Cycles",
                "7. Generate Experimental Wave",
                "8. Batch Generate Experimental",
                "9. Wavetable Information",
                "10. Configuration",
                "0. Exit"
            };
            
            for (String item : menuItems) {
                System.out.println("║  " + item + TextUI.getSpaces(30 - item.length()) + "║");
            }
            
            System.out.println("╚════════════════════════════════════╝");
            
            System.out.print("\nEnter your choice > ");
            String choice = TextUI.readLine();
            
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
                    generateSingleCycleWavetable();
                    break;
                case "6":
                    generateBatchSingleCycleWavetables();
                    break;
                case "7":
                    generateExperimentalWavetable();
                    break;
                case "8":
                    generateBatchExperimentalWavetables();
                    break;
                case "9":
                    displayWavetableInfo();
                    break;
                case "10":
                    ConfigurationMenuHandler configMenu = new ConfigurationMenuHandler();
                    configMenu.displayConfigMenu();
                    break;
                case "0":
                    System.out.println("\nGoodbye! Thanks for using TableMorph!");
                    System.exit(0);
                    break;
                default:
                    System.out.println("\n[ERROR] Invalid option. Please try again.");
            }
        }
    }
    
    /**
     * Generates a random multi-frame wavetable.
     */
    private void generateRandomWavetable() {
        try {
            // Create directory if it doesn't exist
            DirectoryUtil.createWavetableDirectory();
            
            // Generate the wavetable
            MultiFrameWavetableGenerator generator = serviceFactory.getMultiFrameGenerator();
            Path wavetablePath = generator.generateRandomWavetable();
            
            System.out.println("\n[SUCCESS] New wavetable generated!");
            System.out.println("[FILE] Location: " + wavetablePath.toAbsolutePath());
            
            TextUI.waitForEnter(null);
        } catch (IOException e) {
            System.out.println("\n[ERROR] Error generating wavetable: " + e.getMessage());
            TextUI.waitForEnter(null);
        }
    }
    
    /**
     * Generates a morphed wavetable by combining generated waveforms with audio samples.
     */
    private void generateMorphedWavetable() {
        try {
            // Create required directories
            DirectoryUtil.createDirectoryIfNotExists(SOUNDS_DIRECTORY);
            DirectoryUtil.createDirectoryIfNotExists(MORPHS_DIRECTORY);
            
            // Get sound files
            List<File> soundFiles = getSoundFiles();
            if (soundFiles.isEmpty()) {
                System.out.println("\n[ERROR] No sound files found in the 'sounds' directory.");
                System.out.println("Please add some .wav files to the 'sounds' directory and try again.");
                TextUI.waitForEnter(null);
                return;
            }
            
            // Display morph type options
            TextUI.displayHeader("MORPH TYPES");
            for (MorphType type : MorphType.values()) {
                System.out.println(type.getId() + ". " + type.getName() + " - " + type.getDescription());
            }
            
            // Get morph type
            int morphTypeId = TextUI.readInt("\nSelect morph type (1-" + MorphType.values().length + "): ", 
                                           1, MorphType.values().length, 1);
            MorphType morphType = MorphType.getById(morphTypeId);
            
            System.out.println("\nMorphing with " + soundFiles.size() + " sound files...");
            
            // Generate the wavetable
            WavetableMorphService morphService = serviceFactory.getMorphService();
            Path wavetablePath = morphService.generateMorphedWavetable(morphType, soundFiles);
            
            System.out.println("\n[SUCCESS] New morphed wavetable generated!");
            System.out.println("[FILE] Location: " + wavetablePath.toAbsolutePath());
            
            TextUI.waitForEnter(null);
        } catch (IOException e) {
            System.out.println("\n[ERROR] Error generating morphed wavetable: " + e.getMessage());
            TextUI.waitForEnter(null);
        }
    }
    
    /**
     * Batch generates multiple morphed wavetables.
     */
    private void generateBatchMorphedWavetables() {
        try {
            // Create required directories
            DirectoryUtil.createDirectoryIfNotExists(SOUNDS_DIRECTORY);
            DirectoryUtil.createDirectoryIfNotExists(MORPHS_DIRECTORY);
            
            // Get sound files
            List<File> soundFiles = getSoundFiles();
            if (soundFiles.isEmpty()) {
                System.out.println("\n[ERROR] No sound files found in the 'sounds' directory.");
                System.out.println("Please add some .wav files to the 'sounds' directory and try again.");
                TextUI.waitForEnter(null);
                return;
            }
            
            // Get number of wavetables to generate
            int count = TextUI.readInt("\nHow many morphed wavetables to generate? (1-100): ", 1, 100, 5);
            
            // Get morph types to use
            TextUI.displayHeader("MORPH TYPES");
            System.out.println("Select which morph types to include:");
            
            List<MorphType> selectedTypes = new ArrayList<>();
            for (MorphType type : MorphType.values()) {
                boolean include = TextUI.readBoolean(type.getId() + ". " + type.getName() + 
                                                  " - Include this type?", true);
                if (include) {
                    selectedTypes.add(type);
                }
            }
            
            if (selectedTypes.isEmpty()) {
                System.out.println("\n[ERROR] No morph types selected. Using default (Blend).");
                selectedTypes.add(MorphType.BLEND);
            }
            
            System.out.println("\nGenerating " + count + " morphed wavetables...");
            
            // Generate the wavetables
            WavetableMorphService morphService = serviceFactory.getMorphService();
            List<Path> generatedFiles = morphService.batchGenerateMorphedWavetables(
                selectedTypes, soundFiles, count);
            
            System.out.println("\n[SUCCESS] Batch generation complete!");
            System.out.println("[FILE] All morphed wavetables are saved in the '" + MORPHS_DIRECTORY + "' directory");
            
            TextUI.waitForEnter(null);
        } catch (IOException e) {
            System.out.println("\n[ERROR] Error during batch generation: " + e.getMessage());
            TextUI.waitForEnter(null);
        }
    }
    
    /**
     * Batch generates multiple wavetables.
     */
    private void generateBatchWavetables() {
        // Create directory if it doesn't exist
        DirectoryUtil.createWavetableDirectory();
        
        // Get number of wavetables to generate
        int count = TextUI.readInt("\nHow many wavetables to generate? (1-100): ", 1, 100, 5);
        
        System.out.println("\nGenerating " + count + " wavetables...");
        
        try {
            // Generate the wavetables
            MultiFrameWavetableGenerator generator = serviceFactory.getMultiFrameGenerator();
            List<Path> generatedFiles = generator.batchGenerateWavetables(count);
            
            System.out.println("\n[SUCCESS] Batch generation complete!");
            System.out.println("[FILE] All wavetables are saved in the 'wavetables' directory");
            
            TextUI.waitForEnter(null);
        } catch (IOException e) {
            System.out.println("\n[ERROR] Error during batch generation: " + e.getMessage());
            TextUI.waitForEnter(null);
        }
    }
    
    /**
     * Generates a single-cycle wavetable.
     */
    private void generateSingleCycleWavetable() {
        try {
            SingleCycleWaveformGenerator generator = serviceFactory.getSingleCycleGenerator();
            
            // Create directory if it doesn't exist
            DirectoryUtil.createSingleCycleDirectory();
            
            // Display waveform type options
            TextUI.displayHeader("WAVEFORM TYPES");
            
            int maxOption = WaveformType.EXPERIMENTAL.getId();
            for (int i = 0; i <= maxOption; i++) {
                WaveformType type = WaveformType.getById(i);
                System.out.println(i + ". " + type.getName());
            }
            
            // Get waveform type
            int waveformTypeId = TextUI.readInt("\nSelect waveform type (0-" + maxOption + "): ", 
                                              0, maxOption, 0);
            
            // Generate the wavetable
            Path wavetablePath = generator.generateSingleCycleWavetable(
                waveformTypeId, System.currentTimeMillis());
            
            System.out.println("\n[SUCCESS] New single-cycle wavetable generated!");
            System.out.println("[FILE] Location: " + wavetablePath.toAbsolutePath());
            
            TextUI.waitForEnter(null);
        } catch (IOException e) {
            System.out.println("\n[ERROR] Error generating single-cycle wavetable: " + e.getMessage());
            TextUI.waitForEnter(null);
        }
    }
    
    /**
     * Batch generates multiple single-cycle wavetables.
     */
    private void generateBatchSingleCycleWavetables() {
        // Create directory if it doesn't exist
        DirectoryUtil.createSingleCycleDirectory();
        
        // Get number of wavetables to generate
        int count = TextUI.readInt("\nHow many single-cycle wavetables to generate? (1-100): ", 1, 100, 5);
        
        System.out.println("\nGenerating " + count + " single-cycle wavetables...");
        
        try {
            SingleCycleWaveformGenerator generator = serviceFactory.getSingleCycleGenerator();
            List<Path> generatedFiles = generator.batchGenerateSingleCycleWavetables(count);
            
            System.out.println("\n[SUCCESS] Batch generation complete!");
            System.out.println("[FILE] All single-cycle wavetables are saved in the '" + 
                GeneratorConfig.getSingleCycleDirectory() + "' directory");
            
            TextUI.waitForEnter(null);
        } catch (IOException e) {
            System.out.println("\n[ERROR] Error during batch generation: " + e.getMessage());
            TextUI.waitForEnter(null);
        }
    }
    
    /**
     * Generates an experimental wavetable.
     */
    private void generateExperimentalWavetable() {
        try {
            SingleCycleWaveformGenerator generator = serviceFactory.getSingleCycleGenerator();
            
            // Create directory if it doesn't exist
            DirectoryUtil.createSingleCycleDirectory();
            
            // Generate the experimental wavetable
            Path wavetablePath = generator.generateSingleCycleWavetable(
                WaveformType.EXPERIMENTAL.getId(), System.currentTimeMillis());
                
            System.out.println("\n[SUCCESS] New experimental wavetable generated!");
            System.out.println("[FILE] Location: " + wavetablePath.toAbsolutePath());
            
            TextUI.waitForEnter(null);
        } catch (IOException e) {
            System.out.println("\n[ERROR] Error generating experimental wavetable: " + e.getMessage());
            TextUI.waitForEnter(null);
        }
    }
    
    /**
     * Batch generates multiple experimental wavetables.
     */
    private void generateBatchExperimentalWavetables() {
        // Create directory if it doesn't exist
        DirectoryUtil.createSingleCycleDirectory();
        
        // Get number of wavetables to generate
        int count = TextUI.readInt("\nHow many experimental wavetables to generate? (1-100): ", 1, 100, 5);
        
        System.out.println("\nGenerating " + count + " experimental wavetables...");
        
        try {
            SingleCycleWaveformGenerator generator = serviceFactory.getSingleCycleGenerator();
            List<Path> generatedFiles = generator.batchGenerateExperimentalWavetables(count);
            
            System.out.println("\n[SUCCESS] Batch generation complete!");
            System.out.println("[FILE] All experimental wavetables are saved in the '" + 
                GeneratorConfig.getSingleCycleDirectory() + "' directory");
            
            if (GeneratorConfig.getSaveToVital() && GeneratorConfig.isOSSupported()) {
                System.out.println("[VITAL] Also saved to Vital's wavetables directory: " + 
                    GeneratorConfig.getVitalWavetablesDirectory());
            }
            
            TextUI.waitForEnter(null);
        } catch (IOException e) {
            System.out.println("\n[ERROR] Error during batch generation: " + e.getMessage());
            TextUI.waitForEnter(null);
        }
    }
    
    /**
     * Displays information about the wavetable generation.
     */
    private void displayWavetableInfo() {
        int frames = GeneratorConfig.getWavetableFrames();
        int samples = GeneratorConfig.getWavetableSamples();
        int singleCycleSamples = GeneratorConfig.getSingleCycleSamples();
        
        String wavetableDirPath = DirectoryUtil.getWavetableDirectoryPath();
        String singleCycleDirPath = DirectoryUtil.getSingleCycleDirectoryPath();
        String soundsDirPath = DirectoryUtil.getSoundsDirectoryPath();
        String morphsDirPath = DirectoryUtil.getMorphsDirectoryPath();
        
        // Find maximum path length for alignment
        int maxPathLength = Math.max(
            Math.max(wavetableDirPath.length(), singleCycleDirPath.length()),
            Math.max(soundsDirPath.length(), morphsDirPath.length())
        );
        
        TextUI.displayHeader("WAVETABLE INFORMATION");
        
        String[] info = {
            "Each generated wavetable contains " + frames + " frames of " + samples + " samples",
            "each. These are designed to be compatible with the Vital",
            "synthesizer's wavetable format.",
            "",
            "Single-cycle wavetables contain 1 frame of " + singleCycleSamples + " samples",
            "and work great as oscillators in Vital.",
            "",
            "Experimental wavetables use advanced algorithms to create",
            "unique and evolving timbres for your sounds.",
            "",
            "Current Directories:",
            "Wavetables directory: " + wavetableDirPath,
            "Single-cycles directory: " + singleCycleDirPath,
            "Samples directory: " + soundsDirPath,
            "Morphs directory: " + morphsDirPath
        };
        
        for (String line : info) {
            System.out.println("║ " + line + TextUI.getSpaces(47 - line.length()) + " ║");
        }
        
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        
        TextUI.waitForEnter(null);
    }
    
    /**
     * Gets the list of sound files from the sounds directory.
     * 
     * @return List of sound files
     */
    private List<File> getSoundFiles() {
        File soundsDir = new File(SOUNDS_DIRECTORY);
        if (!soundsDir.exists() || !soundsDir.isDirectory()) {
            return new ArrayList<>();
        }
        
        return Arrays.asList(soundsDir.listFiles(file -> 
            file.isFile() && file.getName().toLowerCase().endsWith(".wav")));
    }
} 