package com.github.jansowa.model;

import com.github.jansowa.dao.CacheModel;
import com.github.jansowa.domain.FileBasicInfo;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Setter
class CacheModelPerformance {
    @NonNull private CacheModel cacheModel;
    private long stopwatchTime;

    public void startStopwatch() {

    }

    public long checkStopwatchTimeInMilis() {
        return 0;
    }

    public FileBasicInfo[] generateFileBasicInfos(int numberOfFiles) {
        return new FileBasicInfo[0];
    }

    public void putFiles(FileBasicInfo[] files) {

    }

    public void removeFiles(String[] paths) {

    }

    public void moveFiles(String[] sourcePaths, String[] destinationPaths) {

    }

    public FileBasicInfo[] readFiles(String[] paths) {
        return new FileBasicInfo[0];
    }

    public void containFiles(String[] paths) {

    }

    public int getNumberOfFiles() {
        return 0;
    }
}