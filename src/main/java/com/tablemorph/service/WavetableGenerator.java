package com.tablemorph.service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import com.tablemorph.config.GeneratorConfig;

/**
 * Service class for generating random wavetables.
 * Creates complex multi-frame wavetables for use in the Vital synthesizer.
 *
 * @author Proxeeus
 */
public class WavetableGenerator {
    private static final String WAVETABLE_DIRECTORY = "wavetables";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final int SAMPLE_RATE = 44100;
    private static final int BITS_PER_SAMPLE = 16;
    private static final int CHANNELS = 1;
    
    // Morphing type constants
    public static final int MORPH_TYPE_BLEND = 1;   // Simple blending of samples and generated data
    public static final int MORPH_TYPE_ADDITIVE = 2; // Add sample data to generated data
    public static final int MORPH_TYPE_HARMONIC = 3; // Use sample data to influence harmonic content
    public static final int MORPH_TYPE_FOLD = 4;    // Apply waveshaping to sample data
    public static final int MORPH_TYPE_SPECTRAL = 5; // Mix in spectral domain
    
    private final Random random = new Random();

    /**
     * Creates the wavetables directory if it doesn't exist.
     */
    public void createWavetableDirectory() {
        File wavetableDir = new File(WAVETABLE_DIRECTORY);
        if (!wavetableDir.exists()) {
            wavetableDir.mkdir();
        }
    }

    /**
     * Gets the wavetable directory path.
     */
    public String getWavetableDirectoryPath() {
        return new File(WAVETABLE_DIRECTORY).getAbsolutePath();
    }

    /**
     * Generates a random wavetable and saves it as a WAV file.
     */
    public Path generateRandomWavetable() throws IOException {
        return generateUniqueWavetable(System.currentTimeMillis());
    }

    /**
     * Generates a unique wavetable using a specific seed value.
     */
    public Path generateUniqueWavetable(long seed) throws IOException {
        createWavetableDirectory();
        
        // Create seeded random generator
        Random seededRandom = new Random(seed);
        
        // Generate unique filename
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String filename = "tablemorph_" + timestamp + "_" + Math.abs(seededRandom.nextInt(10000)) + ".wav";
        Path outputPath = Paths.get(WAVETABLE_DIRECTORY, filename);
        
        // Generate wavetable data
        byte[] wavetableData = generateMultiFrameWavetable();
        
        // Write to default directory
        saveWavetableToFile(wavetableData, outputPath);
        
        // If enabled, also save to Vital's directory
        if (GeneratorConfig.getSaveToVital() && GeneratorConfig.isOSSupported()) {
            String vitalDir = GeneratorConfig.getVitalWavetablesDirectory();
            if (!vitalDir.isEmpty()) {
                // Create Vital directory if it doesn't exist
                File vitalDirectory = new File(vitalDir);
                if (!vitalDirectory.exists()) {
                    vitalDirectory.mkdirs();
                }
                
                Path vitalPath = Paths.get(vitalDir, filename);
                try {
                    saveWavetableToFile(wavetableData, vitalPath);
                    System.out.println("✓ Also saved to Vital's wavetables directory: " + vitalPath);
                } catch (IOException e) {
                    System.out.println("Warning: Could not save to Vital's directory: " + e.getMessage());
                }
            }
        }
        
        return outputPath;
    }

    /**
     * Generates a morphed wavetable by combining random generation with audio samples.
     */
    public Path generateMorphedWavetable(int morphType, List<File> soundFiles, String outputDirectory) throws IOException {
        return generateMorphedWavetable(morphType, soundFiles, outputDirectory, System.currentTimeMillis());
    }

    /**
     * Generates a morphed wavetable with a specific seed.
     */
    public Path generateMorphedWavetable(int morphType, List<File> soundFiles, String outputDirectory, long seed) 
            throws IOException {
        // Create output directory if it doesn't exist
        File morphDir = new File(outputDirectory);
        if (!morphDir.exists()) {
            morphDir.mkdir();
        }
        
        // Create seeded random generator
        Random seededRandom = new Random(seed);
        
        // Generate unique filename
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String filename = "tablemorph_morph_" + timestamp + "_" + Math.abs(seededRandom.nextInt(10000)) + ".wav";
        Path outputPath = Paths.get(outputDirectory, filename);
        
        // Process samples and generate wavetable data
        byte[] wavetableData = generateMorphedWavetableData(morphType, processSoundFiles(soundFiles, seededRandom), seededRandom);
        
        // Write to default directory
        saveWavetableToFile(wavetableData, outputPath);
        
        // If enabled, also save to Vital's directory
        if (GeneratorConfig.getSaveToVital() && GeneratorConfig.isOSSupported()) {
            String vitalDir = GeneratorConfig.getVitalWavetablesDirectory();
            if (!vitalDir.isEmpty()) {
                // Create Vital directory if it doesn't exist
                File vitalDirectory = new File(vitalDir);
                if (!vitalDirectory.exists()) {
                    vitalDirectory.mkdirs();
                }
                
                Path vitalPath = Paths.get(vitalDir, filename);
                try {
                    saveWavetableToFile(wavetableData, vitalPath);
                    System.out.println("✓ Also saved to Vital's wavetables directory: " + vitalPath);
                } catch (IOException e) {
                    System.out.println("Warning: Could not save to Vital's directory: " + e.getMessage());
                }
            }
        }
        
        return outputPath;
    }

