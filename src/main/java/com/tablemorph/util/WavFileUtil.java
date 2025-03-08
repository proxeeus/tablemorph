package com.tablemorph.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Utility class for WAV file operations.
 */
public class WavFileUtil {
    private static final int SAMPLE_RATE = 44100;
    private static final int BITS_PER_SAMPLE = 16;
    private static final int CHANNELS = 1;
    
    /**
     * Saves PCM data to a WAV file.
     * 
     * @param pcmData The PCM audio data
     * @param filePath The path to save the file to
     * @throws IOException If the file cannot be written
     */
    public static void saveToFile(byte[] pcmData, Path filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            writeWavHeader(fos, pcmData.length);
            fos.write(pcmData);
        }
    }
    
    /**
     * Writes a WAV header to the output stream.
     * 
     * @param fos The output stream to write to
     * @param audioDataLength The length of the audio data in bytes
     * @throws IOException If the header cannot be written
     */
    private static void writeWavHeader(FileOutputStream fos, int audioDataLength) throws IOException {
        // RIFF header
        fos.write("RIFF".getBytes()); // ChunkID
        writeInt(fos, 36 + audioDataLength); // ChunkSize: 4 + (8 + SubChunk1Size) + (8 + SubChunk2Size)
        fos.write("WAVE".getBytes()); // Format
        
        // fmt subchunk
        fos.write("fmt ".getBytes()); // Subchunk1ID
        writeInt(fos, 16); // Subchunk1Size: 16 for PCM
        writeShort(fos, 1); // AudioFormat: 1 for PCM
        writeShort(fos, CHANNELS); // NumChannels
        writeInt(fos, SAMPLE_RATE); // SampleRate
        writeInt(fos, SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8); // ByteRate
        writeShort(fos, CHANNELS * BITS_PER_SAMPLE / 8); // BlockAlign
        writeShort(fos, BITS_PER_SAMPLE); // BitsPerSample
        
        // data subchunk
        fos.write("data".getBytes()); // Subchunk2ID
        writeInt(fos, audioDataLength); // Subchunk2Size
    }
    
    /**
     * Writes a 32-bit integer to the output stream in little-endian format.
     */
    private static void writeInt(FileOutputStream fos, int value) throws IOException {
        fos.write(value & 0xFF);
        fos.write((value >> 8) & 0xFF);
        fos.write((value >> 16) & 0xFF);
        fos.write((value >> 24) & 0xFF);
    }
    
    /**
     * Writes a 16-bit integer to the output stream in little-endian format.
     */
    private static void writeShort(FileOutputStream fos, int value) throws IOException {
        fos.write(value & 0xFF);
        fos.write((value >> 8) & 0xFF);
    }
    
    /**
     * Reads a WAV file and returns the audio data as normalized float array.
     * 
     * @param file The WAV file to read
     * @return Normalized float array of audio samples, or null if error
     * @throws IOException If the file cannot be read or has an invalid format
     */
    public static float[] readWavFile(File file) throws IOException {
        if (!file.exists() || file.length() < 44) { // 44 is minimum WAV header size
            throw new IOException("Invalid WAV file: too small or doesn't exist");
        }
        
        try (FileInputStream fis = new FileInputStream(file);
             java.io.BufferedInputStream bis = new java.io.BufferedInputStream(fis)) {
            
            // Read and verify WAV header
            byte[] header = new byte[12];
            bis.read(header, 0, 12);
            
            String riffHeader = new String(header, 0, 4);
            String waveHeader = new String(header, 8, 4);
            
            if (!riffHeader.equals("RIFF") || !waveHeader.equals("WAVE")) {
                throw new IOException("Invalid WAV file format - not a RIFF/WAVE file");
            }
            
            // Read chunks until we find the format and data chunks
            byte[] chunkHeader = new byte[8];
            int formatChannels = 0;
            int formatSampleRate = 0;
            int formatBitsPerSample = 0;
            byte[] audioData = null;
            
            int maxChunks = 10; // Limit to prevent infinite loops with malformed files
            int chunksRead = 0;
            
            while (chunksRead < maxChunks) {
                chunksRead++;
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
     * Converts a float waveform to PCM byte data.
     * 
     * @param waveform The float waveform with values in range [-1, 1]
     * @return PCM byte data
     */
    public static byte[] convertToPCM(float[] waveform) {
        int sampleCount = waveform.length;
        byte[] pcmData = new byte[sampleCount * 2]; // 16-bit = 2 bytes per sample
        
        for (int i = 0; i < sampleCount; i++) {
            // Convert float [-1.0, 1.0] to 16-bit PCM
            short sample = (short) (waveform[i] * 32767);
            
            // Write sample in little-endian format
            pcmData[i * 2] = (byte) (sample & 0xFF);
            pcmData[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        
        return pcmData;
    }
    
    /**
     * Converts a 2D float waveform (multiple frames) to PCM byte data.
     * 
     * @param waveformData The 2D float waveform with values in range [-1, 1]
     * @return PCM byte data
     */
    public static byte[] convertToPCM(float[][] waveformData) {
        int frameCount = waveformData.length;
        int sampleCount = waveformData[0].length;
        int totalSamples = frameCount * sampleCount;
        
        byte[] pcmData = new byte[totalSamples * 2]; // 16-bit = 2 bytes per sample
        
        for (int frame = 0; frame < frameCount; frame++) {
            for (int sample = 0; sample < sampleCount; sample++) {
                int index = frame * sampleCount + sample;
                
                // Convert float [-1.0, 1.0] to 16-bit PCM
                short pcmSample = (short) (waveformData[frame][sample] * 32767);
                
                // Write sample in little-endian format
                pcmData[index * 2] = (byte) (pcmSample & 0xFF);
                pcmData[index * 2 + 1] = (byte) ((pcmSample >> 8) & 0xFF);
            }
        }
        
        return pcmData;
    }
} 