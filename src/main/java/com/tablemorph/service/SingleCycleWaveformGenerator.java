package com.tablemorph.service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.tablemorph.config.GeneratorConfig;
import com.tablemorph.model.WaveformType;
import com.tablemorph.model.Wavetable;
import com.tablemorph.util.DirectoryUtil;
import com.tablemorph.util.WavFileUtil;
import com.tablemorph.util.WaveformUtil;

/**
 * Service class for generating single-cycle wavetables.
 */
public class SingleCycleWaveformGenerator {
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    private final Random random = new Random();
    private final ExperimentalWaveformGenerator experimentalGenerator;
    
    public SingleCycleWaveformGenerator() {
        this.experimentalGenerator = new ExperimentalWaveformGenerator();
    }
    
    /**
     * Generates a random single-cycle wavetable.
     * 
     * @return The path to the generated wavetable
     * @throws IOException If an error occurs during generation
     */
    public Path generateRandomSingleCycleWavetable() throws IOException {
        // Include experimental waveforms in the random selection
        int maxType = WaveformType.EXPERIMENTAL.getId() + 1;
        return generateSingleCycleWavetable(random.nextInt(maxType), System.currentTimeMillis());
    }
    
    /**
     * Generates a single-cycle wavetable with the specified type and seed.
     * 
     * @param waveformTypeId The ID of the waveform type to generate
     * @param seed The random seed for generation
     * @return The path to the generated wavetable
     * @throws IOException If an error occurs during generation
     */
    public Path generateSingleCycleWavetable(int waveformTypeId, long seed) throws IOException {
        DirectoryUtil.createSingleCycleDirectory();
        
        // Create seeded random generator for consistent results with the same seed
        Random seededRandom = new Random(seed);
        WaveformType waveformType = WaveformType.getById(waveformTypeId);
        
        // Generate unique filename
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String waveformName = waveformType.getName();
        String filename = "singlecycle_" + waveformName + "_" + timestamp + "_" + 
            Math.abs(seededRandom.nextInt(10000)) + ".wav";
        
        String singleCycleDir = GeneratorConfig.getSingleCycleDirectory();
        Path outputPath = Paths.get(singleCycleDir, filename);
        
        // Generate wavetable and save to file
        Wavetable wavetable = generateSingleCycleWavetable(waveformType, seededRandom);
        wavetable.setFilePath(outputPath);
        WavFileUtil.saveToFile(wavetable.getPcmData(), outputPath);
        
        // If enabled, also save to Vital's directory
        Path vitalPath = DirectoryUtil.getVitalPath(filename);
        if (vitalPath != null) {
            try {
                WavFileUtil.saveToFile(wavetable.getPcmData(), vitalPath);
                System.out.println("[OK] Also saved to Vital's wavetables directory: " + vitalPath);
            } catch (IOException e) {
                System.out.println("[ERROR] Could not save to Vital's directory: " + e.getMessage());
            }
        }
        
        return outputPath;
    }
    
    /**
     * Batch generates multiple single-cycle wavetables.
     * 
     * @param count The number of wavetables to generate
     * @return List of paths to the generated wavetables
     * @throws IOException If an error occurs during generation
     */
    public List<Path> batchGenerateSingleCycleWavetables(int count) throws IOException {
        List<Path> generatedFiles = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            // Generate a unique seed based on current time and iteration
            long seed = System.currentTimeMillis() + i;
            
            // Choose a random waveform type for each iteration
            int waveformType = random.nextInt(WaveformType.CUSTOM.getId());
            
            // Generate the wavetable
            Path path = generateSingleCycleWavetable(waveformType, seed);
            generatedFiles.add(path);
            
            // Brief pause between generations for better time-based seed uniqueness
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        return generatedFiles;
    }
    
    /**
     * Batch generates multiple experimental wavetables.
     * 
     * @param count The number of experimental wavetables to generate
     * @return List of paths to the generated wavetables
     * @throws IOException If an error occurs during generation
     */
    public List<Path> batchGenerateExperimentalWavetables(int count) throws IOException {
        List<Path> generatedFiles = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            // Generate a unique seed based on current time and iteration
            long seed = System.currentTimeMillis() + i;
            
            // Generate the experimental wavetable
            Path path = generateSingleCycleWavetable(WaveformType.EXPERIMENTAL.getId(), seed);
            generatedFiles.add(path);
            
            // Output progress
            System.out.println("Generated experimental wavetable " + (i+1) + "/" + count + ": " + path.getFileName());
            
            // Brief pause between generations for better time-based seed uniqueness
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        return generatedFiles;
    }
    
