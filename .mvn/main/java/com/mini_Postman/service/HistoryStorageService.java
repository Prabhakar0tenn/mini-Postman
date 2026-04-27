package com.mini_Postman.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini_Postman.model.HistoryItem;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryStorageService {

    private final ObjectMapper objectMapper;

    @Value("${app.history.storage-path:./data/history.json}")
    private String storagePath;

    @Value("${app.history.max-items:500}")
    private int maxItems;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private List<HistoryItem> inMemoryCache = new ArrayList<>();
    private File storageFile;

    @PostConstruct
    public void init() {
        storageFile = new File(storagePath);
        File parentDir = storageFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            if (created) {
                log.info("Created data directory: {}", parentDir.getAbsolutePath());
            }
        }
        
        loadFromFile();
    }

    private void loadFromFile() {
        lock.writeLock().lock();
        try {
            if (storageFile.exists() && storageFile.length() > 0) {
                inMemoryCache = objectMapper.readValue(storageFile, new TypeReference<List<HistoryItem>>() {});
                log.info("Loaded {} history items from {}", inMemoryCache.size(), storagePath);
            } else {
                inMemoryCache = new ArrayList<>();
            }
        } catch (IOException e) {
            log.error("Failed to load history from file", e);
            inMemoryCache = new ArrayList<>();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void saveToFile() {
        try {
            objectMapper.writeValue(storageFile, inMemoryCache);
        } catch (IOException e) {
            log.error("Failed to save history to file", e);
        }
    }

    public void saveHistoryItem(HistoryItem item) {
        lock.writeLock().lock();
        try {
            inMemoryCache.add(0, item); // Add to the beginning (latest first)
            
            // Trim if exceeds max items
            if (inMemoryCache.size() > maxItems) {
                inMemoryCache = new ArrayList<>(inMemoryCache.subList(0, maxItems));
            }
            
            saveToFile();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<HistoryItem> getAllHistory() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(inMemoryCache);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Optional<HistoryItem> getHistoryItem(String id) {
        lock.readLock().lock();
        try {
            return inMemoryCache.stream()
                    .filter(item -> id.equals(item.getId()))
                    .findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean deleteHistoryItem(String id) {
        lock.writeLock().lock();
        try {
            boolean removed = inMemoryCache.removeIf(item -> id.equals(item.getId()));
            if (removed) {
                saveToFile();
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void clearAllHistory() {
        lock.writeLock().lock();
        try {
            inMemoryCache.clear();
            saveToFile();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
