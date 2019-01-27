package com.github.jansowa.model;

import com.github.jansowa.dao.CacheModel;
import com.github.jansowa.domain.FileBasicInfo;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
class CacheModelPerformance {
    @NonNull
    private CacheModel cacheModel;
    private long stopwatchTime;

    public void startStopwatch() {
        stopwatchTime = new Date().getTime();
    }

    public long checkStopwatchTimeInMilis() {
        long currentTime = new Date().getTime();
        return currentTime - stopwatchTime;
    }

    public FileBasicInfo[] generateFileBasicInfos(int numberOfFiles) {
        FileBasicInfo[] generatedFiles = new FileBasicInfo[numberOfFiles];
        for (int i = 0; i < numberOfFiles; i++)
            generatedFiles[i] = generateSingleFile(i);
        return generatedFiles;
    }

    private FileBasicInfo generateSingleFile(int number) {
        Date creationTime = new Date();
        return FileBasicInfo.builder()
                .filePath("generatedFiles/file"+number+".txt")
                .creationTime(creationTime)
                .lastUsageTime(creationTime)
                .extension("txt")
                .name("file"+number)
                .url("http://example.com/generatedFiles/file"+number+".txt")
                .build();
    }

    public void putFiles(FileBasicInfo[] files) {
        for (FileBasicInfo file : files)
            cacheModel.put(file);
    }

    public void removeFiles(String[] paths) {
        for(String path: paths)
            cacheModel.remove(path);
    }

    public void moveFiles(String[] sourcePaths, String[] destinationPaths) {
        int numberOfFiles = sourcePaths.length;
        for(int i=0; i<numberOfFiles; i++)
            cacheModel.movePath(sourcePaths[i], destinationPaths[i]);
    }

    public List<FileBasicInfo> readFiles(String[] paths) {
        List<String> pathsList = Arrays.asList(paths);
        return pathsList.stream().map(path -> cacheModel.read(path))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public boolean[] containFiles(String[] paths) {
        int numberOfPaths = paths.length;
        boolean[] isPathOccupied = new boolean[numberOfPaths];
        for (int i = 0; i < numberOfPaths; i++) {
            isPathOccupied[i] = cacheModel.contains(paths[i]);
        }
        return isPathOccupied;
    }

    public int getNumberOfFiles() {
        return cacheModel.getNumberOfFiles();
    }
}