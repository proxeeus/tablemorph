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
import com.tablemorph.model.Wavetable;
import com.tablemorph.util.DirectoryUtil;
import com.tablemorph.util.WavFileUtil;
import com.tablemorph.util.WaveformUtil;

/**
 * Service class for generating multi-frame wavetables.
 */
public class MultiFrameWavetableGenerator {
    private static final String WAVETABLE_DIRECTORY = "wavetables";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    private final Random random = new Random();
    
    /**
     * Generates a random multi-frame wavetable.
     * 
     * @return The path to the generated wavetable
     * @throws IOException If an error occurs during generation
     */
    public Path generateRandomWavetable() throws IOException {
        return generateUniqueWavetable(System.currentTimeMillis());
    }
    
    /**
     * Generates a multi-frame wavetable with a specific seed value.
     * 
     * @param seed The random seed for generation
     * @return The path to the generated wavetable
     * @throws IOException If an error occurs during generation
     */
    public Path generateUniqueWavetable(long seed) throws IOException {
        DirectoryUtil.createWavetableDirectory();
        
        // Create seeded random generator
        Random seededRandom = new Random(seed);
        
        // Generate unique filename
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String filename = "tablemorph_" + timestamp + "_" + Math.abs(seededRandom.nextInt(10000)) + ".wav";
        Path outputPath = Paths.get(WAVETABLE_DIRECTORY, filename);
        
        // Generate wavetable
        Wavetable wavetable = generateMultiFrameWavetable(seededRandom);
        wavetable.setFilePath(outputPath);
        
        // Write to default directory
        WavFileUtil.saveToFile(wavetable.getPcmData(), outputPath);
        
        // If enabled, also save to Vital's directory
        Path vitalPath = DirectoryUtil.getVitalPath(filename);
        if (vitalPath != null) {
            try {
                WavFileUtil.saveToFile(wavetable.getPcmData(), vitalPath);
                System.out.println("✓ Also saved to Vital's wavetables directory: " + vitalPath);
            } catch (IOException e) {
                System.out.println("Warning: Could not save to Vital's directory: " + e.getMessage());
            }
        }
        
        return outputPath;
    }
    
