package com.tablemorph.service;

import java.util.Random;
import java.util.Arrays;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

/**
 * Experimental waveform generator that provides advanced and unusual single-cycle waveforms.
 * These algorithms create surprising and unique timbres, complementing the standard waveform generator.
 */
public class ExperimentalWaveformGenerator {
    
    private final Random random = new Random();
    
    /**
     * Generates waveform data using one of several experimental algorithms.
     * 
     * @param sampleCount Number of samples in the waveform
     * @return Array of sample data for the waveform
     */
    public double[] generateExperimentalWaveform(int sampleCount) {
        // Randomly select one of the experimental algorithms
        int algorithmType = random.nextInt(6);
        
        switch (algorithmType) {
            case 0: return generateFractalWaveform(sampleCount);
            case 1: return generateChaoticWaveform(sampleCount);
            case 2: return generateSpectralMorphing(sampleCount);
            case 3: return generateWaveFolding(sampleCount);
            case 4: return generateFeedbackFM(sampleCount);
            case 5: return generateLayeredHarmonics(sampleCount);
            default: return generateFractalWaveform(sampleCount);
        }
    }
    
    /**
     * Generates a fractal waveform using recursive subdivision with random variation.
     * Creates organic, evolving textures with self-similar properties.
     */
    private double[] generateFractalWaveform(int sampleCount) {
        double[] waveform = new double[sampleCount];
        
        // Initialize with a simple shape
        for (int i = 0; i < sampleCount; i++) {
            waveform[i] = Math.sin(2 * Math.PI * i / sampleCount);
        }
        
        // Apply fractal iterations
        int iterations = 3 + random.nextInt(3);
        double roughness = 0.2 + random.nextDouble() * 0.6;
        
        for (int iter = 0; iter < iterations; iter++) {
            double[] newWave = Arrays.copyOf(waveform, sampleCount);
            
            for (int i = 0; i < sampleCount; i++) {
                // Add self-similar detail at different scales
                double detail = 0;
                for (int j = 1; j <= 4; j++) {
                    int offset = (i * j) % sampleCount;
                    detail += waveform[offset] * Math.pow(roughness, j);
                }
                
                newWave[i] = waveform[i] + detail * (0.7 / iterations);
            }
            
            waveform = newWave;
        }
        
        // Normalize
        normalize(waveform);
        return waveform;
    }
    
    /**
     * Generates a waveform using chaotic systems like the logistic map or Lorenz attractor.
     * Creates unpredictable yet musical timbres with rich harmonic content.
     */
    private double[] generateChaoticWaveform(int sampleCount) {
        double[] waveform = new double[sampleCount];
        
        // Choose between different chaotic systems
        int chaosType = random.nextInt(3);
        
        if (chaosType == 0) {
            // Logistic map
            double r = 3.7 + random.nextDouble() * 0.29; // Chaotic region
            double x = random.nextDouble();
            
            for (int i = 0; i < sampleCount; i++) {
                // Run the map a few times to settle
                for (int j = 0; j < 10; j++) {
                    x = r * x * (1 - x);
                }
                
                // Save the value
                waveform[i] = x * 2 - 1; // Scale to [-1, 1]
            }
        } else if (chaosType == 1) {
            // Simplified Lorenz attractor projection
            double x = random.nextDouble() * 0.1;
            double y = random.nextDouble() * 0.1;
            double z = random.nextDouble() * 0.1;
            
            double a = 10.0;
            double b = 28.0;
            double c = 8.0 / 3.0;
            double dt = 0.005;
            
            for (int i = 0; i < sampleCount; i++) {
                // Compute next step in Lorenz system
                double dx = a * (y - x) * dt;
                double dy = (x * (b - z) - y) * dt;
                double dz = (x * y - c * z) * dt;
                
                x += dx;
                y += dy;
                z += dz;
                
                // Use x coordinate scaled to [-1, 1]
                waveform[i] = Math.max(-1, Math.min(1, x / 20.0));
            }
        } else {
            // Henon map
            double a = 1.3 + random.nextDouble() * 0.2;
            double b = 0.2 + random.nextDouble() * 0.1;
            double x = random.nextDouble() * 0.1;
            double y = random.nextDouble() * 0.1;
            
            for (int i = 0; i < sampleCount; i++) {
                double newX = 1 - a * x * x + y;
                double newY = b * x;
                
                x = newX;
                y = newY;
                
                waveform[i] = Math.max(-1, Math.min(1, x));
            }
        }
        
        // Filter to make it more musical
        double[] filtered = new double[sampleCount];
        for (int i = 0; i < sampleCount; i++) {
            // Simple lowpass filter
            double sum = 0;
            int windowSize = 5;
            for (int j = -windowSize; j <= windowSize; j++) {
                int idx = (i + j + sampleCount) % sampleCount;
                sum += waveform[idx];
            }
            filtered[i] = sum / (2 * windowSize + 1);
        }
        
        normalize(filtered);
        return filtered;
    }
    
