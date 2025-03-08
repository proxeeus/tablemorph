package com.tablemorph.model;

import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * Model class representing a wavetable with its properties and data.
 */
public class Wavetable {
    private final LocalDateTime creationTime;
    private final int sampleCount;
    private final int frameCount;
    private final WaveformType waveformType;
    private final MorphType morphType;
    private final byte[] pcmData;
    private Path filePath;
    
    /**
     * Constructor for a single-cycle wavetable.
     * 
     * @param waveformType The type of waveform in this wavetable
     * @param sampleCount Number of samples in the wavetable
     * @param pcmData The raw PCM data
     */
    public Wavetable(WaveformType waveformType, int sampleCount, byte[] pcmData) {
        this.waveformType = waveformType;
        this.sampleCount = sampleCount;
        this.frameCount = 1; // Single-cycle has one frame
        this.pcmData = pcmData;
        this.morphType = null; // Not applicable for single-cycle
        this.creationTime = LocalDateTime.now();
    }
    
    /**
     * Constructor for a multi-frame wavetable.
     * 
     * @param frameCount Number of frames in the wavetable
     * @param sampleCount Number of samples per frame
     * @param pcmData The raw PCM data
     */
    public Wavetable(int frameCount, int sampleCount, byte[] pcmData) {
        this.waveformType = null; // Not applicable for multi-frame
        this.frameCount = frameCount;
        this.sampleCount = sampleCount;
        this.pcmData = pcmData;
        this.morphType = null; // Not a morph
        this.creationTime = LocalDateTime.now();
    }
    
    /**
     * Constructor for a morphed wavetable.
     * 
     * @param morphType Type of morphing algorithm used
     * @param frameCount Number of frames in the wavetable
     * @param sampleCount Number of samples per frame
     * @param pcmData The raw PCM data
     */
    public Wavetable(MorphType morphType, int frameCount, int sampleCount, byte[] pcmData) {
        this.waveformType = null; // Not applicable for morphed wavetable
        this.morphType = morphType;
        this.frameCount = frameCount;
        this.sampleCount = sampleCount;
        this.pcmData = pcmData;
        this.creationTime = LocalDateTime.now();
    }
    
    public LocalDateTime getCreationTime() {
        return creationTime;
    }
    
    public int getSampleCount() {
        return sampleCount;
    }
    
    public int getFrameCount() {
        return frameCount;
    }
    
    public WaveformType getWaveformType() {
        return waveformType;
    }
    
    public MorphType getMorphType() {
        return morphType;
    }
    
    public byte[] getPcmData() {
        return pcmData;
    }
    
    public Path getFilePath() {
        return filePath;
    }
    
    public void setFilePath(Path filePath) {
        this.filePath = filePath;
    }
    
    /**
     * Determines if this is a single-cycle wavetable.
     * 
     * @return true if single-cycle, false otherwise
     */
    public boolean isSingleCycle() {
        return frameCount == 1 && waveformType != null;
    }
    
    /**
     * Determines if this is a morphed wavetable.
     * 
     * @return true if morphed, false otherwise
     */
    public boolean isMorphed() {
        return morphType != null;
    }
    
    /**
     * Gets the total size of the wavetable in bytes.
     * 
     * @return Size in bytes
     */
    public int getSize() {
        return pcmData.length;
    }
} 