    /**
     * Generates a single-cycle wavetable object.
     * 
     * @param waveformType The type of waveform to generate
     * @param randomGenerator The random number generator to use
     * @return The generated wavetable
     */
    private Wavetable generateSingleCycleWavetable(WaveformType waveformType, Random randomGenerator) {
        int sampleCount = GeneratorConfig.getSingleCycleSamples();
        float[] waveform = generateSingleCycleWaveform(waveformType, sampleCount, randomGenerator);
        
        // Ensure proper normalization and smoothing
        WaveformUtil.normalize(waveform);
        WaveformUtil.smoothLoopPoints(waveform);
        
        // Convert to PCM data
        byte[] pcmData = WavFileUtil.convertToPCM(waveform);
        
        return new Wavetable(waveformType, sampleCount, pcmData);
    }
    
    /**
     * Generates the waveform data for a single-cycle wavetable.
     * 
     * @param waveformType The type of waveform to generate
     * @param sampleCount The number of samples in the waveform
     * @param randomGenerator The random number generator to use
     * @return The generated waveform data
     */
    private float[] generateSingleCycleWaveform(WaveformType waveformType, int sampleCount, Random randomGenerator) {
        // Check if we should use an experimental waveform instead
        if (waveformType != WaveformType.EXPERIMENTAL && 
            randomGenerator.nextDouble() < GeneratorConfig.getExperimentalWaveformProbability()) {
            // Randomly replace with an experimental waveform
            return generateExperimentalWaveform(sampleCount, randomGenerator);
        }
        
        float[] waveform = new float[sampleCount];
        
        switch (waveformType) {
            case SINE:
                generateSineWaveform(waveform, sampleCount, randomGenerator);
                break;
                
            case TRIANGLE:
                generateTriangleWaveform(waveform, sampleCount, randomGenerator);
                break;
                
            case SAW:
                generateSawWaveform(waveform, sampleCount, randomGenerator);
                break;
                
            case SQUARE:
                generateSquareWaveform(waveform, sampleCount, randomGenerator);
                break;
                
            case NOISE:
                generateNoiseWaveform(waveform, sampleCount, randomGenerator);
                break;
                
            case FM:
                generateFMWaveform(waveform, sampleCount, randomGenerator);
                break;
                
            case ADDITIVE:
                generateAdditiveWaveform(waveform, sampleCount, randomGenerator);
                break;
                
            case FORMANT:
                generateFormantWaveform(waveform, sampleCount, randomGenerator);
                break;
                
            case EXPERIMENTAL:
                return generateExperimentalWaveform(sampleCount, randomGenerator);
                
            default:
                // Default to sine wave if unknown type
                generateSineWaveform(waveform, sampleCount, randomGenerator);
                break;
        }
        
        return waveform;
    }
    
    /**
     * Generates an experimental waveform.
     */
    private float[] generateExperimentalWaveform(int sampleCount, Random randomGenerator) {
        // Create a new instance with the provided random seed
        ExperimentalWaveformGenerator generator = new ExperimentalWaveformGenerator();
        // The ExperimentalWaveformGenerator uses its own internal Random instance
        
        // Get the experimental waveform data
        double[] waveformData = experimentalGenerator.generateExperimentalWaveform(sampleCount);
        
        // Convert double array to float array
        float[] waveform = new float[sampleCount];
        for (int i = 0; i < sampleCount; i++) {
            waveform[i] = (float) waveformData[i];
        }
        
        return waveform;
    }
    
    /**
     * Generates a sine waveform.
     */
    private void generateSineWaveform(float[] waveform, int sampleCount, Random randomGenerator) {
        // Pure sine wave
        for (int i = 0; i < sampleCount; i++) {
            double phase = (double) i / sampleCount;
            waveform[i] = (float) Math.sin(2 * Math.PI * phase);
        }
        
        // Apply subtle phase modulation with 50% probability
        if (randomGenerator.nextDouble() < 0.5) {
            float modDepth = 0.05f + randomGenerator.nextFloat() * 0.1f; // 0.05 - 0.15
            float modRate = 1.0f + randomGenerator.nextInt(3); // 1-3x
            
            for (int i = 0; i < sampleCount; i++) {
                double phase = (double) i / sampleCount;
                // Add phase modulation
                double modPhase = phase + modDepth * Math.sin(2 * Math.PI * phase * modRate);
                // Wrap phase to [0,1]
                modPhase = modPhase - Math.floor(modPhase);
                // Generate sine with modulated phase
                waveform[i] = (float) Math.sin(2 * Math.PI * modPhase);
            }
        }
    }
    