    /**
     * Process sound files and extract sample data.
     */
    private List<float[]> processSoundFiles(List<File> soundFiles, Random seededRandom) throws IOException {
        // Limit the number of samples to process based on configuration
        int maxSamples = GeneratorConfig.getMaxMorphSamples();
        
        // Extract sample data from provided sound files
        List<float[]> sampleData = new ArrayList<>();
        List<File> filesToProcess = new ArrayList<>(soundFiles);
        
        // Shuffle the files list to randomize selection
        java.util.Collections.shuffle(filesToProcess, seededRandom);
        
        // Limit to max samples if needed
        if (filesToProcess.size() > maxSamples) {
            System.out.println("Note: Using " + maxSamples + " out of " + filesToProcess.size() + 
                " available sound files (configurable in settings)");
            filesToProcess = filesToProcess.subList(0, maxSamples);
        }
        
        // Process each selected file
        for (File soundFile : filesToProcess) {
            try {
                float[] samples = readWavFile(soundFile);
                if (samples != null && samples.length > 0) {
                    // Extract a random section of the sample if it's large
                    samples = extractRandomSection(samples, seededRandom);
                    sampleData.add(samples);
                    System.out.println("✓ Added " + soundFile.getName() + " to the morphing pool");
                }
            } catch (Exception e) {
                System.out.println("Warning: Could not read file " + soundFile.getName() + ": " + e.getMessage());
            }
        }
        
        return sampleData;
    }

    /**
     * Extracts a random section from a sample array.
     * For large samples, selects a random portion to use rather than the entire file.
     * Occasionally may return the complete sample based on configuration probability.
     */
    private float[] extractRandomSection(float[] samples, Random randomGenerator) {
        // Check if we should use the complete sample based on probability
        double fullSampleProbability = GeneratorConfig.getFullSampleProbability();
        if (randomGenerator.nextDouble() < fullSampleProbability) {
            // Use the full sample as-is, just resample if needed to match the target length
            System.out.println("  → Using complete sample (full sample mode)");
            return resampleIfNeeded(samples, GeneratorConfig.getWavetableSamples(), randomGenerator);
        }
        
        // If the sample is reasonably short, return as is
        int frameSize = GeneratorConfig.getWavetableSamples();
        if (samples.length <= frameSize * 2) {
            return samples;
        }
        
        // Calculate maximum section length (use between 5-15% of the file)
        int maxSectionLength = Math.min(samples.length, frameSize * 20);
        int minSectionLength = Math.min(samples.length, frameSize * 4);
        
        // Determine a random section length
        int sectionLength = minSectionLength + randomGenerator.nextInt(maxSectionLength - minSectionLength);
        
        // Choose a random starting point
        int maxStart = samples.length - sectionLength;
        int start = (maxStart > 0) ? randomGenerator.nextInt(maxStart) : 0;
        
        // Extract the section
        float[] section = new float[sectionLength];
        System.arraycopy(samples, start, section, 0, sectionLength);
        
        return section;
    }

