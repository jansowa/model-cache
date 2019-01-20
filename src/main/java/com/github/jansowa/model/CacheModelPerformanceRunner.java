package com.github.jansowa.model;

import com.github.jansowa.dao.CacheModel;
import com.github.jansowa.domain.FileBasicInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class CacheModelPerformanceRunner {
    private CacheModelPerformance cacheModelPerformance;

    class TimeResults{
        public long putTestTime;
        public long containTestTime;
        public long removeTestTime;
        public long moveTestTime;
        public long readTestTime;
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
            timeResults.moveTestTime = timeOfMoveFiles(numberOfTests);
            timeResults.readTestTime = timeOfReadFiles(numberOfTests);

            allTimeResults.add(timeResults);
        }

        return allTimeResults;
    }

    private void printResults(List<TimeResults> timeResults, List<String> cacheModelsNames, int numberOfTests){
        int numberOfResults = timeResults.size();
        final String timeMessage= "ms for tests of method ";

        Logger logger = Logger.getLogger("printResult");
        for(int i=0; i<numberOfResults; i++){
            logger.info("Results of " + cacheModelsNames.get(i) + " for " + numberOfTests + " operations:");
            logger.info(timeMessage + timeResults.get(i).putTestTime);
            logger.info(timeMessage + timeResults.get(i).containTestTime);
            logger.info(timeMessage + timeResults.get(i).removeTestTime);
            logger.info(timeMessage + timeResults.get(i).moveTestTime);
            logger.info(timeMessage + timeResults.get(i).readTestTime);
            logger.info("Summary: "
                    +
                    (timeResults.get(i).putTestTime +
                            timeResults.get(i).containTestTime +
                            timeResults.get(i).removeTestTime +
                            timeResults.get(i).moveTestTime +
                            timeResults.get(i).readTestTime));
        }
    }

    private long timeOfPutFiles(int numberOfFiles){
        FileBasicInfo[] generatedFiles = cacheModelPerformance.generateFileBasicInfos(numberOfFiles);
        cacheModelPerformance.startStopwatch();
        cacheModelPerformance.putFiles(generatedFiles);
        return cacheModelPerformance.checkStopwatchTimeInMilis();
    }

    private long timeOfContainFiles(int numberOfFiles){
        FileBasicInfo[] generatedFiles = cacheModelPerformance.generateFileBasicInfos(numberOfFiles);
        cacheModelPerformance.putFiles(generatedFiles);
        String[] filesPaths = getPathsFromFiles(generatedFiles);
        cacheModelPerformance.startStopwatch();
        cacheModelPerformance.containFiles(filesPaths);
        return cacheModelPerformance.checkStopwatchTimeInMilis();
    }

    private long timeOfRemoveFiles(int numberOfFiles){
        FileBasicInfo[] generatedFiles = cacheModelPerformance.generateFileBasicInfos(numberOfFiles);
        cacheModelPerformance.putFiles(generatedFiles);
        String[] filesPaths = getPathsFromFiles(generatedFiles);
        cacheModelPerformance.startStopwatch();
        cacheModelPerformance.removeFiles(filesPaths);
        return cacheModelPerformance.checkStopwatchTimeInMilis();
    }

    private long timeOfMoveFiles(int numberOfFiles){
        FileBasicInfo[] generatedFiles = cacheModelPerformance.generateFileBasicInfos(numberOfFiles);
        cacheModelPerformance.putFiles(generatedFiles);
        String[] sourcePaths = getPathsFromFiles(generatedFiles);

        String[] destinationPaths = new String[numberOfFiles];
        for (int i = 0; i < numberOfFiles; i++) {
            destinationPaths[i] = addTextToPath(sourcePaths[i]);
        }

        cacheModelPerformance.startStopwatch();
        cacheModelPerformance.moveFiles(sourcePaths, destinationPaths);
        return cacheModelPerformance.checkStopwatchTimeInMilis();
    }

    private long timeOfReadFiles(int numberOfFiles){
        FileBasicInfo[] generatedFiles = cacheModelPerformance.generateFileBasicInfos(numberOfFiles);
        cacheModelPerformance.putFiles(generatedFiles);
        String[] filesPaths = getPathsFromFiles(generatedFiles);
        cacheModelPerformance.startStopwatch();
        cacheModelPerformance.readFiles(filesPaths);
        return cacheModelPerformance.checkStopwatchTimeInMilis();
    }

    private String[] getPathsFromFiles(FileBasicInfo[] files){
        int numberOfFiles = files.length;
        String[] paths = new String[numberOfFiles];
        for (int i = 0; i < numberOfFiles; i++) {
            paths[i] = files[i].getFilePath();
        }
        return paths;
    }

    String addTextToPath(String path){
        return path.substring(0, 1) +
                "a" +
                path.substring(1, path.length());
    }
}