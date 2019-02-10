package com.github.jansowa.model;

import com.github.jansowa.dao.CacheModel;
import com.github.jansowa.domain.FileBasicInfo;

import java.util.ArrayList;
import java.util.List;

public class CacheModelPerformanceRunner {
    private CacheModelPerformance cacheModelPerformance;

    class TimeResults{
        long putTestTime;
        long containTestTime;
        long removeTestTime;
        long moveTestTime;
        long readTestTime;
        long sizeOfFiles;
    }

    public static void main(String[] args){
        CacheModelPerformanceRunner runner = new CacheModelPerformanceRunner();
        List<String> cacheModelsNames = new ArrayList<>();
        List<CacheModel> cacheModels = new ArrayList<>();
        //TODO Here I will add all cache models and its names
        int numberOfTests = 1000;
        List<TimeResults> allTimeResults = runner.runTestForAllCache(cacheModels, numberOfTests);
        runner.printResults(allTimeResults, cacheModelsNames, numberOfTests);
    }

    private List<TimeResults> runTestForAllCache(List<CacheModel> allCacheModels, int numberOfTests){
        List<TimeResults> allTimeResults = new ArrayList<>();

        for (CacheModel cacheModel: allCacheModels) {
            cacheModelPerformance = new CacheModelPerformance(cacheModel);
            TimeResults timeResults = new TimeResults();

            timeResults.putTestTime = timeOfPutFiles(numberOfTests);
            timeResults.containTestTime = timeOfContainFiles(numberOfTests);
            timeResults.removeTestTime = timeOfRemoveFiles(numberOfTests);
            timeResults.moveTestTime = timeOfMoveSingleFiles(numberOfTests);
            timeResults.readTestTime = timeOfReadFiles(numberOfTests);
            timeResults.sizeOfFiles = sizeOfFiles(numberOfTests);

            allTimeResults.add(timeResults);
        }

        return allTimeResults;
    }

    private void printResults(List<TimeResults> timeResults, List<String> cacheModelsNames, int numberOfTests){
        int numberOfResults = timeResults.size();
        final String timeMessage= "ms for tests of method ";

        for(int i=0; i<numberOfResults; i++){
            System.out.println("Results of " + cacheModelsNames.get(i) + " for " + numberOfTests + " operations:");
            System.out.println(timeResults.get(i).putTestTime + timeMessage + "put");
            System.out.println(timeResults.get(i).containTestTime + timeMessage + "contain");
            System.out.println(timeResults.get(i).removeTestTime + timeMessage + "remove");
            System.out.println(timeResults.get(i).moveTestTime + timeMessage + "move");
            System.out.println(timeResults.get(i).readTestTime + timeMessage + "read");
            System.out.println("Summary: "
                    +
                    (timeResults.get(i).putTestTime +
                            timeResults.get(i).containTestTime +
                            timeResults.get(i).removeTestTime +
                            timeResults.get(i).moveTestTime +
                            timeResults.get(i).readTestTime) + "ms");
            System.out.println("Size of "+numberOfTests+" files in bytes: "+timeResults.get(i).sizeOfFiles);
            System.out.println();
        }
    }

    private long timeOfPutFiles(int numberOfFiles){
        FileBasicInfo[] generatedFiles = cacheModelPerformance.generateFileBasicInfos(numberOfFiles);
        cacheModelPerformance.startStopwatch();
        cacheModelPerformance.putFiles(generatedFiles);
        long testTime = cacheModelPerformance.checkStopwatchTimeInMilis();
        cacheModelPerformance.getCacheModel().removeAllData();
        return testTime;
    }

    private long timeOfContainFiles(int numberOfFiles){
        FileBasicInfo[] generatedFiles = cacheModelPerformance.generateFileBasicInfos(numberOfFiles);
        cacheModelPerformance.putFiles(generatedFiles);
        String[] filesPaths = getPathsFromFiles(generatedFiles);
        cacheModelPerformance.startStopwatch();
        cacheModelPerformance.containFiles(filesPaths);
        long testTime = cacheModelPerformance.checkStopwatchTimeInMilis();
        cacheModelPerformance.getCacheModel().removeAllData();
        return testTime;
    }

    private long timeOfRemoveFiles(int numberOfFiles){
        FileBasicInfo[] generatedFiles = cacheModelPerformance.generateFileBasicInfos(numberOfFiles);
        cacheModelPerformance.putFiles(generatedFiles);
        String[] filesPaths = getPathsFromFiles(generatedFiles);
        cacheModelPerformance.startStopwatch();
        cacheModelPerformance.removeFiles(filesPaths);
        return cacheModelPerformance.checkStopwatchTimeInMilis();
    }

    private long timeOfMoveSingleFiles(int numberOfFiles){
        FileBasicInfo[] generatedFiles = cacheModelPerformance.generateFileBasicInfos(numberOfFiles);
        cacheModelPerformance.putFiles(generatedFiles);
        String[] sourcePaths = getPathsFromFiles(generatedFiles);

        String[] destinationPaths = new String[numberOfFiles];
        for (int i = 0; i < numberOfFiles; i++) {
            destinationPaths[i] = addTextToPath(sourcePaths[i]);
        }

        cacheModelPerformance.startStopwatch();
        cacheModelPerformance.moveFiles(sourcePaths, destinationPaths);
        long testTime = cacheModelPerformance.checkStopwatchTimeInMilis();
        cacheModelPerformance.getCacheModel().removeAllData();
        return testTime;
    }

    private long timeOfReadFiles(int numberOfFiles){
        FileBasicInfo[] generatedFiles = cacheModelPerformance.generateFileBasicInfos(numberOfFiles);
        cacheModelPerformance.putFiles(generatedFiles);
        String[] filesPaths = getPathsFromFiles(generatedFiles);
        cacheModelPerformance.startStopwatch();
        cacheModelPerformance.readFiles(filesPaths);
        long testTime = cacheModelPerformance.checkStopwatchTimeInMilis();
        cacheModelPerformance.getCacheModel().removeAllData();
        return testTime;
    }



    private long sizeOfFiles(int numberOfFiles){
        FileBasicInfo[] generatedFiles = cacheModelPerformance.generateFileBasicInfos(numberOfFiles);
        cacheModelPerformance.putFiles(generatedFiles);
        return cacheModelPerformance.getCacheModel().getSizeInBytes();
    }

    private String[] getPathsFromFiles(FileBasicInfo[] files){
        int numberOfFiles = files.length;
        String[] paths = new String[numberOfFiles];
        for (int i = 0; i < numberOfFiles; i++) {
            paths[i] = files[i].getFilePath();
        }
        return paths;
    }

    private String addTextToPath(String path){
        return path.substring(0, 1) +
                "a" +
                path.substring(1, path.length());
    }
}