package com.github.jansowa.dao.datastructure;

import com.github.jansowa.dao.CacheModel;
import com.github.jansowa.domain.FileBasicInfo;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class HashMapCacheModel implements CacheModel, Serializable{
    private transient double maxStorageMB;
    private transient String cacheModelPath;
    private Map<String, FileBasicInfo> storedFiles;

    public MapCacheModel(double maxStorageMB, String cacheModelPath){
        this.maxStorageMB = maxStorageMB;
        this.cacheModelPath = cacheModelPath;
        this.storedFiles = new HashMap<>();
    }

    @Override
    public void put(FileBasicInfo file) {

    }

    @Override
    public void remove(String filePath) {

    }

    @Override
    public boolean contains(String filePath) {
        return false;
    }

    @Override
    public void movePath(String sourcePath, String destinationPath) {

    }

    @Override
    public Optional<FileBasicInfo> read(String filePath) {
        return Optional.empty();
    }

    @Override
    public int getNumberOfFiles() {
        return 0;
    }

    @Override
    public void removeAllData() {

    }
}
