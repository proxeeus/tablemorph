package com.tablemorph.model.cache;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Main cache class that stores information about sound files.
 * This is the serializable object that will be persisted to disk.
 */
public class SoundFileCache implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Main cache of sound files, mapped by their absolute path
    private final Map<String, SoundFileInfo> cachedFiles;
    
    // Timestamp when cache was last fully updated
    private LocalDateTime lastUpdated;
    
    // Root directory path (for validation)
    private final String rootDirectoryPath;
    
    // Cache statistics
    private int totalSoundFiles;
    private int totalDirectories;
    private long totalSizeBytes;
    
    // Cache hit/miss statistics (transient - not serialized)
    private transient int cacheHits;
    private transient int cacheMisses;
    
    /**
     * Creates a new empty cache for the specified root directory.
     * 
     * @param rootDirectory The sounds directory
     */
    public SoundFileCache(File rootDirectory) {
        this.cachedFiles = new HashMap<>();
        this.lastUpdated = LocalDateTime.now();
        this.rootDirectoryPath = rootDirectory.getAbsolutePath();
        this.totalSoundFiles = 0;
        this.totalDirectories = 0;
        this.totalSizeBytes = 0;
        this.cacheHits = 0;
        this.cacheMisses = 0;
    }
    
    /**
     * Adds a sound file to the cache.
     * 
     * @param file The sound file to add
     * @param rootDirectory The root directory for relative path calculation
     */
    public void addFile(File file, File rootDirectory) {
        SoundFileInfo info = new SoundFileInfo(file, rootDirectory);
        cachedFiles.put(info.getAbsolutePath(), info);
        totalSizeBytes += info.getFileSize();
        totalSoundFiles++;
    }
    
    /**
     * Removes a file from the cache.
     * 
     * @param path The absolute path of the file to remove
     * @return true if the file was removed, false if it wasn't in the cache
     */
    public boolean removeFile(String path) {
        SoundFileInfo removed = cachedFiles.remove(path);
        if (removed != null) {
            totalSizeBytes -= removed.getFileSize();
            totalSoundFiles--;
            return true;
        }
        return false;
    }
    
    /**
     * Gets the list of all cached files as File objects.
     * 
     * @return List of File objects
     */
    public List<File> getAllFiles() {
        // Increase hit count for stats
        cacheHits++;
        
        return cachedFiles.values().stream()
                .map(SoundFileInfo::toFile)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets the number of files in the cache.
     * 
     * @return Number of files
     */
    public int getFileCount() {
        return totalSoundFiles;
    }
    
    /**
     * Gets the total size of all cached files.
     * 
     * @return Total size in bytes
     */
    public long getTotalSize() {
        return totalSizeBytes;
    }
    
    /**
     * Updates the time when the cache was last updated.
     */
    public void updateTimestamp() {
        this.lastUpdated = LocalDateTime.now();
    }
    
    /**
     * Gets the time when the cache was last updated.
     * 
     * @return Last update time
     */
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    /**
     * Gets the root directory path this cache was created for.
     * 
     * @return Root directory path
     */
    public String getRootDirectoryPath() {
        return rootDirectoryPath;
    }
    
    /**
     * Gets the number of directories scanned.
     * 
     * @return Number of directories
     */
    public int getTotalDirectories() {
        return totalDirectories;
    }
    
    /**
     * Sets the number of directories scanned.
     * 
     * @param count Directory count
     */
    public void setTotalDirectories(int count) {
        this.totalDirectories = count;
    }
    
    /**
     * Increments the directory count.
     */
    public void incrementDirectoryCount() {
        this.totalDirectories++;
    }
    
    /**
     * Gets the cache hit count.
     * 
     * @return Number of cache hits
     */
    public int getCacheHits() {
        return cacheHits;
    }
    
    /**
     * Gets the cache miss count.
     * 
     * @return Number of cache misses
     */
    public int getCacheMisses() {
        return cacheMisses;
    }
    
    /**
     * Increments the cache miss counter.
     */
    public void incrementCacheMiss() {
        this.cacheMisses++;
    }
    
    /**
     * Clears all cache data.
     */
    public void clear() {
        cachedFiles.clear();
        totalSoundFiles = 0;
        totalDirectories = 0;
        totalSizeBytes = 0;
        updateTimestamp();
    }
    
    /**
     * Gets a specific file info by path.
     * 
     * @param path Absolute path of the file
     * @return SoundFileInfo or null if not found
     */
    public SoundFileInfo getFileInfo(String path) {
        return cachedFiles.get(path);
    }
    
    /**
     * Checks if a file is in the cache.
     * 
     * @param path Absolute path of the file
     * @return true if in cache, false otherwise
     */
    public boolean containsFile(String path) {
        return cachedFiles.containsKey(path);
    }
    
    /**
     * Returns all cached file information.
     * 
     * @return Collection of all file info objects
     */
    public List<SoundFileInfo> getAllFileInfos() {
        return new ArrayList<>(cachedFiles.values());
    }
} 