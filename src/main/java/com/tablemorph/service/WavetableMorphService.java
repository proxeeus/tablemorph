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
import java.util.Arrays;

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
        
        // Generate a random waveform with multiple strategies for more variety
        // Make frame position less deterministic for morph behavior
        float frameFactor = (float)frame / frameCount;
        if (randomGenerator.nextFloat() < 0.2f) {
            // 20% chance of inverting the frame factor
            frameFactor = 1.0f - frameFactor;
        }
        
        // Apply a nonlinear curve to frame factor to create more variation
        float curve = 0.5f + randomGenerator.nextFloat() * 1.5f; // 0.5 to 2.0
        if (curve != 1.0f) {
            frameFactor = (float)Math.pow(frameFactor, curve);
        }
        
        // Modulate complexity with frame factor, but add randomization
        float complexityBase = 0.3f + frameFactor * 0.7f; // Base complexity increases with frame position
        float complexityVariance = randomGenerator.nextFloat() * 0.5f; // Up to 50% variance
        float complexity = complexityBase * (1 - complexityVariance) + randomGenerator.nextFloat() * complexityVariance;
        
        // Choose a generation strategy for this frame - use multiple approaches
        int generationStrategy = randomGenerator.nextInt(5);
        
        switch (generationStrategy) {
            case 0: // Additive synthesis with custom harmonic structure
                int harmonicCount = 3 + (int)(25 * complexity); // More harmonics with higher complexity
                
                // Different harmonic structures
                int harmonicStyle = randomGenerator.nextInt(4);
                float[] harmonicWeights = new float[harmonicCount];
                
                // Create different harmonic patterns
                switch (harmonicStyle) {
                    case 0: // Standard harmonic series with natural decay
                        for (int h = 0; h < harmonicCount; h++) {
                            harmonicWeights[h] = 1.0f / (h + 1);
                        }
                        break;
                    case 1: // Odd harmonics emphasized (square-like)
                        for (int h = 0; h < harmonicCount; h++) {
                            int harmonicNumber = h + 1;
                            harmonicWeights[h] = harmonicNumber % 2 == 1 ? 
                                1.0f / harmonicNumber : 
                                0.1f / harmonicNumber;
                        }
                        break;
                    case 2: // Formant-like with resonant peaks
                        for (int h = 0; h < harmonicCount; h++) {
                            int harmonicNumber = h + 1;
                            // Create formant peaks at specific harmonics
                            int formant1 = 3 + randomGenerator.nextInt(3);
                            int formant2 = 8 + randomGenerator.nextInt(5);
                            
                            float distance1 = Math.abs(harmonicNumber - formant1);
                            float distance2 = Math.abs(harmonicNumber - formant2);
                            
                            // Harmonics closer to formants are stronger
                            float resonance = Math.min(
                                1.0f / (1.0f + distance1 * 0.7f), 
                                1.0f / (1.0f + distance2 * 0.5f)
                            );
                            
                            harmonicWeights[h] = resonance / harmonicNumber;
                        }
                        break;
                    case 3: // Random pattern with boosted/cut harmonics
                        for (int h = 0; h < harmonicCount; h++) {
                            // Base weight with natural decay
                            harmonicWeights[h] = 1.0f / (h + 1);
                            
                            // Randomly boost or cut some harmonics
                            if (randomGenerator.nextFloat() < 0.3f) {
                                if (randomGenerator.nextBoolean()) {
                                    // Boost
                                    harmonicWeights[h] *= 2.0f + randomGenerator.nextFloat() * 2.0f;
                                } else {
                                    // Cut
                                    harmonicWeights[h] *= 0.1f + randomGenerator.nextFloat() * 0.3f;
                                }
                            }
                        }
                        break;
                }
                
                // Add randomization to each harmonic
                for (int h = 0; h < harmonicCount; h++) {
                    // Add up to 30% randomization
                    harmonicWeights[h] *= 0.85f + randomGenerator.nextFloat() * 0.3f;
                }
                
                // Generate the waveform
                for (int i = 0; i < sampleCount; i++) {
                    double phase = (double)i / sampleCount;
                    for (int h = 0; h < harmonicCount; h++) {
                        int harmonicNumber = h + 1;
                        
                        // Add random phase offsets for more organic sound
                        double phaseOffset = 0;
                        if (randomGenerator.nextFloat() < 0.4f) {
                            phaseOffset = randomGenerator.nextDouble() * 2.0 * Math.PI;
                        }
                        
                        waveform[i] += harmonicWeights[h] * 
                            (float)Math.sin(2.0 * Math.PI * harmonicNumber * phase + phaseOffset);
                    }
                }
                break;
                
            case 1: // Frequency modulation - for complex spectra
                // FM parameters
                float carrierFreq = 1.0f;
                float modulatorFreq = 1.0f + randomGenerator.nextInt(7); // 1-7x carrier
                float modulationIndex = complexity * (3.0f + randomGenerator.nextFloat() * 5.0f); // Scales with complexity
                
                // Optionally add feedback for more chaotic sounds
                float feedback = 0.0f;
                if (randomGenerator.nextFloat() < 0.3f) {
                    feedback = 0.1f + randomGenerator.nextFloat() * 0.4f; // 0.1-0.5 feedback
                }
                
                // Optionally use multiple modulators for richer sound
                int numModulators = 1;
                if (randomGenerator.nextFloat() < 0.4f) {
                    numModulators = 2 + randomGenerator.nextInt(2); // 2-3 modulators
                }
                
                float[] modFreqs = new float[numModulators];
                float[] modIndices = new float[numModulators];
                
                for (int m = 0; m < numModulators; m++) {
                    // Non-integer ratios create more complex spectra
                    modFreqs[m] = (m + 1) * (0.8f + randomGenerator.nextFloat() * 0.4f);
                    modIndices[m] = modulationIndex / (m + 1) * (0.7f + randomGenerator.nextFloat() * 0.6f);
                }
                
                // Generate FM
                float lastSample = 0;
                
                for (int i = 0; i < sampleCount; i++) {
                    float phase = (float)i / sampleCount;
                    
                    // Calculate modulator outputs
                    float modOutput = 0;
                    for (int m = 0; m < numModulators; m++) {
                        modOutput += (float)Math.sin(2 * Math.PI * phase * modFreqs[m] + feedback * lastSample) 
                            * modIndices[m];
                    }
                    
                    // Apply modulation to carrier
                    float carrierPhase = phase * carrierFreq + modOutput;
                    waveform[i] = (float)Math.sin(2 * Math.PI * carrierPhase);
                    lastSample = waveform[i];
                }
                break;
                
            case 2: // Wavefolding and distortion
                // Generate a base waveform first - usually a simple shape
                int baseWaveType = randomGenerator.nextInt(3);
                for (int i = 0; i < sampleCount; i++) {
                    float phase = (float)i / sampleCount;
                    switch (baseWaveType) {
                        case 0: // Sine
                            waveform[i] = (float)Math.sin(2 * Math.PI * phase);
                            break;
                        case 1: // Triangle
                            waveform[i] = 2.0f * Math.abs(2.0f * (phase - (float)Math.floor(phase + 0.5f))) - 1.0f;
                            break;
                        case 2: // Saw
                            waveform[i] = 2.0f * (phase - (float)Math.floor(phase + 0.5f));
                            break;
                    }
                }
                
                // Apply multiple stages of waveshaping
                int numStages = 1 + randomGenerator.nextInt(3); // 1-3 stages
                
                for (int stage = 0; stage < numStages; stage++) {
                    // Choose different shaping methods for each stage
                    int shapeType = randomGenerator.nextInt(4);
                    float intensity = 1.0f + complexity * (1.0f + randomGenerator.nextFloat() * 3.0f);
                    
                    // Apply the selected waveshaping
                    switch (shapeType) {
                        case 0: // Soft clipping
                            for (int i = 0; i < sampleCount; i++) {
                                waveform[i] = (float)Math.tanh(waveform[i] * intensity);
                            }
                            break;
                            
                        case 1: // Polynomial distortion
                            for (int i = 0; i < sampleCount; i++) {
                                float x = waveform[i];
                                // Apply polynomial shaping: ax^3 + bx^2 + cx + d
                                float a = 0.5f + randomGenerator.nextFloat() * 0.5f;
                                float b = randomGenerator.nextFloat() * 0.3f;
                                float c = 1.0f - a - b; // Keep some of the original
                                
                                waveform[i] = a * x * x * x + b * x * x + c * x;
                                // Rescale if needed
                                if (Math.abs(waveform[i]) > 1.0f) {
                                    waveform[i] /= Math.abs(waveform[i]);
                                }
                            }
                            break;
                            
                        case 2: // Wave folding
                            float threshold = 0.4f + randomGenerator.nextFloat() * 0.4f;
                            // Apply gain to drive the folding
                            for (int i = 0; i < sampleCount; i++) {
                                float value = waveform[i] * intensity;
                                // Fold back when the value exceeds the threshold
                                while (Math.abs(value) > threshold) {
                                    if (value > threshold) {
                                        value = threshold - (value - threshold);
                                    } else {
                                        value = -threshold - (value + threshold);
                                    }
                                }
                                waveform[i] = value;
                            }
                            break;
                            
                        case 3: // Bit crushing / quantization
                            int bits = 2 + (int)(6 * (1 - complexity)); // More complex = fewer bits
                            float levels = (float)Math.pow(2, bits);
                            for (int i = 0; i < sampleCount; i++) {
                                waveform[i] = Math.round(waveform[i] * levels) / levels;
                            }
                            break;
                    }
                }
                break;
                
            case 3: // Phase distortion and reshaping
                // Generate a base oscillator
                boolean useSine = randomGenerator.nextBoolean();
                
                // Create a phase distortion function
                float[] phaseDistortion = new float[sampleCount];
                int distortionType = randomGenerator.nextInt(4);
                
                switch (distortionType) {
                    case 0: // Exponential phase distortion
                        float exp = 0.5f + randomGenerator.nextFloat() * 2.5f;
                        for (int i = 0; i < sampleCount; i++) {
                            float phase = (float)i / sampleCount;
                            phaseDistortion[i] = (float)Math.pow(phase, exp);
                        }
                        break;
                        
                    case 1: // S-curve (sigmoid) phase distortion
                        float steepness = 3.0f + randomGenerator.nextFloat() * 7.0f;
                        for (int i = 0; i < sampleCount; i++) {
                            float phase = (float)i / sampleCount;
                            phaseDistortion[i] = (float)(1.0 / (1.0 + Math.exp(-steepness * (phase - 0.5))));
                        }
                        break;
                        
                    case 2: // Multi-segment linear phase distortion
                        int segments = 2 + randomGenerator.nextInt(4); // 2-5 segments
                        float[] segmentPositions = new float[segments + 1];
                        segmentPositions[0] = 0;
                        segmentPositions[segments] = 1;
                        
                        // Create random break points
                        for (int s = 1; s < segments; s++) {
                            segmentPositions[s] = randomGenerator.nextFloat();
                        }
                        Arrays.sort(segmentPositions);
                        
                        // Create segment rates
                        float[] segmentRates = new float[segments];
                        float totalRate = 0;
                        for (int s = 0; s < segments; s++) {
                            segmentRates[s] = 0.1f + randomGenerator.nextFloat() * 3.0f;
                            totalRate += segmentRates[s] * (segmentPositions[s+1] - segmentPositions[s]);
                        }
                        
                        // Normalize to ensure phase spans from 0 to 1
                        for (int s = 0; s < segments; s++) {
                            segmentRates[s] /= totalRate;
                        }
                        
                        // Generate distorted phase
                        for (int i = 0; i < sampleCount; i++) {
                            float phase = (float)i / sampleCount;
                            // Find which segment this phase belongs to
                            int segment = 0;
                            while (segment < segments && phase > segmentPositions[segment+1]) {
                                segment++;
                            }
                            
                            // Calculate distorted phase within this segment
                            float segmentPhase = (phase - segmentPositions[segment]) / 
                                                (segmentPositions[segment+1] - segmentPositions[segment]);
                            float accumulatedPhase = 0;
                            for (int s = 0; s < segment; s++) {
                                accumulatedPhase += (segmentPositions[s+1] - segmentPositions[s]) * segmentRates[s];
                            }
                            accumulatedPhase += segmentPhase * (segmentPositions[segment+1] - segmentPositions[segment]) * 
                                               segmentRates[segment];
                            
                            phaseDistortion[i] = accumulatedPhase;
                        }
                        break;
                        
                    case 3: // Sinusoidal phase distortion
                        int cycles = 1 + randomGenerator.nextInt(5); // 1-5 cycles of modulation
                        float depth = 0.1f + randomGenerator.nextFloat() * 0.4f; // How much modulation
                        
                        for (int i = 0; i < sampleCount; i++) {
                            float phase = (float)i / sampleCount;
                            phaseDistortion[i] = phase + depth * (float)Math.sin(2 * Math.PI * phase * cycles);
                            // Normalize to 0-1 range
                            phaseDistortion[i] = phaseDistortion[i] - (float)Math.floor(phaseDistortion[i]);
                        }
                        break;
                }
                
                // Normalize phase distortion to ensure full 0-1 range
                float minPhase = 1.0f;
                float maxPhase = 0.0f;
                for (int i = 0; i < sampleCount; i++) {
                    minPhase = Math.min(minPhase, phaseDistortion[i]);
                    maxPhase = Math.max(maxPhase, phaseDistortion[i]);
                }
                float phaseRange = maxPhase - minPhase;
                
                // Apply phase distortion to oscillator
                for (int i = 0; i < sampleCount; i++) {
                    // Normalize phase to 0-1
                    float normalizedPhase = (phaseDistortion[i] - minPhase) / phaseRange;
                    
                    // Map to oscillator - either sine or saw
                    if (useSine) {
                        waveform[i] = (float)Math.sin(2 * Math.PI * normalizedPhase);
                    } else {
                        waveform[i] = 2.0f * normalizedPhase - 1.0f;
                    }
                }
                break;
                
            default: // Complex spectrum with noise
                // Start with a harmonic base
                int baseHarmonics = 3 + (int)(10 * complexity);
                for (int i = 0; i < sampleCount; i++) {
                    float phase = (float)i / sampleCount;
                    for (int h = 1; h <= baseHarmonics; h++) {
                        float harmonicAmp = (float)(Math.pow(0.8, h-1) / h);
                        waveform[i] += harmonicAmp * (float)Math.sin(2 * Math.PI * h * phase);
                    }
                }
                
                // Add controlled noise component
                float noiseAmount = complexity * 0.3f * (randomGenerator.nextFloat() * 0.5f + 0.5f);
                
                if (noiseAmount > 0.01f) {
                    // Generate noise
                    float[] noise = new float[sampleCount];
                    for (int i = 0; i < sampleCount; i++) {
                        noise[i] = randomGenerator.nextFloat() * 2.0f - 1.0f;
                    }
                    
                    // Filter the noise for more musicality
                    int filterPasses = 1 + randomGenerator.nextInt(3);
                    for (int pass = 0; pass < filterPasses; pass++) {
                        float[] filtered = new float[sampleCount];
                        for (int i = 0; i < sampleCount; i++) {
                            float sum = 0;
                            int windowSize = 2 + randomGenerator.nextInt(5);
                            for (int j = -windowSize; j <= windowSize; j++) {
                                int idx = (i + j + sampleCount) % sampleCount;
                                sum += noise[idx];
                            }
                            filtered[i] = sum / (2 * windowSize + 1);
                        }
                        System.arraycopy(filtered, 0, noise, 0, sampleCount);
                    }
                    
                    // Mix noise with harmonic content
                    for (int i = 0; i < sampleCount; i++) {
                        waveform[i] = waveform[i] * (1 - noiseAmount) + noise[i] * noiseAmount;
                    }
                }
                
                // Apply some modulation
                if (randomGenerator.nextFloat() < 0.7f) {
                    int modType = randomGenerator.nextInt(3);
                    float modDepth = 0.2f + randomGenerator.nextFloat() * 0.6f;
                    
                    for (int i = 0; i < sampleCount; i++) {
                        float phase = (float) i / sampleCount;
                        float mod = (float) Math.sin(2 * Math.PI * phase * (2 + randomGenerator.nextInt(5)));
                        
                        switch (modType) {
                            case 0: // Amplitude modulation
                                waveform[i] *= 1.0f + mod * modDepth;
                                break;
                            case 1: // Filter-like effect
                                int offset = (int)(mod * modDepth * 10);
                                int idx = Math.max(0, Math.min(sampleCount-1, i + offset));
                                waveform[i] = 0.7f * waveform[i] + 0.3f * waveform[idx];
                                break;
                            case 2: // Phase distortion
                                int newIdx = (int)(i + mod * modDepth * 20) % sampleCount;
                                if (newIdx < 0) newIdx += sampleCount;
                                float[] tempWaveform = new float[sampleCount];
                                System.arraycopy(waveform, 0, tempWaveform, 0, sampleCount);
                                waveform[i] = tempWaveform[newIdx];
                                break;
                        }
                    }
                }
        }
        
        // Apply some random modulation
        if (randomGenerator.nextFloat() < 0.5f) {
            int modType = randomGenerator.nextInt(3);
            float modDepth = 0.1f + randomGenerator.nextFloat() * 0.5f;
            
            float[] temp = new float[sampleCount];
            System.arraycopy(waveform, 0, temp, 0, sampleCount);
            
            for (int i = 0; i < sampleCount; i++) {
                float phase = (float) i / sampleCount;
                float mod = (float) Math.sin(2 * Math.PI * phase * (1 + randomGenerator.nextInt(8)));
                
                switch (modType) {
                    case 0: // Amplitude modulation
                        waveform[i] *= 1.0f + mod * modDepth;
                        break;
                    case 1: // Sample-and-hold like effect
                        if (randomGenerator.nextFloat() < 0.1f) {
                            int holdLength = 2 + randomGenerator.nextInt(10);
                            float holdValue = waveform[i];
                            for (int j = 0; j < holdLength && i + j < sampleCount; j++) {
                                waveform[i + j] = holdValue;
                            }
                            i += holdLength - 1;
                        }
                        break;
                    case 2: // Cross-modulation with itself
                        int offset = randomGenerator.nextInt(sampleCount);
                        waveform[i] = waveform[i] * (1 - modDepth) + temp[(i + offset) % sampleCount] * modDepth;
                        break;
                }
            }
        }
        
        // Ensure we're not clipping
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
        
        // Make morphing amount less predictable by adding significant randomization
        // This ensures even consecutive frames can have very different morph characteristics
        float rawMorphAmount = (float) frame / frameCount;
        float morphVariance = 0.3f + randomGenerator.nextFloat() * 0.4f; // 0.3-0.7 variance range
        float morphAmount = rawMorphAmount * (1 - morphVariance) + randomGenerator.nextFloat() * morphVariance;
        
        // Apply further randomization - possibility of inverting the morph direction for some frames
        if (randomGenerator.nextFloat() < 0.15f) { // 15% chance
            morphAmount = 1.0f - morphAmount; // Completely invert morph direction
        }
        
        // Keep within sensible range but with more variance than before
        morphAmount = 0.05f + morphAmount * 0.9f;
        
        // Additional random modifiers for more unpredictable morphing
        float intensityFactor = 0.5f + randomGenerator.nextFloat() * 1.5f; // 0.5x to 2.0x intensity
        float phaseFactor = randomGenerator.nextFloat() * 0.5f; // 0-0.5 phase shifting
        
        // Resample the sample data if needed with variable resampling approach
        float[] resampledData;
        if (randomGenerator.nextFloat() < 0.3f) {
            // Time-stretched version (30% chance)
            int stretchFactor = randomGenerator.nextInt(4) + 1; // 1-4x
            int resampleLength = Math.min(baseWaveform.length * stretchFactor, sampleData.length);
            float[] tempData = new float[resampleLength];
            System.arraycopy(sampleData, 0, tempData, 0, resampleLength);
            resampledData = WaveformUtil.resample(tempData, baseWaveform.length, randomGenerator);
        } else {
            // Normal resampling
            resampledData = WaveformUtil.resample(sampleData, baseWaveform.length, randomGenerator);
        }
        
        // Apply different morphing algorithms based on the morphType with enhanced randomization
        switch (morphType) {
            case BLEND:
                // Enhanced crossfade with non-linear blending
                float blendCurve = randomGenerator.nextFloat() < 0.5f ? 2.0f : 0.5f; // Exponential or logarithmic
                
                for (int i = 0; i < result.length; i++) {
                    // Create a modulated morph factor that varies across the waveform
                    float modulatedMorph = morphAmount;
                    
                    // Apply non-linear morphing curve
                    if (blendCurve != 1.0f) {
                        modulatedMorph = (float) Math.pow(modulatedMorph, blendCurve);
                    }
                    
                    // Apply sinusoidal modulation to morph amount in some cases
                    if (randomGenerator.nextFloat() < 0.4f) {
                        float modRate = 1.0f + randomGenerator.nextInt(8); // 1-8 Hz modulation
                        modulatedMorph *= 0.7f + 0.3f * (float)Math.sin(2 * Math.PI * i / result.length * modRate);
                    }
                    
                    result[i] = baseWaveform[i] * (1 - modulatedMorph) + resampledData[i] * modulatedMorph;
                }
                
                // Random phase offset for more variety (25% chance)
                if (randomGenerator.nextFloat() < 0.25f) {
                    int offset = randomGenerator.nextInt(result.length);
                    float[] temp = new float[result.length];
                    System.arraycopy(result, 0, temp, 0, result.length);
                    
                    for (int i = 0; i < result.length; i++) {
                        result[i] = temp[(i + offset) % result.length];
                    }
                }
                break;
                
            case ADDITIVE:
                // Enhanced additive morphing with variable scaling and selective harmonics
                for (int i = 0; i < result.length; i++) {
                    // Base mixing
                    result[i] = baseWaveform[i];
                    
                    // Add modulated sample with variable gain
                    float gain = morphAmount * intensityFactor;
                    
                    // Randomize which parts of the sample contribute more
                    float sampleInfluence = resampledData[i] * gain;
                    
                    // Apply frequency-dependent morphing (more high-freq content in later frames)
                    if (randomGenerator.nextFloat() < 0.6f) {
                        int harmonicFactor = 1 + randomGenerator.nextInt(5); // 1-5x harmonic content
                        int harmonicIndex = (i * harmonicFactor) % result.length;
                        sampleInfluence = (sampleInfluence + resampledData[harmonicIndex] * gain * 0.7f) * 0.6f;
                    }
                    
                    result[i] += sampleInfluence;
                }
                break;
                
            case HARMONIC:
                // More varied harmonic interaction between base and sample
                float[] harmonicWaveform = new float[result.length];
                
                // Use more harmonics with more variability
                int minHarmonics = 1 + randomGenerator.nextInt(3);
                int maxHarmonics = 5 + randomGenerator.nextInt(12);
                int harmonicCount = minHarmonics + randomGenerator.nextInt(maxHarmonics - minHarmonics + 1);
                
                // Different harmonic series types
                int harmonicType = randomGenerator.nextInt(4);
                
                for (int h = 1; h <= harmonicCount; h++) {
                    float harmonicStrength;
                    
                    switch (harmonicType) {
                        case 0: // Regular harmonics
                            harmonicStrength = 1.0f / h;
                            break;
                        case 1: // Odd harmonics emphasized
                            harmonicStrength = h % 2 == 1 ? 1.0f / h : 0.2f / h;
                            break;
                        case 2: // Even harmonics emphasized
                            harmonicStrength = h % 2 == 0 ? 1.0f / h : 0.2f / h;
                            break;
                        default: // Custom decay
                            float decay = 0.7f + randomGenerator.nextFloat() * 0.25f;
                            harmonicStrength = (float) Math.pow(decay, h-1);
                    }
                    
                    // Add random amplitude variance to each harmonic
                    harmonicStrength *= 0.7f + randomGenerator.nextFloat() * 0.6f;
                    
                    for (int i = 0; i < result.length; i++) {
                        float phase = (float) i / result.length;
                        
                        // Use sample data to influence phase and/or amplitude
                        float sampleInfluence = resampledData[i] * morphAmount;
                        float phaseMod = phase * h + sampleInfluence * phaseFactor;
                        
                        // Wrap phase
                        phaseMod = phaseMod - (float)Math.floor(phaseMod);
                        
                        // Apply amplitude modulation from the sample
                        float ampMod = 1.0f;
                        if (randomGenerator.nextFloat() < 0.5f) {
                            ampMod = 0.7f + (1.0f + resampledData[(i*h) % result.length]) * 0.3f;
                        }
                        
                        harmonicWaveform[i] += harmonicStrength * ampMod * 
                                             (float)Math.sin(2 * Math.PI * phaseMod);
                    }
                }
                
                // Mix between base waveform and harmonic waveform with variable blending
                float harmonicMix = 0.2f + morphAmount * 0.8f;
                
                // Apply selective frequency masking
                if (randomGenerator.nextFloat() < 0.3f) {
                    for (int i = 0; i < result.length; i++) {
                        // Create a frequency mask that emphasizes certain frequency regions
                        float freqMask = 0.5f + 0.5f * (float)Math.sin(i * 8.0f / result.length * Math.PI);
                        result[i] = baseWaveform[i] * (1 - harmonicMix * freqMask) + 
                                  harmonicWaveform[i] * harmonicMix * freqMask;
                    }
                } else {
                    // Standard mixing
                    for (int i = 0; i < result.length; i++) {
                        result[i] = baseWaveform[i] * (1 - harmonicMix) + harmonicWaveform[i] * harmonicMix;
                    }
                }
                break;
                
            case FOLD:
                // Enhanced waveshaping with dynamic thresholds and variable folding
                float baseFoldThreshold = 0.3f + randomGenerator.nextFloat() * 0.6f; // 0.3-0.9 range
                
                // Different folding behaviors
                int foldType = randomGenerator.nextInt(3);
                
                // Apply varied scaling based on frame position
                float inputScale = 1.0f + morphAmount * intensityFactor;
                
                for (int i = 0; i < result.length; i++) {
                    // Dynamic threshold that changes across the waveform
                    float threshold = baseFoldThreshold;
                    if (randomGenerator.nextFloat() < 0.4f) {
                        // Modulate the threshold for some wavetables
                        threshold *= 0.7f + 0.6f * (float)Math.sin(i * 4.0f / result.length * Math.PI);
                    }
                    
                    // Scale input with sample-influenced gain
                    float input = resampledData[i] * inputScale;
                    
                    // Apply folding based on type
                    switch (foldType) {
                        case 0: // Standard reflection folding
                            while (Math.abs(input) > threshold) {
                                if (input > threshold) {
                                    input = threshold - (input - threshold);
                                } else {
                                    input = -threshold - (input + threshold);
                                }
                            }
                            break;
                        case 1: // Asymmetric folding
                            while (input > threshold || input < -threshold*0.7f) {
                                if (input > threshold) {
                                    input = threshold - (input - threshold) * 0.8f;
                                } else if (input < -threshold*0.7f) {
                                    input = -threshold*0.7f - (input + threshold*0.7f) * 1.2f;
                                }
                            }
                            break;
                        case 2: // Tanh saturation instead of folding
                            input = threshold * (float)Math.tanh(input / threshold);
                            break;
                    }
                    
                    // Mix folded sample with base waveform
                    float foldMix = 0.1f + morphAmount * 0.9f;
                    result[i] = baseWaveform[i] * (1 - foldMix) + (input / threshold) * foldMix;
                }
                
                // Apply additional waveshaping to the entire waveform (40% chance)
                if (randomGenerator.nextFloat() < 0.4f) {
                    for (int i = 0; i < result.length; i++) {
                        result[i] = (float)Math.tanh(result[i] * (1.0f + morphAmount));
                    }
                }
                break;
                
            case SPECTRAL:
                // Enhanced spectral-like processing
                float baseAmount = morphAmount * 0.5f;
                float[] spectralResult = new float[result.length];
                
                // First, get base contribution
                for (int i = 0; i < result.length; i++) {
                    spectralResult[i] = baseWaveform[i] * (1.0f - baseAmount);
                }
                
                // Create multiple spectral "bands" with different processing
                int numBands = 2 + randomGenerator.nextInt(4); // 2-5 bands
                float[] bandFreqs = new float[numBands];
                float[] bandAmps = new float[numBands];
                float[] bandPhases = new float[numBands];
                
                // Set up band parameters
                float totalAmp = 0f;
                for (int b = 0; b < numBands; b++) {
                    // Frequencies spread across spectrum with some randomness
                    bandFreqs[b] = (1 + b) * (0.8f + randomGenerator.nextFloat() * 0.4f);
                    
                    // Amplitudes follow natural decay but with randomization
                    bandAmps[b] = morphAmount * (1.0f / (b+1)) * (0.7f + randomGenerator.nextFloat() * 0.6f);
                    totalAmp += bandAmps[b];
                    
                    // Random phase offsets
                    bandPhases[b] = randomGenerator.nextFloat() * 2.0f * (float)Math.PI;
                }
                
                // Normalize band amplitudes
                for (int b = 0; b < numBands; b++) {
                    bandAmps[b] /= totalAmp;
                }
                
                // Apply spectral processing for each band
                for (int i = 0; i < result.length; i++) {
                    float phase = (float) i / result.length;
                    
                    // Get sample influence
                    float sampleValue = resampledData[i];
                    
                    // For each band, create a different spectral contribution
                    for (int b = 0; b < numBands; b++) {
                        float bandPhase = phase * bandFreqs[b] + sampleValue * phaseFactor + bandPhases[b];
                        // Wrap phase
                        bandPhase = bandPhase - (float)Math.floor(bandPhase);
                        
                        // Create a spectral component with sample-influenced amplitude
                        float bandAmp = bandAmps[b] * (1.0f + sampleValue * intensityFactor * 0.5f);
                        
                        // Different waveshapes for different bands
                        float bandValue;
                        int waveType = (b + randomGenerator.nextInt(3)) % 3;
                        switch (waveType) {
                            case 0:
                                bandValue = (float)Math.sin(2 * Math.PI * bandPhase); // Sine
                                break;
                            case 1:
                                bandValue = 2f * (bandPhase - (float)Math.floor(bandPhase + 0.5f)); // Triangle
                                break;
                            default:
                                bandValue = bandPhase < 0.5f ? 1f : -1f; // Square
                        }
                        
                        spectralResult[i] += bandValue * bandAmp;
                    }
                }
                
                // Final result is the spectral processed signal
                System.arraycopy(spectralResult, 0, result, 0, result.length);
                
                // Randomly apply further non-linear processing (20% chance)
                if (randomGenerator.nextFloat() < 0.2f) {
                    for (int i = 0; i < result.length; i++) {
                        result[i] = (float)Math.tanh(result[i] * (1.0f + morphAmount * 0.5f));
                    }
                }
                break;
                
            default:
                // Default to enhanced blend if unknown morphType
                for (int i = 0; i < result.length; i++) {
                    float variableMorph = morphAmount * (0.8f + 0.4f * (float)Math.sin(i * 8.0f / result.length * Math.PI));
                    result[i] = baseWaveform[i] * (1 - variableMorph) + resampledData[i] * variableMorph;
                }
        }
        
        // Add some final random processing (30% chance)
        if (randomGenerator.nextFloat() < 0.3f) {
            int effectType = randomGenerator.nextInt(3);
            
            switch (effectType) {
                case 0: // Phase modulation
                    float[] tempResult = new float[result.length];
                    System.arraycopy(result, 0, tempResult, 0, result.length);
                    
                    // Phase modulation amount
                    float pmAmount = 0.1f + randomGenerator.nextFloat() * 0.4f;
                    
                    for (int i = 0; i < result.length; i++) {
                        float phase = (float) i / result.length;
                        // Calculate phase modification
                        float phaseShift = pmAmount * tempResult[(i + result.length/4) % result.length];
                        // Apply phase modulation by sampling at a different point
                        int newPos = (int)(i + phaseShift * result.length) % result.length;
                        if (newPos < 0) newPos += result.length;
                        result[i] = tempResult[newPos];
                    }
                    break;
                    
                case 1: // Spectral tilt
                    // Emphasize low or high frequencies
                    boolean emphasizeHigh = randomGenerator.nextBoolean();
                    float tiltAmount = 0.3f + randomGenerator.nextFloat() * 0.4f;
                    
                    for (int i = 0; i < result.length; i++) {
                        float position = (float) i / result.length;
                        float tiltFactor = emphasizeHigh ? position : (1.0f - position);
                        result[i] = result[i] * (1.0f - tiltAmount + tiltAmount * tiltFactor);
                    }
                    break;
                    
                case 2: // Bit reduction/quantization effect
                    int bits = 3 + randomGenerator.nextInt(6); // 3-8 bits
                    float levels = (float)Math.pow(2, bits);
                    
                    for (int i = 0; i < result.length; i++) {
                        // Quantize the signal
                        result[i] = Math.round(result[i] * levels) / levels;
                    }
                    break;
            }
        }
        
        // Normalize before returning to ensure full dynamic range
        WaveformUtil.normalize(result);
        
        return result;
    }
} 