package com.tablemorph.service.cache;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.tablemorph.config.GeneratorConfig;
import com.tablemorph.model.cache.SoundFileCache;
import com.tablemorph.model.cache.SoundFileInfo;
import com.tablemorph.util.DirectoryUtil;

/**
 * Service class that manages the sound file cache.
 * Handles cache creation, validation, and persistence.
 */
public class SoundFileCacheManager {
    private static final String SOUNDS_DIRECTORY = "sounds";
    private static final String CACHE_FILENAME = ".soundscache";
    
    private SoundFileCache cache;
    private boolean isDirty;
    
    // Thread-safety for the cache
    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    
    // Performance metrics
    private long lastScanDurationMs;
    
    /**
     * Creates a new cache manager.
     */
    public SoundFileCacheManager() {
        this.isDirty = false;
        this.lastScanDurationMs = 0;
    }
    
    /**
     * Initializes the cache. 
     * Loads from disk if available and valid, or builds a new one.
     */
    public void initializeCache() {
        try {
            // Try to load existing cache
            if (loadCache()) {
                // Validate if loaded cache is up-to-date
                if (isCacheValid()) {
                    return;
                }
            }
            
            // Cache doesn't exist or is invalid - build a new one
            updateCache();
            
        } catch (Exception e) {
            System.out.println("Warning: Error initializing sound file cache: " + e.getMessage());
            // Create an empty cache as fallback
            File soundsDir = new File(SOUNDS_DIRECTORY);
            cache = new SoundFileCache(soundsDir);
        }
    }
    
