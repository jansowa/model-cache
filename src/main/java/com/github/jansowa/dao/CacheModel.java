package com.github.jansowa.dao;

import com.github.jansowa.domain.FileBasicInfo;

import java.util.Optional;

public interface CacheModel {
    void put(String filePath, byte[] data);
    void remove(String filePath);
    boolean contains(String filePath);
    void movePath(String sourcePath, String destinationPath);
    Optional<FileBasicInfo> read(String filePath);
}