    /**
     * Generates morphed wavetable audio data by combining sample data with generated wavetables.
     */
    private byte[] generateMorphedWavetableData(int morphType, List<float[]> sampleData, Random randomGenerator) {
        int frameCount = GeneratorConfig.getWavetableFrames();
        int sampleCount = GeneratorConfig.getWavetableSamples();
        
        // Check for direct sample incorporation mode
        double fullSampleProbability = GeneratorConfig.getFullSampleProbability();
        if (randomGenerator.nextDouble() < fullSampleProbability && !sampleData.isEmpty()) {
            // Choose a random sample to use as the primary source
            float[] selectedSample = sampleData.get(randomGenerator.nextInt(sampleData.size()));
            
            // Decide if we want to directly incorporate this sample or randomly distribute its data
            if (randomGenerator.nextDouble() < 0.5) {
                // Direct incorporation - use the sample as the primary basis for the wavetable
                System.out.println("  → Directly incorporating complete sample into wavetable");
                return generateDirectSampleWavetable(selectedSample, frameCount, sampleCount, randomGenerator);
            }
            
            // Random distribution - incorporate sample data at random positions
            System.out.println("  → Incorporating complete sample data at random positions");
            return generateRandomlyDistributedSampleWavetable(selectedSample, frameCount, sampleCount, randomGenerator);
        }
        
        // Total size of the wavetable (all frames, each with sampleCount samples)
        int wavetableLength = frameCount * sampleCount;
        
        float[][] frames = new float[frameCount][sampleCount];
        
        // Generate each frame
        for (int frame = 0; frame < frameCount; frame++) {
            // First, create a base random waveform
            float[] baseWaveform = generateRandomWaveform(frame, sampleCount, frameCount, randomGenerator);
            
            // Select a random sample source for this frame - potentially different for each frame
            float[] selectedSample = sampleData.get(randomGenerator.nextInt(sampleData.size()));
            
            // Create morphed frame based on the morph type
            frames[frame] = createMorphedFrame(morphType, baseWaveform, selectedSample, frame, frameCount, randomGenerator);
            
            // Apply final processing
            smoothWaveform(frames[frame], 3, sampleCount);
            normalizeWaveform(frames[frame]);
        }
        
        // Combine all frames into a single continuous buffer
        float[] combinedFrames = new float[wavetableLength];
        for (int frame = 0; frame < frameCount; frame++) {
            System.arraycopy(frames[frame], 0, combinedFrames, frame * sampleCount, sampleCount);
        }
        
        // Convert to PCM data
        return convertToPCM(frames);
    }

    /**
     * Creates a morphed frame by combining the generated waveform with sample data
     * using the specified morphing algorithm.
     */
    private float[] createMorphedFrame(int morphType, float[] baseWaveform, float[] sampleData, 
                                     int frame, int frameCount, Random randomGenerator) {
        float[] result = new float[baseWaveform.length];
        
        // Calculate morphing amount based on frame position (more original at start, more sample at end)
        float morphAmount = (float) frame / frameCount;
        morphAmount = morphAmount * 0.8f + 0.1f; // Keep within 0.1 - 0.9 range
        
        // Apply randomization to morphAmount
        morphAmount += (randomGenerator.nextFloat() * 0.4f - 0.2f); // +/- 0.2 randomization
        morphAmount = Math.max(0.05f, Math.min(0.95f, morphAmount)); // Clamp to 0.05-0.95
        
        // Resample the sample data if needed
        float[] resampledData = resampleIfNeeded(sampleData, baseWaveform.length, randomGenerator);
        
        switch (morphType) {
            case MORPH_TYPE_BLEND:
                // Simple crossfade between generated and sample data
                for (int i = 0; i < result.length; i++) {
                    result[i] = baseWaveform[i] * (1 - morphAmount) + resampledData[i] * morphAmount;
                }
                break;
                
            case MORPH_TYPE_ADDITIVE:
                // Add sample data to generated data with scaling
                for (int i = 0; i < result.length; i++) {
                    result[i] = baseWaveform[i] + (resampledData[i] * morphAmount * 0.5f);
                }
                break;
                
            case MORPH_TYPE_HARMONIC:
                // Use sample data to influence harmonic content
                float[] harmonicWaveform = new float[result.length];
                int harmonicCount = 1 + randomGenerator.nextInt(5); // 1-5 harmonics
                
                for (int h = 1; h <= harmonicCount; h++) {
                    float harmonicStrength = 1.0f / h;
                    for (int i = 0; i < result.length; i++) {
                        int idx = (i * h) % result.length;
                        harmonicWaveform[i] += resampledData[idx] * harmonicStrength;
                    }
                }
                
                // Normalize harmonic waveform
                normalizeWaveform(harmonicWaveform);
                
                // Blend with base waveform
                for (int i = 0; i < result.length; i++) {
                    result[i] = baseWaveform[i] * (1 - morphAmount) + harmonicWaveform[i] * morphAmount;
                }
                break;
                
            case MORPH_TYPE_FOLD:
                // Apply waveshaping/folding based on sample data
                for (int i = 0; i < result.length; i++) {
                    // Wavefold: reflect values that exceed threshold back
                    float foldThreshold = 0.3f + morphAmount * 0.4f;
                    float value = baseWaveform[i];
                    float foldInfluence = resampledData[i] * morphAmount;
                    
                    // Apply folding based on sample influence
                    if (Math.abs(value) > foldThreshold + Math.abs(foldInfluence * 0.2f)) {
                        float excess = Math.abs(value) - foldThreshold;
                        value = Math.signum(value) * (foldThreshold - excess);
                    }
                    
                    result[i] = value;
                }
                break;
                
            case MORPH_TYPE_SPECTRAL:
                // Create a spectral morphing effect
                // (Simple implementation that creates phase modulation)
                for (int i = 0; i < result.length; i++) {
                    // Use sample data to modulate the phase of the base waveform
                    int modulatedIndex = (i + (int)(resampledData[i] * morphAmount * 20)) % result.length;
                    if (modulatedIndex < 0) modulatedIndex += result.length;
                    result[i] = baseWaveform[modulatedIndex];
                }
                break;
                
            default:
                // Default to blend if unknown morph type
                for (int i = 0; i < result.length; i++) {
                    result[i] = baseWaveform[i] * (1 - morphAmount) + resampledData[i] * morphAmount;
                }
        }
        
        return result;
    }