    /**
     * Updates the cache by scanning the sounds directory.
     */
    public void updateCache() {
        if (!GeneratorConfig.getCacheEnabled()) {
            return;
        }
        
        File soundsDir = new File(SOUNDS_DIRECTORY);
        
        try {
            cacheLock.writeLock().lock();
            
            // Create a new cache
            cache = new SoundFileCache(soundsDir);
            
            // Measure scan time
            long startTime = System.currentTimeMillis();
            
            // Scan directory and populate cache
            int dirCount = scanDirectory(soundsDir, soundsDir);
            cache.setTotalDirectories(dirCount);
            
            // Update timestamp and mark as dirty
            cache.updateTimestamp();
            isDirty = true;
            
            // Calculate scan duration
            lastScanDurationMs = System.currentTimeMillis() - startTime;
            
            // Save the cache to disk
            saveCache();
            
            System.out.println("Sound file cache updated. Found " + cache.getFileCount() + 
                            " sound files in " + cache.getTotalDirectories() + " directories.");
            
        } catch (Exception e) {
            System.out.println("Warning: Error updating sound file cache: " + e.getMessage());
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * Recursively scans a directory and adds sound files to the cache.
     * 
     * @param directory The directory to scan
     * @param rootDir The root sounds directory
     * @return Number of directories scanned
     */
    private int scanDirectory(File directory, File rootDir) {
        if (!directory.exists() || !directory.isDirectory()) {
            return 0;
        }
        
        int dirCount = 1; // Count this directory
        
        File[] files = directory.listFiles();
        if (files == null) {
            return dirCount;
        }
        
        for (File file : files) {
            if (file.isFile() && file.getName().toLowerCase().endsWith(".wav")) {
                cache.addFile(file, rootDir);
            } else if (file.isDirectory()) {
                dirCount += scanDirectory(file, rootDir);
            }
        }
        
        return dirCount;
    }
    
    /**
     * Checks if the cache is valid based on directory modification time and cache lifetime.
     * 
     * @return true if cache is valid, false otherwise
     */
    public boolean isCacheValid() {
        if (cache == null || !GeneratorConfig.getCacheEnabled()) {
            return false;
        }
        
        try {
            cacheLock.readLock().lock();
            
            // Check cache lifetime
            int maxLifetimeMinutes = GeneratorConfig.getCacheLifetimeMinutes();
            LocalDateTime now = LocalDateTime.now();
            Duration age = Duration.between(cache.getLastUpdated(), now);
            
            if (age.toMinutes() > maxLifetimeMinutes) {
                return false;
            }
            
            // Validate the root directory exists and check last modified time
            File soundsDir = new File(SOUNDS_DIRECTORY);
            if (!soundsDir.exists() || !soundsDir.isDirectory()) {
                return false;
            }
            
            // Check if root directory path matches
            if (!soundsDir.getAbsolutePath().equals(cache.getRootDirectoryPath())) {
                return false;
            }
            
            // Quick validation: check if directory last modified time is newer than cache
            long dirLastModified = getDirectoryLastModifiedTime(soundsDir);
            long cacheLastModified = cache.getLastUpdated().atZone(java.time.ZoneId.systemDefault())
                                         .toInstant().toEpochMilli();
            
            return dirLastModified <= cacheLastModified;
            
        } catch (Exception e) {
            return false;
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    /**
     * Gets the most recent last modified time for any file or subdirectory in a directory.
     * 
     * @param directory The directory to check
     * @return The most recent last modified time
     */
    private long getDirectoryLastModifiedTime(File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            return 0;
        }
        
        long lastModified = directory.lastModified();
        
        File[] files = directory.listFiles();
        if (files == null) {
            return lastModified;
        }
        
        for (File file : files) {
            if (file.isFile()) {
                lastModified = Math.max(lastModified, file.lastModified());
            } else if (file.isDirectory()) {
                lastModified = Math.max(lastModified, getDirectoryLastModifiedTime(file));
            }
        }
        
        return lastModified;
    }
    
    /**
     * Gets the cached sound files.
     * If cache is invalid, it will be updated first.
     * 
     * @return List of sound files
     */
    public List<File> getCachedSoundFiles() {
        // If caching is disabled, scan directly
        if (!GeneratorConfig.getCacheEnabled()) {
            return scanSoundFilesDirectly();
        }
        
        // Initialize cache if needed
        if (cache == null) {
            initializeCache();
        }
        
        // Validate and update if needed
        if (!isCacheValid()) {
            System.out.println("Sound file cache is outdated, updating...");
            updateCache();
        }
        
        try {
            cacheLock.readLock().lock();
            if (cache != null) {
                return cache.getAllFiles();
            } else {
                return scanSoundFilesDirectly();
            }
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    /**
     * Scans the sounds directory directly without using cache.
     * This is a fallback method if cache is disabled or unavailable.
     * 
     * @return List of sound files
     */
    private List<File> scanSoundFilesDirectly() {
        File soundsDir = new File(SOUNDS_DIRECTORY);
        List<File> result = new ArrayList<>();
        scanDirectorySoundFiles(soundsDir, result);
        
        // Track cache misses if cache exists
        if (cache != null) {
            cache.incrementCacheMiss();
        }
        
        return result;
    }
    
    /**
     * Helper method to scan a directory for sound files without using cache.
     * 
     * @param directory The directory to scan
     * @param result The list to add found files to
     */
    private void scanDirectorySoundFiles(File directory, List<File> result) {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }
        
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            if (file.isFile() && file.getName().toLowerCase().endsWith(".wav")) {
                result.add(file);
            } else if (file.isDirectory()) {
                scanDirectorySoundFiles(file, result);
            }
        }
    }
    
    /**
     * Invalidates the cache, forcing it to be rebuilt on next access.
     */
    public void invalidateCache() {
        try {
            cacheLock.writeLock().lock();
            cache = null;
            isDirty = false;
            
            // Delete cache file
            File cacheFile = getCacheFile();
            if (cacheFile.exists()) {
                cacheFile.delete();
            }
            
            System.out.println("Sound file cache has been invalidated.");
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * Saves the cache to disk.
     * 
     * @return true if successful, false otherwise
     */
    public boolean saveCache() {
        if (cache == null || !GeneratorConfig.getCacheEnabled()) {
            return false;
        }
        
        if (!isDirty) {
            return true; // No changes to save
        }
        
        try {
            cacheLock.readLock().lock();
            
            File cacheFile = getCacheFile();
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new BufferedOutputStream(new FileOutputStream(cacheFile)))) {
                oos.writeObject(cache);
                isDirty = false;
                return true;
            } catch (IOException e) {
                System.out.println("Warning: Could not save sound file cache: " + e.getMessage());
                return false;
            }
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    /**
     * Loads the cache from disk.
     * 
     * @return true if successful, false otherwise
     */
    public boolean loadCache() {
        if (!GeneratorConfig.getCacheEnabled()) {
            return false;
        }
        
        File cacheFile = getCacheFile();
        if (!cacheFile.exists() || cacheFile.length() == 0) {
            return false;
        }
        
        try {
            cacheLock.writeLock().lock();
            
            try (ObjectInputStream ois = new ObjectInputStream(
                    new BufferedInputStream(new FileInputStream(cacheFile)))) {
                cache = (SoundFileCache) ois.readObject();
                isDirty = false;
                return true;
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Warning: Could not load sound file cache: " + e.getMessage());
                return false;
            }
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * Gets the cache file.
     * 
     * @return The cache file
     */
    private File getCacheFile() {
        return new File(CACHE_FILENAME);
    }
    
    /**
     * Gets cache statistics.
     * 
     * @return A string with cache statistics
     */
    public String getCacheStats() {
        if (cache == null) {
            return "Cache not initialized";
        }
        
        try {
            cacheLock.readLock().lock();
            
            StringBuilder stats = new StringBuilder();
            stats.append("Sound File Cache Statistics:\n");
            stats.append("- Files: ").append(cache.getFileCount()).append("\n");
            stats.append("- Directories: ").append(cache.getTotalDirectories()).append("\n");
            stats.append("- Total size: ").append(formatSize(cache.getTotalSize())).append("\n");
            stats.append("- Last updated: ").append(cache.getLastUpdated()).append("\n");
            stats.append("- Cache hits: ").append(cache.getCacheHits()).append("\n");
            stats.append("- Cache misses: ").append(cache.getCacheMisses()).append("\n");
            stats.append("- Last scan time: ").append(lastScanDurationMs).append("ms\n");
            
            return stats.toString();
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    /**
     * Formats a file size in bytes to a human-readable string.
     * 
     * @param size Size in bytes
     * @return Human-readable size string
     */
    private String formatSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }
} 