    /**
     * Generates a waveform by morphing between different spectral profiles.
     * Creates evolving timbres with complex harmonic relationships.
     */
    private double[] generateSpectralMorphing(int sampleCount) {
        double[] waveform = new double[sampleCount];
        
        // Generate spectral profile A
        double[] spectrumA = new double[sampleCount / 2];
        for (int i = 0; i < spectrumA.length; i++) {
            // Random amplitude for each harmonic
            if (i < spectrumA.length / 8) { // Focus on lower harmonics
                spectrumA[i] = Math.pow(random.nextDouble(), 1.5);
            } else {
                spectrumA[i] = Math.pow(random.nextDouble(), 3.0) * 0.3;
            }
        }
        
        // Generate spectral profile B
        double[] spectrumB = new double[sampleCount / 2];
        for (int i = 0; i < spectrumB.length; i++) {
            // Different harmonic structure
            if (i % (random.nextInt(3) + 2) == 0) { // Interesting harmonic patterns
                spectrumB[i] = Math.pow(random.nextDouble(), 1.2);
            } else {
                spectrumB[i] = Math.pow(random.nextDouble(), 4.0) * 0.2;
            }
        }
        
        // Apply spectral morphing to create the waveform
        Complex[] complexSpectrum = new Complex[sampleCount];
        for (int i = 0; i < sampleCount; i++) {
            complexSpectrum[i] = Complex.ZERO;
        }
        
        double morphDepth = random.nextDouble(); // Morphing between A and B
        
        // Build spectrum using both spectral profiles
        for (int i = 0; i < sampleCount / 2; i++) {
            if (i > 0) { // Skip DC
                double amplitude = spectrumA[i] * (1 - morphDepth) + spectrumB[i] * morphDepth;
                double phase = random.nextDouble() * 2 * Math.PI; // Random phase
                
                // Add to complex spectrum
                complexSpectrum[i] = Complex.valueOf(amplitude * Math.cos(phase), amplitude * Math.sin(phase));
                // Mirror for real signal
                complexSpectrum[sampleCount - i] = complexSpectrum[i].conjugate();
            }
        }
        
        // Convert to time domain
        FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex[] timeData = transformer.transform(complexSpectrum, TransformType.INVERSE);
        
        // Extract real part
        for (int i = 0; i < sampleCount; i++) {
            waveform[i] = timeData[i].getReal();
        }
        
        normalize(waveform);
        return waveform;
    }
    
    /**
     * Generates a waveform using waveshaping and folding techniques.
     * Creates distinctive timbres with complex harmonics and dynamic character.
     */
    private double[] generateWaveFolding(int sampleCount) {
        double[] waveform = new double[sampleCount];
        
        // Generate base waveform
        int baseType = random.nextInt(3);
        for (int i = 0; i < sampleCount; i++) {
            double phase = (double) i / sampleCount;
            
            if (baseType == 0) {
                waveform[i] = Math.sin(2 * Math.PI * phase);
            } else if (baseType == 1) {
                waveform[i] = 2 * (phase - Math.floor(phase + 0.5)); // Triangle
            } else {
                waveform[i] = phase < 0.5 ? 1 : -1; // Square
            }
            
            // Apply pre-gain to drive the folding
            waveform[i] *= 1.5 + random.nextDouble() * 4.5; 
        }
        
        // Apply wave folding
        int foldStages = 1 + random.nextInt(3); // Number of folding stages
        double threshold = 0.8 + random.nextDouble() * 0.4; // Folding threshold
        
        for (int stage = 0; stage < foldStages; stage++) {
            for (int i = 0; i < sampleCount; i++) {
                // Apply different folding algorithms
                if (Math.abs(waveform[i]) > threshold) {
                    if (random.nextBoolean()) {
                        // Reflection folding
                        double excess = Math.abs(waveform[i]) - threshold;
                        waveform[i] = Math.signum(waveform[i]) * (threshold - excess);
                    } else {
                        // Wrapping folding
                        while (Math.abs(waveform[i]) > threshold) {
                            waveform[i] = Math.signum(waveform[i]) * 2 * threshold - waveform[i];
                        }
                    }
                }
            }
        }
        
        // Apply subtle filter
        double[] filtered = new double[sampleCount];
        double filterAmount = 0.2 + random.nextDouble() * 0.3;
        
        for (int i = 0; i < sampleCount; i++) {
            filtered[i] = waveform[i] * (1 - filterAmount);
            filtered[i] += waveform[(i + 1) % sampleCount] * (filterAmount / 2);
            filtered[i] += waveform[(i - 1 + sampleCount) % sampleCount] * (filterAmount / 2);
        }
        
        normalize(filtered);
        return filtered;
    }
    
