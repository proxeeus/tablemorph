package com.tablemorph.model.cache;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Class representing metadata about a sound file in the cache.
 * Stores information needed to detect changes without reloading the entire file.
 */
public class SoundFileInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String absolutePath;
    private final String relativePath;
    private final String filename;
    private final long fileSize;
    private final long lastModifiedTimestamp;
    
    /**
     * Creates a new SoundFileInfo from a File object and base directory.
     * 
     * @param file The sound file
     * @param baseDir The base directory to calculate relative path
     */
    public SoundFileInfo(File file, File baseDir) {
        this.absolutePath = file.getAbsolutePath();
        
        // Calculate relative path
        Path basePath = baseDir.toPath();
        Path filePath = file.toPath();
        this.relativePath = basePath.relativize(filePath).toString();
        
        this.filename = file.getName();
        this.fileSize = file.length();
        this.lastModifiedTimestamp = file.lastModified();
    }
    
    /**
     * Gets the absolute path of the file.
     * 
     * @return Absolute path
     */
    public String getAbsolutePath() {
        return absolutePath;
    }
    
    /**
     * Gets the relative path from the base directory.
     * 
     * @return Relative path
     */
    public String getRelativePath() {
        return relativePath;
    }
    
    /**
     * Gets the filename.
     * 
     * @return Filename
     */
    public String getFilename() {
        return filename;
    }
    
    /**
     * Gets the file size in bytes.
     * 
     * @return File size
     */
    public long getFileSize() {
        return fileSize;
    }
    
    /**
     * Gets the last modified timestamp.
     * 
     * @return Last modified timestamp
     */
    public long getLastModifiedTimestamp() {
        return lastModifiedTimestamp;
    }
    
    /**
     * Gets the last modified time as LocalDateTime.
     * 
     * @return Last modified time
     */
    public LocalDateTime getLastModifiedTime() {
        return LocalDateTime.ofInstant(
            Instant.ofEpochMilli(lastModifiedTimestamp), 
            ZoneId.systemDefault()
        );
    }
    
    /**
     * Converts this info back to a File object.
     * 
     * @return File object
     */
    public File toFile() {
        return new File(absolutePath);
    }
    
    /**
     * Checks if this file info is still valid compared to the actual file.
     * 
     * @return true if valid, false if file changed or deleted
     */
    public boolean isValid() {
        File file = new File(absolutePath);
        return file.exists() && 
               file.isFile() && 
               file.length() == fileSize && 
               file.lastModified() == lastModifiedTimestamp;
    }
    
    @Override
    public String toString() {
        return relativePath;
    }
} 