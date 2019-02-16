package com.github.jansowa.dao.relationaldb;

import com.github.jansowa.dao.CacheModel;
import com.github.jansowa.domain.FileBasicInfo;

import java.util.Optional;

public class SQLiteCacheModel2 implements CacheModel {
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

    @Override
    public long getSizeInBytes() {
        return 0;
    }

    @Override
    public long getMaxNumberOfFiles() {
        return 0;
    }

    @Override
    public void setMaxNumberOfFiles(long maxStorage) {

    }

    @Override
    public void removeFromDevice() {

    }
}