    /**
     * Resamples the input array to match the target length.
     */
    private float[] resampleIfNeeded(float[] input, int targetLength, Random randomGenerator) {
        // If already the right length, return a copy
        if (input.length == targetLength) {
            return input.clone();
        }
        
        float[] output = new float[targetLength];
        
        // Choose a random offset to start reading from input (for variety)
        int offset = randomGenerator.nextInt(input.length);
        
        // Linear interpolation resampling
        for (int i = 0; i < targetLength; i++) {
            double position = (double) i * input.length / targetLength;
            int index1 = (int) position;
            int index2 = (index1 + 1) % input.length;
            
            // Adjust indices with offset
            index1 = (index1 + offset) % input.length;
            index2 = (index2 + offset) % input.length;
            
            float fraction = (float) (position - Math.floor(position));
            output[i] = input[index1] * (1 - fraction) + input[index2] * fraction;
        }
        
        return output;
    }

    /**
     * Generates a random waveform for a specific frame.
     */
    private float[] generateRandomWaveform(int frame, int wavetableLength, int numFrames) {
        return generateRandomWaveform(frame, wavetableLength, numFrames, this.random);
    }
    
    /**
     * Generates a random waveform for a specific frame using the provided Random instance.
     */
    private float[] generateRandomWaveform(int frame, int wavetableLength, int numFrames, Random randomGenerator) {
        float[] waveform = new float[wavetableLength];
        
        // Choose a random waveform type with some frame-specific variations
        int type = randomGenerator.nextInt(12); // Increased number of waveform types
        switch (type) {
            case 0: // Sine wave with phase variation
                double phase = (frame * 2 * Math.PI) / numFrames;
                for (int i = 0; i < wavetableLength; i++) {
                    waveform[i] = (float) Math.sin(2 * Math.PI * i / wavetableLength + phase);
                }
                break;
                
            case 1: // Sawtooth with varying harmonics
                int harmonics = (frame % 5) + 1;
                for (int i = 0; i < wavetableLength; i++) {
                    float value = 0;
                    for (int h = 1; h <= harmonics; h++) {
                        value += (float) (Math.sin(2 * Math.PI * h * i / wavetableLength) / h);
                    }
                    waveform[i] = value;
                }
                break;
                
            case 2: // Square with varying pulse width
                float pulseWidth = 0.3f + (frame * 0.4f / numFrames);
                for (int i = 0; i < wavetableLength; i++) {
                    waveform[i] = (i / (float) wavetableLength < pulseWidth) ? 1.0f : -1.0f;
                }
                break;
                
            case 3: // Triangle with varying slope
                float slope = 0.5f + (frame * 0.5f / numFrames);
                for (int i = 0; i < wavetableLength; i++) {
                    float x = i / (float) wavetableLength;
                    waveform[i] = (x < slope) ? 
                        (2 * x / slope - 1) : 
                        (2 * (1 - x) / (1 - slope) - 1);
                }
                break;
                
            case 4: // Noise with varying smoothing
                for (int i = 0; i < wavetableLength; i++) {
                    waveform[i] = (float) (randomGenerator.nextDouble() * 2 - 1);
                }
                int windowSize = (frame % 5) + 3;
                smoothWaveform(waveform, windowSize, wavetableLength);
                break;

            case 5: // Complex waveform with multiple sine waves
                for (int i = 0; i < wavetableLength; i++) {
                    float value = 0;
                    int numWaves = (frame % 3) + 2;
                    for (int w = 0; w < numWaves; w++) {
                        float freq = (float) (1 + w * 2);
                        float wavePhase = (float) (frame * Math.PI / numFrames);
                        value += (float) (Math.sin(2 * Math.PI * freq * i / wavetableLength + wavePhase) / freq);
                    }
                    waveform[i] = value;
                }
                break;

            case 6: // Exponential sweep
                float startFreq = 1.0f;
                float endFreq = (frame + 1) * 4.0f;
                for (int i = 0; i < wavetableLength; i++) {
                    float t = i / (float) wavetableLength;
                    float freq = startFreq + (endFreq - startFreq) * t;
                    waveform[i] = (float) Math.sin(2 * Math.PI * freq * t);
                }
                break;

            case 7: // FM synthesis-like waveform
                float carrierFreq = 1.0f;
                float modFreq = (frame % 3) + 1;
                float modIndex = 0.5f + (frame * 0.5f / numFrames);
                for (int i = 0; i < wavetableLength; i++) {
                    float t = i / (float) wavetableLength;
                    float modulator = (float) Math.sin(2 * Math.PI * modFreq * t);
                    waveform[i] = (float) Math.sin(2 * Math.PI * carrierFreq * t + modIndex * modulator);
                }
                break;

            case 8: // Additive synthesis with inharmonic partials
                for (int i = 0; i < wavetableLength; i++) {
                    float value = 0;
                    int numPartials = (frame % 4) + 2;
                    for (int p = 0; p < numPartials; p++) {
                        float freq = (float) (1 + p * 1.5f); // Inharmonic frequencies
                        float amplitude = 1.0f / (p + 1);
                        float partialPhase = (float) (frame * Math.PI / numFrames);
                        value += (float) (amplitude * Math.sin(2 * Math.PI * freq * i / wavetableLength + partialPhase));
                    }
                    waveform[i] = value;
                }
                break;

            case 9: // Ring modulation-like waveform
                float freq1 = 1.0f;
                float freq2 = (frame % 3) + 1;
                for (int i = 0; i < wavetableLength; i++) {
                    float t = i / (float) wavetableLength;
                    float wave1 = (float) Math.sin(2 * Math.PI * freq1 * t);
                    float wave2 = (float) Math.sin(2 * Math.PI * freq2 * t);
                    waveform[i] = wave1 * wave2;
                }
                break;

            case 10: // Complex modulation with multiple carriers
                for (int i = 0; i < wavetableLength; i++) {
                    float t = i / (float) wavetableLength;
                    float value = 0;
                    int numCarriers = (frame % 2) + 1;
                    for (int c = 0; c < numCarriers; c++) {
                        float carrierFrequency = 1.0f + c * 2.0f;
                        float modulatorFrequency = (frame % 3) + 1;
                        float modulationIndex = 0.3f + (frame * 0.3f / numFrames);
                        float modulator = (float) Math.sin(2 * Math.PI * modulatorFrequency * t);
                        value += (float) (Math.sin(2 * Math.PI * carrierFrequency * t + modulationIndex * modulator));
                    }
                    waveform[i] = value;
                }
                break;

            case 11: // Spectral morphing waveform
                for (int i = 0; i < wavetableLength; i++) {
                    float t = i / (float) wavetableLength;
                    float value = 0;
                    int numBands = (frame % 3) + 2;
                    for (int b = 0; b < numBands; b++) {
                        float bandFreq = (float) (1 + b * 3);
                        float bandWidth = 0.1f + (frame * 0.2f / numFrames);
                        float bandPhase = (float) (frame * Math.PI / numFrames);
                        value += (float) (Math.sin(2 * Math.PI * bandFreq * t + bandPhase) * 
                                        Math.exp(-bandWidth * (t - 0.5f) * (t - 0.5f)));
                    }
                    waveform[i] = value;
                }
                break;
        }

        // Add additional randomization to ensure uniqueness between wavetables
        if (randomGenerator.nextBoolean()) {
            // Apply random amplitude modulation
            float modRate = 0.5f + randomGenerator.nextFloat() * 2.0f;
            for (int i = 0; i < wavetableLength; i++) {
                float t = i / (float) wavetableLength;
                float mod = 0.7f + 0.3f * (float) Math.sin(2 * Math.PI * modRate * t);
                waveform[i] *= mod;
            }
        }

        // Apply additional processing based on frame position
        if (frame % 2 == 0) {
            // Even frames: apply subtle distortion
            for (int i = 0; i < wavetableLength; i++) {
                float x = waveform[i];
                waveform[i] = (float) (Math.tanh(x * 1.5f) / Math.tanh(1.5f));
            }
        } else {
            // Odd frames: apply subtle filtering
            float cutoff = 0.5f + (frame * 0.5f / numFrames);
            for (int i = 0; i < wavetableLength; i++) {
                float x = i / (float) wavetableLength;
                if (x > cutoff) {
                    waveform[i] *= (1.0f - (x - cutoff) / (1.0f - cutoff));
                }
            }
        }

        // Normalize the waveform
        normalizeWaveform(waveform);
        
        return waveform;
    }

