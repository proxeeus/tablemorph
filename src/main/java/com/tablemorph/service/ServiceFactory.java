package com.tablemorph.service;

/**
 * Factory class for creating service instances.
 * Uses the Singleton pattern to ensure only one instance of each service exists.
 */
public class ServiceFactory {
    // Singleton instance
    private static ServiceFactory instance;
    
    // Service instances
    private final SingleCycleWaveformGenerator singleCycleGenerator;
    private final MultiFrameWavetableGenerator multiFrameGenerator;
    private final WavetableMorphService morphService;
    private final ExperimentalWaveformGenerator experimentalGenerator;
    
    /**
     * Private constructor to prevent direct instantiation.
     */
    private ServiceFactory() {
        this.singleCycleGenerator = new SingleCycleWaveformGenerator();
        this.multiFrameGenerator = new MultiFrameWavetableGenerator();
        this.morphService = new WavetableMorphService();
        this.experimentalGenerator = new ExperimentalWaveformGenerator();
    }
    
    /**
     * Gets the singleton instance of the factory.
     * 
     * @return The factory instance
     */
    public static synchronized ServiceFactory getInstance() {
        if (instance == null) {
            instance = new ServiceFactory();
        }
        return instance;
    }
    
    /**
     * Gets the single-cycle waveform generator.
     * 
     * @return The single-cycle waveform generator
     */
    public SingleCycleWaveformGenerator getSingleCycleGenerator() {
        return singleCycleGenerator;
    }
    
    /**
     * Gets the multi-frame wavetable generator.
     * 
     * @return The multi-frame wavetable generator
     */
    public MultiFrameWavetableGenerator getMultiFrameGenerator() {
        return multiFrameGenerator;
    }
    
    /**
     * Gets the wavetable morph service.
     * 
     * @return The wavetable morph service
     */
    public WavetableMorphService getMorphService() {
        return morphService;
    }
    
    /**
     * Gets the experimental waveform generator.
     * 
     * @return The experimental waveform generator
     */
    public ExperimentalWaveformGenerator getExperimentalGenerator() {
        return experimentalGenerator;
    }
} 