    /**
     * Generates a triangle waveform.
     */
    private void generateTriangleWaveform(float[] waveform, int sampleCount, Random randomGenerator) {
        // Determine if we want some asymmetry
        boolean asymmetric = randomGenerator.nextDouble() < 0.4; // 40% chance of asymmetry
        float asymmetry = asymmetric ? 0.2f + randomGenerator.nextFloat() * 0.6f : 0.5f; // 0.2-0.8 or 0.5
        
        for (int i = 0; i < sampleCount; i++) {
            float phase = (float) i / sampleCount;
            
            if (phase < asymmetry) {
                // Rising edge
                waveform[i] = -1.0f + 2.0f * (phase / asymmetry);
            } else {
                // Falling edge
                waveform[i] = 1.0f - 2.0f * ((phase - asymmetry) / (1.0f - asymmetry));
            }
        }
        
        // Occasionally smooth the triangle slightly
        if (randomGenerator.nextDouble() < 0.3) {
            WaveformUtil.smooth(waveform, 1 + randomGenerator.nextInt(3));
        }
    }
    
    /**
     * Generates a sawtooth waveform.
     */
    private void generateSawWaveform(float[] waveform, int sampleCount, Random randomGenerator) {
        // Reverse sawtooth with 25% probability
        boolean reverseDirection = randomGenerator.nextDouble() < 0.25;
        
        if (!reverseDirection) {
            // Standard sawtooth
            for (int i = 0; i < sampleCount; i++) {
                waveform[i] = 2 * ((float) i / sampleCount) - 1;
            }
        } else {
            // Reverse sawtooth
            for (int i = 0; i < sampleCount; i++) {
                waveform[i] = 1 - 2 * ((float) i / sampleCount);
            }
        }
    }
    
    /**
     * Generates a square waveform.
     */
    private void generateSquareWaveform(float[] waveform, int sampleCount, Random randomGenerator) {
        // Randomize pulse width
        float pulseWidth = 0.3f + randomGenerator.nextFloat() * 0.4f; // 30-70%
        
        for (int i = 0; i < sampleCount; i++) {
            float phase = (float) i / sampleCount;
            waveform[i] = phase < pulseWidth ? 1.0f : -1.0f;
        }
        
        // Occasionally apply a tiny bit of smoothing to reduce aliasing
        if (randomGenerator.nextDouble() < 0.4) {
            WaveformUtil.smooth(waveform, 1); // Just a tiny bit
        }
    }
    
    /**
     * Generates a noise waveform.
     */
    private void generateNoiseWaveform(float[] waveform, int sampleCount, Random randomGenerator) {
        // Generate pure random noise
        for (int i = 0; i < sampleCount; i++) {
            waveform[i] = randomGenerator.nextFloat() * 2 - 1;
        }
        
        // Apply some filtering for more colored noise
        int filterAmount = randomGenerator.nextInt(3); // 0 = white, 1 = pink-ish, 2 = darker
        
        if (filterAmount > 0) {
            // Simple lowpass filter passes to darken the noise
            for (int p = 0; p < filterAmount; p++) {
                WaveformUtil.smooth(waveform, 1 + randomGenerator.nextInt(3));
            }
            
            // Re-normalize after filtering
            WaveformUtil.normalize(waveform);
        }
    }
    
    /**
     * Generates an FM waveform.
     */
    private void generateFMWaveform(float[] waveform, int sampleCount, Random randomGenerator) {
        // FM parameters
        float carrierFreq = 1.0f; // Base frequency
        float modulatorFreq = 1.0f + randomGenerator.nextInt(5); // 1-5x carrier frequency
        float modulationIndex = 0.5f + randomGenerator.nextFloat() * 4.5f; // Modulation amount
        
        for (int i = 0; i < sampleCount; i++) {
            float phase = (float) i / sampleCount;
            
            // Calculate modulator output
            float modOutput = (float) Math.sin(2 * Math.PI * phase * modulatorFreq);
            
            // Apply modulation to carrier
            float carrierPhase = phase * carrierFreq + modOutput * modulationIndex;
            waveform[i] = (float) Math.sin(2 * Math.PI * carrierPhase);
        }
    }
    