    /**
     * Smooths a waveform using a simple moving average.
     */
    private void smoothWaveform(float[] waveform, int windowSize, int wavetableLength) {
        float[] smoothed = new float[wavetableLength];
        
        for (int i = 0; i < wavetableLength; i++) {
            float sum = 0;
            int count = 0;
            
            for (int j = -windowSize/2; j <= windowSize/2; j++) {
                int idx = (i + j + wavetableLength) % wavetableLength;
                sum += waveform[idx];
                count++;
            }
            
            smoothed[i] = sum / count;
        }
        
        System.arraycopy(smoothed, 0, waveform, 0, wavetableLength);
    }

    /**
     * Normalizes a waveform to prevent clipping.
     */
    private void normalizeWaveform(float[] waveform) {
        float max = 0;
        for (float sample : waveform) {
            max = Math.max(max, Math.abs(sample));
        }
        
        if (max > 0) {
            for (int i = 0; i < waveform.length; i++) {
                waveform[i] /= max;
            }
        }
    }

    /**
     * Converts float waveform data to 16-bit PCM.
     */
    private byte[] convertToPCM(float[] waveform) {
        byte[] pcm = new byte[waveform.length * 2];
        for (int i = 0; i < waveform.length; i++) {
            short sample = (short) (waveform[i] * Short.MAX_VALUE);
            pcm[i * 2] = (byte) (sample & 0xFF);
            pcm[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        return pcm;
    }

    /**
     * Converts 2D wavetable data to PCM byte array.
     */
    private byte[] convertToPCM(float[][] waveformData) {
        int frameCount = waveformData.length;
        int sampleCount = waveformData[0].length;
        float[] combinedFrames = new float[frameCount * sampleCount];
        
        // Combine all frames
        for (int frame = 0; frame < frameCount; frame++) {
            System.arraycopy(waveformData[frame], 0, combinedFrames, frame * sampleCount, sampleCount);
        }
        
        return convertToPCM(combinedFrames);
    }

    /**
     * Saves PCM data to a WAV file.
     */
    private void saveWavetableToFile(byte[] pcmData, Path filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            // Write WAV header
            writeWavHeader(fos, pcmData.length);
            
            // Write PCM data
            fos.write(pcmData);
        }
    }

    /**
     * Writes a WAV file header.
     */
    private void writeWavHeader(FileOutputStream fos, int audioDataLength) throws IOException {
        // RIFF header
        fos.write("RIFF".getBytes());
        writeInt(fos, 36 + audioDataLength); // Total file size - 8
        fos.write("WAVE".getBytes());
        
        // Format chunk
        fos.write("fmt ".getBytes());
        writeInt(fos, 16); // Format chunk size
        writeShort(fos, 1); // Audio format (1 = PCM)
        writeShort(fos, CHANNELS);
        writeInt(fos, SAMPLE_RATE);
        writeInt(fos, SAMPLE_RATE * CHANNELS * (BITS_PER_SAMPLE / 8)); // Byte rate
        writeShort(fos, CHANNELS * (BITS_PER_SAMPLE / 8)); // Block align
        writeShort(fos, BITS_PER_SAMPLE);
        
        // Data chunk
        fos.write("data".getBytes());
        writeInt(fos, audioDataLength);
    }

    /**
     * Writes a 32-bit integer to the output stream.
     */
    private void writeInt(FileOutputStream fos, int value) throws IOException {
        fos.write(value & 0xFF);
        fos.write((value >> 8) & 0xFF);
        fos.write((value >> 16) & 0xFF);
        fos.write((value >> 24) & 0xFF);
    }

    /**
     * Writes a 16-bit integer to the output stream.
     */
    private void writeShort(FileOutputStream fos, int value) throws IOException {
        fos.write(value & 0xFF);
        fos.write((value >> 8) & 0xFF);
    }

    /**
     * Generates a multi-frame wavetable with random waveforms.
     */
    private byte[] generateMultiFrameWavetable() {
        int frameCount = GeneratorConfig.getWavetableFrames();
        int sampleCount = GeneratorConfig.getWavetableSamples();
        
        float[][] frames = new float[frameCount][sampleCount];
        
        // Generate each frame
        for (int frame = 0; frame < frameCount; frame++) {
            frames[frame] = generateRandomWaveform(frame, sampleCount, frameCount);
            
            // Apply smoothing and normalization
            smoothWaveform(frames[frame], 3, sampleCount);
            normalizeWaveform(frames[frame]);
        }
        
        // Convert to PCM data
        return convertToPCM(frames);
    }

    /**
     * Reads a WAV file and returns its sample data as a float array.
     */
    private float[] readWavFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis)) {
            
            // Read WAV header
            byte[] header = new byte[12];
            if (bis.read(header, 0, 12) != 12) {
                throw new IOException("Invalid WAV file format - header too short");
            }
            
            // Verify RIFF header
            if (!new String(header, 0, 4).equals("RIFF") || 
                !new String(header, 8, 4).equals("WAVE")) {
                throw new IOException("Invalid WAV file format - not a RIFF/WAVE file");
            }
            
            // Read chunks until we find the format and data chunks
            byte[] chunkHeader = new byte[8];
            int formatChannels = 0;
            int formatSampleRate = 0;
            int formatBitsPerSample = 0;
            byte[] audioData = null;
            
            while (true) {
                int bytesRead = bis.read(chunkHeader, 0, 8);
                if (bytesRead < 8) {
                    break; // End of file
                }
                
                String chunkId = new String(chunkHeader, 0, 4);
                int chunkSize = (chunkHeader[4] & 0xFF) | ((chunkHeader[5] & 0xFF) << 8) |
                               ((chunkHeader[6] & 0xFF) << 16) | ((chunkHeader[7] & 0xFF) << 24);
                
                if (chunkSize < 0 || chunkSize > file.length()) {
                    // Invalid chunk size, try to skip a smaller amount
                    bis.skip(4);
                    continue;
                }
                
                if (chunkId.equals("fmt ")) {
                    // Read format chunk
                    byte[] formatData = new byte[Math.min(chunkSize, 16)]; // Read at most 16 bytes
                    bis.read(formatData, 0, formatData.length);
                    if (chunkSize > 16) {
                        bis.skip(chunkSize - 16); // Skip any extra format data
                    }
                    
                    formatChannels = (formatData[2] & 0xFF) | ((formatData[3] & 0xFF) << 8);
                    formatSampleRate = (formatData[4] & 0xFF) | ((formatData[5] & 0xFF) << 8) |
                                     ((formatData[6] & 0xFF) << 16) | ((formatData[7] & 0xFF) << 24);
                    formatBitsPerSample = (formatData[14] & 0xFF) | ((formatData[15] & 0xFF) << 8);
                    
                } else if (chunkId.equals("data")) {
                    // Read data chunk
                    audioData = new byte[chunkSize];
                    int totalRead = 0;
                    while (totalRead < chunkSize) {
                        int read = bis.read(audioData, totalRead, chunkSize - totalRead);
                        if (read == -1) {
                            break; // End of file
                        }
                        totalRead += read;
                    }
                    break; // We found the data chunk, no need to continue
                    
                } else {
                    // Skip unknown chunk
                    long skipped = bis.skip(chunkSize);
                    if (skipped < chunkSize) {
                        break; // Couldn't skip the full chunk, probably end of file
                    }
                }
            }
            
            if (audioData == null || formatChannels == 0) {
                throw new IOException("Could not find valid data chunk");
            }
            
            // Convert audio data to float samples
            int bytesPerSample = formatBitsPerSample / 8;
            int numSamples = audioData.length / (formatChannels * bytesPerSample);
            float[] samples = new float[numSamples];
            
            for (int i = 0; i < numSamples; i++) {
                // Read all channels but only use first channel
                int sampleOffset = i * formatChannels * bytesPerSample;
                
                if (sampleOffset + bytesPerSample > audioData.length) {
                    break; // End of data
                }
                
                if (formatBitsPerSample == 16) {
                    short value = (short) ((audioData[sampleOffset] & 0xFF) | 
                                         (audioData[sampleOffset + 1] << 8));
                    samples[i] = value / (float) Short.MAX_VALUE;
                } else if (formatBitsPerSample == 24) {
                    int value = (audioData[sampleOffset] & 0xFF) | 
                              ((audioData[sampleOffset + 1] & 0xFF) << 8) |
                              (audioData[sampleOffset + 2] << 16);
                    if ((value & 0x800000) != 0) {
                        value |= 0xFF000000;  // Sign extend
                    }
                    samples[i] = value / 8388608.0f;  // 2^23
                } else if (formatBitsPerSample == 32) {
                    int value = (audioData[sampleOffset] & 0xFF) | 
                              ((audioData[sampleOffset + 1] & 0xFF) << 8) |
                              ((audioData[sampleOffset + 2] & 0xFF) << 16) | 
                              (audioData[sampleOffset + 3] << 24);
                    samples[i] = value / (float) Integer.MAX_VALUE;
                }
            }
            
            return samples;
        }
    }

    /**
     * Generates a wavetable directly from a sample, preserving its characteristics.
     */
    private byte[] generateDirectSampleWavetable(float[] sample, int frameCount, int sampleCount, Random randomGenerator) {
        float[][] frames = new float[frameCount][sampleCount];
        
        // Calculate how many samples to use per frame
        int samplesPerFrame = sample.length / frameCount;
        if (samplesPerFrame < sampleCount) {
            samplesPerFrame = sampleCount;  // Ensure we have enough samples
        }
        
        for (int frame = 0; frame < frameCount; frame++) {
            // Select start position for this frame
            int startPos = frame * samplesPerFrame;
            if (startPos + sampleCount > sample.length) {
                startPos = sample.length - sampleCount;  // Adjust if near end
            }
            
            // Copy samples for this frame
            float[] frameData = new float[sampleCount];
            for (int i = 0; i < sampleCount; i++) {
                frameData[i] = sample[(startPos + i) % sample.length];
            }
            
            // Apply some random variations to make each frame unique
            if (randomGenerator.nextFloat() < 0.3f) {  // 30% chance of variation
                float variation = 0.1f + randomGenerator.nextFloat() * 0.2f;  // 10-30% variation
                for (int i = 0; i < sampleCount; i++) {
                    frameData[i] += (randomGenerator.nextFloat() * 2 - 1) * variation;
                }
            }
            
            // Ensure the frame loops smoothly
            smoothLoopPoints(frameData);
            
            // Normalize the frame
            normalizeWaveform(frameData);
            
            frames[frame] = frameData;
        }
        
        return convertToPCM(frames);
    }

    /**
     * Generates a wavetable by randomly distributing sample data across frames.
     */
    private byte[] generateRandomlyDistributedSampleWavetable(float[] sample, int frameCount, int sampleCount, Random randomGenerator) {
        float[][] frames = new float[frameCount][sampleCount];
        
        // Create base frames using random waveforms
        for (int frame = 0; frame < frameCount; frame++) {
            frames[frame] = generateRandomWaveform(frame, sampleCount, frameCount, randomGenerator);
        }
        
        // Randomly incorporate sample data into frames
        int numSampleSegments = Math.min(frameCount, 5 + randomGenerator.nextInt(6));  // 5-10 segments
        for (int i = 0; i < numSampleSegments; i++) {
            // Select random frame to modify
            int targetFrame = randomGenerator.nextInt(frameCount);
            
            // Select random section from sample
            int sectionLength = Math.min(sampleCount, sample.length);
            int startPos = randomGenerator.nextInt(sample.length - sectionLength + 1);
            
            // Copy sample section
            float[] sampleSection = new float[sectionLength];
            System.arraycopy(sample, startPos, sampleSection, 0, sectionLength);
            
            // Normalize section
            normalizeWaveform(sampleSection);
            
            // Blend with existing frame
            float blendAmount = 0.3f + randomGenerator.nextFloat() * 0.7f;  // 30-100% blend
            for (int j = 0; j < sampleCount; j++) {
                frames[targetFrame][j] = frames[targetFrame][j] * (1 - blendAmount) +
                                       sampleSection[j % sectionLength] * blendAmount;
            }
            
            // Ensure smooth loop
            smoothLoopPoints(frames[targetFrame]);
            
            // Normalize the frame
            normalizeWaveform(frames[targetFrame]);
        }
        
        return convertToPCM(frames);
    }

    /**
     * Ensures smooth loop points in a waveform by applying crossfading.
     */
    private void smoothLoopPoints(float[] waveform) {
        int crossfadeLength = Math.min(waveform.length / 8, 64);  // Up to 64 samples or 1/8 of length
        
        // Apply crossfade between start and end
        for (int i = 0; i < crossfadeLength; i++) {
            float fadeIn = i / (float) crossfadeLength;
            float fadeOut = 1 - fadeIn;
            
            float startSample = waveform[i];
            float endSample = waveform[waveform.length - crossfadeLength + i];
            
            waveform[i] = startSample * fadeIn + endSample * fadeOut;
            waveform[waveform.length - crossfadeLength + i] = startSample * fadeOut + endSample * fadeIn;
        }
    }
} 