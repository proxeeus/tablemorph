package com.tablemorph.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.tablemorph.config.GeneratorConfig;
import com.tablemorph.model.MorphType;
import com.tablemorph.model.Wavetable;
import com.tablemorph.util.DirectoryUtil;
import com.tablemorph.util.WavFileUtil;
import com.tablemorph.util.WaveformUtil;

/**
 * Service class for morphing wavetables with audio samples.
 */
public class WavetableMorphService {
    private static final String MORPHS_DIRECTORY = "morphs";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    private final Random random = new Random();
    private final MultiFrameWavetableGenerator wavetableGenerator;
    
    public WavetableMorphService() {
        this.wavetableGenerator = new MultiFrameWavetableGenerator();
    }
    
    /**
     * Generates a morphed wavetable by combining generated waveforms with audio samples.
     * 
     * @param morphType The type of morphing algorithm to use
     * @param soundFiles List of audio files to use for morphing
     * @return Path to the generated morphed wavetable file
     * @throws IOException If an error occurs during generation
     */
    public Path generateMorphedWavetable(MorphType morphType, List<File> soundFiles) throws IOException {
        return generateMorphedWavetable(morphType, soundFiles, MORPHS_DIRECTORY, System.currentTimeMillis());
    }
    
    /**
     * Generates a morphed wavetable with specific seed and output directory.
     * 
     * @param morphType The type of morphing algorithm to use
     * @param soundFiles List of audio files to use for morphing
     * @param outputDirectory The directory to save the morphed wavetable to
     * @param seed Random seed for reproducible generation
     * @return Path to the generated morphed wavetable file
     * @throws IOException If an error occurs during generation
     */
    public Path generateMorphedWavetable(MorphType morphType, List<File> soundFiles, 
                                        String outputDirectory, long seed) throws IOException {
        DirectoryUtil.createDirectoryIfNotExists(outputDirectory);
        
        // Create seeded random generator
        Random seededRandom = new Random(seed);
        
        // Process sound files to extract sample data
        List<float[]> sampleData = processSoundFiles(soundFiles, seededRandom);
        
        if (sampleData.isEmpty()) {
            throw new IOException("No valid audio samples were found in the provided files");
        }
        
        // Generate unique filename
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String filename = "morph_" + morphType.getName().toLowerCase() + "_" + timestamp + "_" + 
            Math.abs(seededRandom.nextInt(10000)) + ".wav";
        Path outputPath = Paths.get(outputDirectory, filename);
        
        // Generate morphed wavetable
        Wavetable wavetable = generateMorphedWavetable(morphType, sampleData, seededRandom);
        wavetable.setFilePath(outputPath);
        
        // Write to output directory
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
     * Batch generates multiple morphed wavetables.
     * 
     * @param morphTypes List of morphing types to use
     * @param soundFiles List of audio files for morphing
     * @param count Number of wavetables to generate
     * @return List of paths to the generated wavetables
     * @throws IOException If an error occurs during generation
     */
    public List<Path> batchGenerateMorphedWavetables(List<MorphType> morphTypes, 
                                                   List<File> soundFiles, int count) throws IOException {
        DirectoryUtil.createDirectoryIfNotExists(MORPHS_DIRECTORY);
        List<Path> generatedFiles = new ArrayList<>();
        
        if (soundFiles.isEmpty()) {
            throw new IOException("No sound files provided for morphing");
        }
        
        for (int i = 0; i < count; i++) {
            // Choose a random morph type for each wavetable
            MorphType morphType = morphTypes.get(random.nextInt(morphTypes.size()));
            
            // Select a random subset of the sound files
            int sampleCount = 1 + random.nextInt(Math.min(soundFiles.size(), 
                                                         GeneratorConfig.getMaxMorphSamples()));
            List<File> selectedFiles = selectRandomFiles(soundFiles, sampleCount);
            
            // Generate a unique seed based on current time and iteration
            long seed = System.currentTimeMillis() + i;
            
            // Print progress
            System.out.print("Generating morphed wavetable " + (i+1) + "/" + count + 
                            " [" + morphType.getName() + "]... ");
            
            // Generate morphed wavetable
            Path path = generateMorphedWavetable(morphType, selectedFiles, MORPHS_DIRECTORY, seed);
            generatedFiles.add(path);
            
            System.out.println("[SUCCESS] Done!");
            System.out.println("  [FILE] " + path.getFileName());
            
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
     * Processes sound files to extract sample data.
     * 
     * @param soundFiles The sound files to process
     * @param randomGenerator The random number generator to use
     * @return List of float arrays containing sample data
     * @throws IOException If an error occurs reading the files
     */
    private List<float[]> processSoundFiles(List<File> soundFiles, Random randomGenerator) throws IOException {
        // Limit the number of samples to process based on configuration
        int maxSamples = GeneratorConfig.getMaxMorphSamples();
        
        // Extract sample data from provided sound files
        List<float[]> sampleData = new ArrayList<>();
        List<File> filesToProcess = new ArrayList<>(soundFiles);
        
        // Shuffle the files list to randomize selection
        Collections.shuffle(filesToProcess, randomGenerator);
        
        // Limit to max samples if needed
        if (filesToProcess.size() > maxSamples) {
            System.out.println("Note: Using " + maxSamples + " out of " + filesToProcess.size() + 
                " available sound files (configurable in settings)");
            filesToProcess = filesToProcess.subList(0, maxSamples);
        }
        
        // Process each selected file
        for (File soundFile : filesToProcess) {
            try {
                float[] samples = WavFileUtil.readWavFile(soundFile);
                if (samples != null && samples.length > 0) {
                    // Extract a random section of the sample if it's large
                    samples = extractRandomSection(samples, randomGenerator);
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
     * 
     * @param samples The full sample array
     * @param randomGenerator The random number generator to use
     * @return A random section of the sample array
     */
    private float[] extractRandomSection(float[] samples, Random randomGenerator) {
        // Check if we should use the complete sample based on probability
        double fullSampleProbability = GeneratorConfig.getFullSampleProbability();
        if (randomGenerator.nextDouble() < fullSampleProbability) {
            // Use the full sample as-is, just resample if needed to match the target length
            System.out.println("  → Using complete sample (full sample mode)");
            return WaveformUtil.resample(samples, GeneratorConfig.getWavetableSamples(), randomGenerator);
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
        
        System.out.println("  → Extracted " + sectionLength + " samples from position " + start);
        
        return section;
    }
    
    /**
     * Selects a random subset of files.
     * 
     * @param files The full list of files
     * @param count The number of files to select
     * @return A randomly selected subset of files
     */
    private List<File> selectRandomFiles(List<File> files, int count) {
        List<File> shuffled = new ArrayList<>(files);
        Collections.shuffle(shuffled, random);
        
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }
    
    /**
     * Generates a morphed wavetable by combining generated waveforms with audio samples.
     * 
     * @param morphType The type of morphing algorithm to use
     * @param sampleData List of audio samples to use for morphing
     * @param randomGenerator The random number generator to use
     * @return The generated wavetable object
     */
    private Wavetable generateMorphedWavetable(MorphType morphType, List<float[]> sampleData, 
                                            Random randomGenerator) {
        int frameCount = GeneratorConfig.getWavetableFrames();
        int sampleCount = GeneratorConfig.getWavetableSamples();
        float[][] frames = new float[frameCount][sampleCount];
        
        // Generate each frame
        for (int frame = 0; frame < frameCount; frame++) {
            // First, create a base random waveform
            float[] baseWaveform = generateFrameWaveform(frame, frameCount, sampleCount, randomGenerator);
            
            // Select a random sample source for this frame - potentially different for each frame
            float[] selectedSample = sampleData.get(randomGenerator.nextInt(sampleData.size()));
            
            // Create morphed frame based on the morph type
            frames[frame] = createMorphedFrame(morphType, baseWaveform, selectedSample, 
                                             frame, frameCount, randomGenerator);
            
            // Apply final processing
            WaveformUtil.smooth(frames[frame], 3);
            WaveformUtil.normalize(frames[frame]);
        }
        
        // Convert to PCM data
        byte[] pcmData = WavFileUtil.convertToPCM(frames);
        
        return new Wavetable(morphType, frameCount, sampleCount, pcmData);
    }
    
    /**
     * Generates a random waveform for a specific frame.
     * 
     * @param frame The frame index
     * @param frameCount The total number of frames
     * @param sampleCount The number of samples per frame
     * @param randomGenerator The random number generator to use
     * @return The generated waveform
     */
    private float[] generateFrameWaveform(int frame, int frameCount, int sampleCount, Random randomGenerator) {
        float[] waveform = new float[sampleCount];
        
        // Generate a random waveform (similar to MultiFrameWavetableGenerator)
        float frameFactor = 1.0f - Math.abs((frame / (float)(frameCount - 1)) * 2 - 1);
        
        // Determine waveform complexity based on frame position
        double complexity = 0.3 + 0.7 * frameFactor; // More complex in the middle
        
        // Generate a base waveform with harmonics that varies with frame position
        int harmonicCount = 3 + (int)(15 * complexity);
        
        // Generate the harmonic content
        for (int i = 0; i < sampleCount; i++) {
            float phase = (float) i / sampleCount;
            waveform[i] = 0;
            
            for (int h = 1; h <= harmonicCount; h++) {
                float amplitude = (float)(1.0f / h * Math.pow(0.8, h-1));
                waveform[i] += amplitude * (float) Math.sin(2 * Math.PI * h * phase);
            }
        }
        
        // Apply some random modulation
        if (randomGenerator.nextDouble() < 0.7) {
            int modType = randomGenerator.nextInt(3);
            float modDepth = 0.2f + randomGenerator.nextFloat() * 0.6f;
            
            for (int i = 0; i < sampleCount; i++) {
                float phase = (float) i / sampleCount;
                float mod = (float) Math.sin(2 * Math.PI * phase * (2 + randomGenerator.nextInt(5)));
                
                switch (modType) {
                    case 0: // Amplitude modulation
                        waveform[i] *= 1.0f + mod * modDepth;
                        break;
                    case 1: // Filter-like effect (simple lowpass)
                        int offset = (int)(mod * modDepth * 10);
                        int idx = Math.max(0, Math.min(sampleCount-1, i + offset));
                        waveform[i] = 0.7f * waveform[i] + 0.3f * waveform[idx];
                        break;
                    case 2: // Wavefolding
                        if (Math.abs(waveform[i]) > 0.8f) {
                            float excess = Math.abs(waveform[i]) - 0.8f;
                            waveform[i] = Math.signum(waveform[i]) * (0.8f - excess * modDepth);
                        }
                        break;
                }
            }
        }
        
        // Normalize the waveform
        WaveformUtil.normalize(waveform);
        
        return waveform;
    }
    
    /**
     * Creates a morphed frame by combining the generated waveform with sample data.
     * 
     * @param morphType The type of morphing algorithm to use
     * @param baseWaveform The base waveform to morph
     * @param sampleData The sample data to morph with
     * @param frame The frame index
     * @param frameCount The total number of frames
     * @param randomGenerator The random number generator to use
     * @return The morphed waveform
     */
    private float[] createMorphedFrame(MorphType morphType, float[] baseWaveform, float[] sampleData,
                                     int frame, int frameCount, Random randomGenerator) {
        float[] result = new float[baseWaveform.length];
        
        // Calculate morphing amount based on frame position
        float morphAmount = (float) frame / frameCount;
        morphAmount = morphAmount * 0.8f + 0.1f; // Keep within 0.1 - 0.9 range
        
        // Apply randomization to morphAmount
        morphAmount += (randomGenerator.nextFloat() * 0.4f - 0.2f); // +/- 0.2 randomization
        morphAmount = Math.max(0.05f, Math.min(0.95f, morphAmount)); // Clamp to 0.05-0.95
        
        // Resample the sample data if needed
        float[] resampledData = WaveformUtil.resample(sampleData, baseWaveform.length, randomGenerator);
        
        // Apply different morphing algorithms based on the morphType
        switch (morphType) {
            case BLEND:
                // Simple crossfade between generated and sample data
                for (int i = 0; i < result.length; i++) {
                    result[i] = baseWaveform[i] * (1 - morphAmount) + resampledData[i] * morphAmount;
                }
                break;
                
            case ADDITIVE:
                // Add sample data to generated data with scaling
                for (int i = 0; i < result.length; i++) {
                    result[i] = baseWaveform[i] + (resampledData[i] * morphAmount * 0.5f);
                }
                break;
                
            case HARMONIC:
                // Use sample data to influence harmonic content
                float[] harmonicWaveform = new float[result.length];
                int harmonicCount = 1 + randomGenerator.nextInt(5); // 1-5 harmonics
                
                for (int h = 1; h <= harmonicCount; h++) {
                    float harmonicStrength = 1.0f / h;
                    
                    for (int i = 0; i < result.length; i++) {
                        float phase = (float) i / result.length;
                        // Use sample data to influence phase of each harmonic
                        float phaseMod = phase + resampledData[i] * morphAmount * 0.2f;
                        // Wrap phase to [0,1]
                        phaseMod = phaseMod - (float)Math.floor(phaseMod);
                        
                        harmonicWaveform[i] += harmonicStrength * 
                                             (float)Math.sin(2 * Math.PI * h * phaseMod);
                    }
                }
                
                // Mix between base waveform and harmonic waveform
                for (int i = 0; i < result.length; i++) {
                    result[i] = baseWaveform[i] * (1 - morphAmount) + 
                              harmonicWaveform[i] * morphAmount;
                }
                break;
                
            case FOLD:
                // Apply waveshaping to sample data
                float foldThreshold = 0.5f + randomGenerator.nextFloat() * 0.4f; // 0.5-0.9
                
                for (int i = 0; i < result.length; i++) {
                    // Scale input first
                    float input = resampledData[i] * (1.0f + morphAmount * 2.0f);
                    
                    // Apply folding
                    while (Math.abs(input) > foldThreshold) {
                        if (input > foldThreshold) {
                            input = foldThreshold - (input - foldThreshold);
                        } else if (input < -foldThreshold) {
                            input = -foldThreshold - (input + foldThreshold);
                        }
                    }
                    
                    // Mix between base waveform and folded sample
                    result[i] = baseWaveform[i] * (1 - morphAmount) + 
                              (input / foldThreshold) * morphAmount;
                }
                break;
                
            case SPECTRAL:
                // Apply a simple spectral-domain-like effect (simplified)
                // This is a simplified version since true spectral domain processing would 
                // require FFT/IFFT which is more complex
                
                // Create frequency shifts based on morphing amount
                for (int i = 0; i < result.length; i++) {
                    float phase = (float) i / result.length;
                    
                    // Phase shift based on sample data and morphing amount
                    float phaseShift = (resampledData[i] * morphAmount * 0.3f);
                    
                    // Apply modulation
                    result[i] = baseWaveform[i] * (1 - morphAmount);
                    
                    // Add multiple modulated versions of the sample (like spectral bands)
                    for (int band = 1; band <= 3; band++) {
                        float bandPhase = phase * band + phaseShift * band;
                        // Wrap phase
                        bandPhase = bandPhase - (float)Math.floor(bandPhase);
                        
                        // Add contribution from this band
                        float bandAmplitude = morphAmount * (1.0f / band);
                        result[i] += bandAmplitude * (float)Math.sin(2 * Math.PI * bandPhase);
                    }
                }
                break;
                
            default:
                // Default to blend if unknown morphType
                for (int i = 0; i < result.length; i++) {
                    result[i] = baseWaveform[i] * (1 - morphAmount) + resampledData[i] * morphAmount;
                }
        }
        
        return result;
    }
} 