    /**
     * Generates an additive waveform.
     */
    private void generateAdditiveWaveform(float[] waveform, int sampleCount, Random randomGenerator) {
        // Determine number of harmonics
        int harmonicCount = 3 + randomGenerator.nextInt(15); // 3-17 harmonics
        
        // Generate harmonic amplitudes with natural rolloff
        float[] harmonicAmps = new float[harmonicCount];
        
        // Rolloff style (0 = even+odd, 1 = odd-only, 2 = decaying)
        int rolloffType = randomGenerator.nextInt(3);
        
        for (int h = 0; h < harmonicCount; h++) {
            int harmonicNumber = h + 1; // Harmonic number (1-based)
            
            switch (rolloffType) {
                case 0: // Even and odd harmonics
                    harmonicAmps[h] = 1.0f / harmonicNumber;
                    break;
                case 1: // Odd harmonics only (like square wave)
                    harmonicAmps[h] = harmonicNumber % 2 == 1 ? 1.0f / harmonicNumber : 0;
                    break;
                case 2: // Custom decay
                    float decay = 0.7f + randomGenerator.nextFloat() * 0.25f; // 0.7-0.95
                    harmonicAmps[h] = (float) Math.pow(decay, harmonicNumber - 1);
                    break;
            }
            
            // Add some randomization to the amplitudes
            harmonicAmps[h] *= 0.8f + randomGenerator.nextFloat() * 0.4f; // 0.8-1.2 variance
        }
        
        // Generate the waveform by summing harmonics
        for (int i = 0; i < sampleCount; i++) {
            float phase = (float) i / sampleCount;
            waveform[i] = 0;
            
            for (int h = 0; h < harmonicCount; h++) {
                int harmonicNumber = h + 1;
                waveform[i] += harmonicAmps[h] * (float) Math.sin(2 * Math.PI * phase * harmonicNumber);
            }
        }
        
        // Normalize the waveform
        WaveformUtil.normalize(waveform);
    }
    
    /**
     * Generates a formant waveform.
     */
    private void generateFormantWaveform(float[] waveform, int sampleCount, Random randomGenerator) {
        // Use 2-3 formant peaks to create vowel-like sounds
        int formantCount = 2 + randomGenerator.nextInt(2); // 2-3 formants
        float[] formantFreqs = new float[formantCount];
        float[] formantAmps = new float[formantCount];
        float[] formantQs = new float[formantCount];
        
        // Set formant parameters based on vowel-like shapes
        // Frequencies loosely based on typical vowel formants
        int vowelType = randomGenerator.nextInt(5); // 5 basic "vowel" types
        
        switch (vowelType) {
            case 0: // "A" like
                formantFreqs[0] = 0.1f; // ~800 Hz
                formantFreqs[1] = 0.25f; // ~1200 Hz
                if (formantCount > 2) formantFreqs[2] = 0.35f; // ~2500 Hz
                break;
            case 1: // "E" like
                formantFreqs[0] = 0.07f; // ~500 Hz
                formantFreqs[1] = 0.3f; // ~2000 Hz
                if (formantCount > 2) formantFreqs[2] = 0.45f; // ~3000 Hz
                break;
            case 2: // "I" like
                formantFreqs[0] = 0.06f; // ~400 Hz
                formantFreqs[1] = 0.35f; // ~2300 Hz
                if (formantCount > 2) formantFreqs[2] = 0.43f; // ~3000 Hz
                break;
            case 3: // "O" like
                formantFreqs[0] = 0.08f; // ~550 Hz
                formantFreqs[1] = 0.15f; // ~1000 Hz
                if (formantCount > 2) formantFreqs[2] = 0.28f; // ~2000 Hz
                break;
            case 4: // "U" like
                formantFreqs[0] = 0.05f; // ~350 Hz
                formantFreqs[1] = 0.12f; // ~800 Hz
                if (formantCount > 2) formantFreqs[2] = 0.3f; // ~2200 Hz
                break;
        }
        
        // Add some randomization to the frequencies
        for (int i = 0; i < formantCount; i++) {
            formantFreqs[i] *= 0.9f + randomGenerator.nextFloat() * 0.2f; // +/- 10%
            formantAmps[i] = 0.7f + randomGenerator.nextFloat() * 0.6f; // 0.7-1.3
            formantQs[i] = 0.05f + randomGenerator.nextFloat() * 0.05f; // Q factor (sharpness of peak)
        }
        
        // Base carrier (usually a sawtooth or pulse train for formants)
        boolean useSaw = randomGenerator.nextBoolean();
        float[] carrier = new float[sampleCount];
        
        // Generate carrier
        for (int i = 0; i < sampleCount; i++) {
            float phase = (float) i / sampleCount;
            if (useSaw) {
                carrier[i] = 2 * phase - 1; // Sawtooth
            } else {
                carrier[i] = (float) Math.sin(2 * Math.PI * phase); // Sine
            }
        }
        
        // Apply formant filtering (simplified model)
        // For each sample, calculate the sum of resonant peaks
        for (int i = 0; i < sampleCount; i++) {
            waveform[i] = 0;
            float phase = (float) i / sampleCount;
            
            for (int f = 0; f < formantCount; f++) {
                // Simple resonant filter emulation
                float resonance = (float) Math.sin(2 * Math.PI * phase / formantFreqs[f]);
                // Apply Q factor
                float envelope = (float) Math.exp(-phase / formantQs[f]);
                
                waveform[i] += carrier[i] * resonance * envelope * formantAmps[f];
            }
        }
        
        // Normalize and smooth
        WaveformUtil.normalize(waveform);
        WaveformUtil.smooth(waveform, 1 + randomGenerator.nextInt(2));
    }
} 