    /**
     * Generates a waveform using feedback FM synthesis.
     * Creates rich, evolving timbres with complex modulation patterns.
     */
    private double[] generateFeedbackFM(int sampleCount) {
        double[] waveform = new double[sampleCount];
        
        // FM parameters
        double carrierFreq = 1.0; // Base frequency
        double modulatorFreq = 0.5 + random.nextDouble() * 4.5; // Modulator frequency ratio
        double modulationIndex = 1.0 + random.nextDouble() * 8.0; // Modulation amount
        double feedback = 0.1 + random.nextDouble() * 0.8; // Feedback amount
        
        // Generate FM waveform with feedback
        double lastSample = 0;
        
        for (int i = 0; i < sampleCount; i++) {
            double phase = (double) i / sampleCount;
            
            // Calculate modulator output with feedback
            double modPhase = phase * modulatorFreq + lastSample * feedback;
            double modOutput = Math.sin(2 * Math.PI * modPhase);
            
            // Apply modulation to carrier
            double carrierPhase = phase * carrierFreq + modOutput * modulationIndex;
            waveform[i] = Math.sin(2 * Math.PI * carrierPhase);
            
            // Store for feedback
            lastSample = waveform[i];
        }
        
        // Apply subtle distortion for more harmonics
        if (random.nextBoolean()) {
            double distortionAmount = 0.1 + random.nextDouble() * 0.4;
            for (int i = 0; i < sampleCount; i++) {
                double distortion = Math.tanh(waveform[i] * 3) * distortionAmount;
                waveform[i] = waveform[i] * (1 - distortionAmount) + distortion;
            }
        }
        
        normalize(waveform);
        return waveform;
    }
    
    /**
     * Generates a waveform by layering multiple harmonic series with phase interactions.
     * Creates complex, evolving timbres with rich harmonic content.
     */
    private double[] generateLayeredHarmonics(int sampleCount) {
        double[] waveform = new double[sampleCount];
        
        // Number of harmonic layers
        int layers = 2 + random.nextInt(3);
        
        // For each layer, generate a different harmonic series
        for (int layer = 0; layer < layers; layer++) {
            // Determine layer frequency ratio
            double freqRatio = 1.0;
            if (layer > 0) {
                // Generate non-integer ratios for interesting timbres
                if (random.nextBoolean()) {
                    // Common musical ratios
                    double[] musicalRatios = {1.0, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
                    freqRatio = musicalRatios[random.nextInt(musicalRatios.length)];
                } else {
                    // Slightly detuned ratios for beating effects
                    freqRatio = 1.0 + layer + random.nextDouble() * 0.06 - 0.03;
                }
            }
            
            // Maximum number of harmonics for this layer
            int maxHarmonics = 10 + random.nextInt(20);
            
            // Harmonics decay rate (how quickly higher harmonics diminish)
            double decay = 0.5 + random.nextDouble() * 0.5;
            
            // Phase offset for this layer
            double phaseOffset = random.nextDouble() * 2 * Math.PI;
            
            // Layer amplitude
            double amplitude = 1.0 / layers;
            
            // Generate harmonic content
            for (int i = 0; i < sampleCount; i++) {
                double phase = ((double) i / sampleCount) * freqRatio;
                double sample = 0;
                
                // Sum the harmonics
                for (int h = 1; h <= maxHarmonics; h++) {
                    // Amplitude for this harmonic
                    double harmonicAmp = amplitude * Math.pow(h, -decay);
                    
                    // Even/odd harmonic balance
                    if (h % 2 == 0) {
                        harmonicAmp *= random.nextDouble() * 0.8 + 0.2; // Vary even harmonic content
                    }
                    
                    // Generate the harmonic and add to the sample
                    sample += harmonicAmp * Math.sin(2 * Math.PI * h * phase + phaseOffset * h);
                }
                
                // Add to the waveform
                waveform[i] += sample;
            }
        }
        
        // Apply subtle wave folding for more character
        if (random.nextBoolean()) {
            double foldThreshold = 0.8 + random.nextDouble() * 0.3;
            for (int i = 0; i < sampleCount; i++) {
                if (Math.abs(waveform[i]) > foldThreshold) {
                    waveform[i] = Math.signum(waveform[i]) * (2 * foldThreshold - Math.abs(waveform[i]));
                }
            }
        }
        
        normalize(waveform);
        return waveform;
    }
    
    /**
     * Normalizes a waveform to the range [-1, 1]
     */
    private void normalize(double[] waveform) {
        // Find maximum absolute value
        double maxAbs = 0.0;
        for (double v : waveform) {
            maxAbs = Math.max(maxAbs, Math.abs(v));
        }
        
        // Normalize if maxAbs is not zero
        if (maxAbs > 0.0001) {
            for (int i = 0; i < waveform.length; i++) {
                waveform[i] /= maxAbs;
            }
        }
    }
} 