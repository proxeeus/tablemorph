package com.tablemorph.util;

import java.util.Arrays;
import java.util.Random;

/**
 * Utility class for waveform processing and manipulation.
 */
public class WaveformUtil {
    
    /**
     * Normalizes a waveform to the range [-1, 1].
     * 
     * @param waveform The waveform to normalize
     */
    public static void normalize(float[] waveform) {
        float maxAbs = 0.0f;
        
        // Find maximum absolute value
        for (float sample : waveform) {
            maxAbs = Math.max(maxAbs, Math.abs(sample));
        }
        
        // If the waveform is not already normalized, normalize it
        if (maxAbs > 0.00001f && Math.abs(maxAbs - 1.0f) > 0.00001f) {
            for (int i = 0; i < waveform.length; i++) {
                waveform[i] /= maxAbs;
            }
        }
    }
    
    /**
     * Applies smoothing to a waveform.
     * 
     * @param waveform The waveform to smooth
     * @param windowSize Size of the smoothing window
     */
    public static void smooth(float[] waveform, int windowSize) {
        int length = waveform.length;
        float[] smoothed = new float[length];
        
        // Apply moving average filter
        for (int i = 0; i < length; i++) {
            float sum = 0.0f;
            float weight = 0.0f;
            
            for (int j = -windowSize; j <= windowSize; j++) {
                int idx = (i + j + length) % length; // Wrap around for loop points
                float w = 1.0f - Math.abs(j) / (float)(windowSize + 1);
                sum += waveform[idx] * w;
                weight += w;
            }
            
            smoothed[i] = sum / weight;
        }
        
        // Copy smoothed data back to original array
        System.arraycopy(smoothed, 0, waveform, 0, length);
    }
    
    /**
     * Smooths the loop points of a waveform to prevent clicking.
     * 
     * @param waveform The waveform to process
     */
    public static void smoothLoopPoints(float[] waveform) {
        int length = waveform.length;
        if (length < 3) return;
        
        // Calculate crossfade window size (about 1% of the total length)
        int windowSize = Math.max(2, length / 100);
        
        // Apply crossfade
        for (int i = 0; i < windowSize; i++) {
            float blend = (float) i / windowSize;
            int endPos = length - windowSize + i;
            
            // Crossfade between start and end
            float startSample = waveform[i];
            float endSample = waveform[endPos];
            
            float blended = startSample * blend + endSample * (1.0f - blend);
            waveform[i] = blended;
            waveform[endPos] = blended;
        }
    }
    
    /**
     * Resamples a waveform to a target length.
     * 
     * @param input The input waveform
     * @param targetLength The desired length
     * @param random Random number generator for interpolation variation
     * @return The resampled waveform
     */
    public static float[] resample(float[] input, int targetLength, Random random) {
        if (input.length == targetLength) {
            return Arrays.copyOf(input, input.length);
        }
        
        float[] output = new float[targetLength];
        double ratio = (double) input.length / targetLength;
        
        // Choose interpolation method - linear is faster, cubic is higher quality
        boolean useCubic = random != null && random.nextDouble() < 0.7; // 70% chance of cubic
        
        for (int i = 0; i < targetLength; i++) {
            double pos = i * ratio;
            int pos1 = (int) pos;
            double frac = pos - pos1;
            
            if (useCubic) {
                // Cubic interpolation (higher quality)
                int pos0 = Math.max(0, pos1 - 1);
                int pos2 = Math.min(input.length - 1, pos1 + 1);
                int pos3 = Math.min(input.length - 1, pos1 + 2);
                
                output[i] = (float) cubicInterpolate(
                    input[pos0], input[pos1], input[pos2], input[pos3], frac);
            } else {
                // Linear interpolation (faster)
                int pos2 = Math.min(input.length - 1, pos1 + 1);
                output[i] = (float) ((1.0 - frac) * input[pos1] + frac * input[pos2]);
            }
        }
        
        return output;
    }
    
    /**
     * Performs cubic interpolation between samples.
     */
    private static double cubicInterpolate(double y0, double y1, double y2, double y3, double mu) {
        double mu2 = mu * mu;
        double a0 = y3 - y2 - y0 + y1;
        double a1 = y0 - y1 - a0;
        double a2 = y2 - y0;
        double a3 = y1;
        
        return a0 * mu * mu2 + a1 * mu2 + a2 * mu + a3;
    }
} 