    /**
     * Batch generates multiple multi-frame wavetables.
     * 
     * @param count The number of wavetables to generate
     * @return List of paths to the generated wavetables
     * @throws IOException If an error occurs during generation
     */
    public List<Path> batchGenerateWavetables(int count) throws IOException {
        List<Path> generatedFiles = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            // Generate a unique seed based on current time and iteration
            long seed = System.currentTimeMillis() + i * 1000 + random.nextInt(10000);
            
            // Print progress
            System.out.print("Generating wavetable " + (i+1) + "/" + count + "... ");
            
            // Generate wavetable with unique seed
            Path path = generateUniqueWavetable(seed);
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
     * Generates a multi-frame wavetable object.
     * 
     * @param randomGenerator The random number generator to use
     * @return The generated wavetable
     */
    private Wavetable generateMultiFrameWavetable(Random randomGenerator) {
        int frameCount = GeneratorConfig.getWavetableFrames();
        int sampleCount = GeneratorConfig.getWavetableSamples();
        float[][] frames = new float[frameCount][sampleCount];
        
        // Generate each frame
        for (int frame = 0; frame < frameCount; frame++) {
            frames[frame] = generateRandomWaveform(frame, sampleCount, frameCount, randomGenerator);
            
            // Apply processing
            WaveformUtil.normalize(frames[frame]);
            WaveformUtil.smooth(frames[frame], 3);
        }
        
        // Convert to PCM data
        byte[] pcmData = WavFileUtil.convertToPCM(frames);
        
        return new Wavetable(frameCount, sampleCount, pcmData);
    }
    
    /**
     * Generates a random waveform for a specific frame.
     * 
     * @param frame The frame index
     * @param sampleCount The number of samples per frame
     * @param numFrames The total number of frames
     * @param randomGenerator The random number generator to use
     * @return The generated waveform
     */
    private float[] generateRandomWaveform(int frame, int sampleCount, int numFrames, Random randomGenerator) {
        float[] waveform = new float[sampleCount];
        
        // Frame position influences complexity - middle frames are more complex
        float frameFactor = 1.0f - Math.abs((frame / (float)(numFrames - 1)) * 2 - 1);
        
        // Determine waveform type for this frame
        double waveTypeRoll = randomGenerator.nextDouble();
        
        if (waveTypeRoll < 0.25) {
            generateBasicWaveform(waveform, sampleCount, randomGenerator); // Basic shapes
        } else if (waveTypeRoll < 0.6) {
            generateHarmonicWaveform(waveform, sampleCount, frameFactor, randomGenerator); // Harmonic content
        } else if (waveTypeRoll < 0.85) {
            generateModulatedWaveform(waveform, sampleCount, frameFactor, randomGenerator); // Modulated
        } else {
            generateComplexWaveform(waveform, sampleCount, frameFactor, randomGenerator); // Complex
        }
        
        // Apply additional processing
        if (randomGenerator.nextDouble() < 0.3) {
            applyWaveShaping(waveform, randomGenerator);
        }
        
        return waveform;
    }
    
    /**
     * Generates a basic waveform (sine, square, saw, triangle).
     */
    private void generateBasicWaveform(float[] waveform, int sampleCount, Random randomGenerator) {
        int type = randomGenerator.nextInt(4);
        
        for (int i = 0; i < sampleCount; i++) {
            float phase = (float) i / sampleCount;
            
            switch (type) {
                case 0: // Sine
                    waveform[i] = (float) Math.sin(2 * Math.PI * phase);
                    break;
                case 1: // Square
                    float pulseWidth = 0.3f + randomGenerator.nextFloat() * 0.4f;
                    waveform[i] = phase < pulseWidth ? 1.0f : -1.0f;
                    break;
                case 2: // Saw
                    waveform[i] = 2 * phase - 1;
                    break;
                case 3: // Triangle
                    if (phase < 0.5f) {
                        waveform[i] = -1.0f + 4.0f * phase;
                    } else {
                        waveform[i] = 3.0f - 4.0f * phase;
                    }
                    break;
            }
        }
    }
    
    /**
     * Generates a harmonic-rich waveform.
     */
    private void generateHarmonicWaveform(float[] waveform, int sampleCount, float complexity, Random randomGenerator) {
        // Determine number of harmonics
        int maxHarmonics = 3 + (int)(30 * complexity);
        
        // Generate harmonic amplitudes
        float[] harmonicAmps = new float[maxHarmonics];
        float totalAmp = 0;
        
        for (int h = 0; h < maxHarmonics; h++) {
            // Randomize harmonic amplitudes with natural decay
            float baseAmp = 1.0f / (h + 1); 
            harmonicAmps[h] = baseAmp * (0.5f + randomGenerator.nextFloat());
            
            // Apply additional amplitude shaping
            if (randomGenerator.nextDouble() < 0.4) {
                // Sometimes create notches or peaks
                if (h % (2 + randomGenerator.nextInt(3)) == 0) {
                    harmonicAmps[h] *= 2.0f;
                } else if (randomGenerator.nextDouble() < 0.3) {
                    harmonicAmps[h] *= 0.2f;
                }
            }
            
            totalAmp += harmonicAmps[h];
        }
        
        // Normalize harmonic amplitudes
        for (int h = 0; h < maxHarmonics; h++) {
            harmonicAmps[h] /= totalAmp;
        }
        
        // Build waveform from harmonics
        for (int i = 0; i < sampleCount; i++) {
            float phase = (float) i / sampleCount;
            waveform[i] = 0;
            
            for (int h = 0; h < maxHarmonics; h++) {
                int harmonicNumber = h + 1;
                float phaseOffset = randomGenerator.nextFloat() * 0.2f; // Add some phase variation
                waveform[i] += harmonicAmps[h] * (float) Math.sin(2 * Math.PI * harmonicNumber * (phase + phaseOffset));
            }
        }
    }
    
    /**
     * Generates a modulated waveform (FM, AM, or PM).
     */
    private void generateModulatedWaveform(float[] waveform, int sampleCount, float complexity, Random randomGenerator) {
        int modulationType = randomGenerator.nextInt(3);
        
        float carrier = 1.0f;
        float modulator = 1.0f + randomGenerator.nextInt(5); // 1-5x carrier
        float depth = 0.5f + complexity * randomGenerator.nextFloat() * 5.0f;
        
        for (int i = 0; i < sampleCount; i++) {
            float phase = (float) i / sampleCount;
            float modSignal = (float) Math.sin(2 * Math.PI * phase * modulator);
            
            switch (modulationType) {
                case 0: // FM
                    float modPhase = phase * carrier + modSignal * depth;
                    waveform[i] = (float) Math.sin(2 * Math.PI * modPhase);
                    break;
                    
                case 1: // AM
                    float modAmp = 0.5f + 0.5f * (1.0f + modSignal * depth);
                    waveform[i] = modAmp * (float) Math.sin(2 * Math.PI * phase * carrier);
                    break;
                    
                case 2: // PM
                    float phaseOffset = modSignal * depth * 0.5f;
                    waveform[i] = (float) Math.sin(2 * Math.PI * (phase + phaseOffset) * carrier);
                    break;
            }
        }
    }
    
    /**
     * Generates a complex waveform with multiple layers of modulation.
     */
    private void generateComplexWaveform(float[] waveform, int sampleCount, float complexity, Random randomGenerator) {
        // Multiple modulation sources
        int layers = 2 + randomGenerator.nextInt(3);
        
        // Base frequency values
        float baseFreq = 1.0f;
        float[] modulatorFreqs = new float[layers];
        float[] modulatorDepths = new float[layers];
        
        // Initialize modulation parameters
        for (int l = 0; l < layers; l++) {
            modulatorFreqs[l] = 1.0f + randomGenerator.nextFloat() * 9.0f; // 1-10x
            modulatorDepths[l] = (0.2f + randomGenerator.nextFloat() * 0.8f) * complexity; // 0.2-1.0 scaled by complexity
        }
        
        // Generate complex waveform
        for (int i = 0; i < sampleCount; i++) {
            float phase = (float) i / sampleCount;
            float modAmount = 0;
            
            // Apply multiple layers of modulation
            for (int l = 0; l < layers; l++) {
                float modOutput = (float) Math.sin(2 * Math.PI * phase * modulatorFreqs[l] + modAmount);
                modAmount += modOutput * modulatorDepths[l];
            }
            
            // Final carrier
            waveform[i] = (float) Math.sin(2 * Math.PI * phase * baseFreq + modAmount);
        }
        
        // Apply additional random shaping
        if (randomGenerator.nextDouble() < 0.3) {
            // Sometimes apply some feedback-like distortion
            for (int i = 0; i < sampleCount; i++) {
                waveform[i] = (float) Math.tanh(waveform[i] * (1.0 + complexity));
            }
        }
    }
    
    /**
     * Applies various waveshaping algorithms to a waveform.
     */
    private void applyWaveShaping(float[] waveform, Random randomGenerator) {
        int type = randomGenerator.nextInt(3);
        
        switch (type) {
            case 0: // Soft clipping
                float threshold = 0.3f + randomGenerator.nextFloat() * 0.5f;
                for (int i = 0; i < waveform.length; i++) {
                    if (waveform[i] > threshold) {
                        waveform[i] = threshold + (1 - threshold) * (float) Math.tanh((waveform[i] - threshold) / (1 - threshold));
                    } else if (waveform[i] < -threshold) {
                        waveform[i] = -threshold - (1 - threshold) * (float) Math.tanh((-waveform[i] - threshold) / (1 - threshold));
                    }
                }
                break;
                
            case 1: // Foldback distortion
                float gain = 1.5f + randomGenerator.nextFloat() * 2.0f;
                float foldLevel = 0.9f;
                for (int i = 0; i < waveform.length; i++) {
                    float value = waveform[i] * gain;
                    while (Math.abs(value) > foldLevel) {
                        if (value > foldLevel) {
                            value = foldLevel - (value - foldLevel);
                        } else if (value < -foldLevel) {
                            value = -foldLevel - (value + foldLevel);
                        }
                    }
                    waveform[i] = value;
                }
                break;
                
            case 2: // Sine shaping
                for (int i = 0; i < waveform.length; i++) {
                    waveform[i] = (float) Math.sin(waveform[i] * Math.PI / 2);
                }
                break;
        }
    }
} 