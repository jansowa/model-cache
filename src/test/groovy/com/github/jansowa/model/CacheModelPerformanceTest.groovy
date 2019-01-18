package com.github.jansowa.model

import com.github.jansowa.dao.CacheModel
import com.github.jansowa.domain.FileBasicInfo
import spock.lang.Shared
import spock.lang.Specification

class CacheModelPerformanceTest extends Specification {
    CacheModel cacheModel
    CacheModelPerformance cacheModelPerformance
    @Shared FileBasicInfo[] generatedSampleFiles

    def setupSpec() {
        generatedSampleFiles = new FileBasicInfo[5] //don't change number of files under 5 - it would broke tests
        Date creationDate
        for (int i = 0; i < 5; i++) {
            creationDate = new Date()
            generatedSampleFiles[i] = new FileBasicInfo("file" + 1, "/test/file" + i + ".txt", "txt", "http://example.com/test/file" + i + ".txt", creationDate, creationDate)
        }
    }

    def setup()
    {
        cacheModel = Mock()
        cacheModelPerformance = new CacheModelPerformance(cacheModel)
    }

    String[] getFilePaths(FileBasicInfo[] files){
        int numberOfFiles = files.length
        String[] paths = new String[numberOfFiles]
        for(int i=0; i<numberOfFiles; i++){
            paths[i] = files[i].getFilePath()
        }
        return paths
    }

    void "Should return time between startStoper and checkTime"()
    {
        given:
            cacheModelPerformance.startStoper()

        when:
            long time = cacheModelPerformance.checkTimeInMilis()

        then:
            time>0
    }

    void "Should generate 3 sample data"()
    {
        when:
            FileBasicInfo[] files = cacheModelPerformance.generateFileBasicInfos(10)
        then:
            files != null
            files.length == 3
            files[0] != null
            files[1] != null
            files[2] != null
    }

    void "Should add 5 files"()
    {
        when:
            cacheModelPerformance.putFiles(generatedSampleFiles)

        then:
            1 * cacheModel.put(generatedSampleFiles[0])
            1 * cacheModel.put(generatedSampleFiles[1])
            1 * cacheModel.put(generatedSampleFiles[2])
            1 * cacheModel.put(generatedSampleFiles[3])
            1 * cacheModel.put(generatedSampleFiles[4])
    }

    void "Should remove 5 files"()
    {
        given:
            cacheModelPerformance.putFiles(generatedSampleFiles)
            String[] paths = getFilePaths(generatedSampleFiles)

        when:
            cacheModelPerformance.removeFiles(getFilePaths(generatedSampleFiles))

        then:
            1 * cacheModel.remove(paths[0])
            1 * cacheModel.remove(paths[1])
            1 * cacheModel.remove(paths[2])
            1 * cacheModel.remove(paths[3])
            1 * cacheModel.remove(paths[4])
    }

    void "Should move 5 files"()
    {
        given:
            cacheModelPerformance.putFiles(generatedSampleFiles)
            String[] sourcePaths = getFilePaths(generatedSampleFiles)
            String[] destinationPaths = new String[sourcePaths.length]
            for(int i=0; i<5; i++){
                destinationPaths[i] =
                        sourcePaths[i].substring(0, 1) +
                                "a" +
                                sourcePaths[i].substring(1, sourcePaths[i].length())
            }

        when:
            cacheModelPerformance.moveFiles(sourcePaths, destinationPaths)

        then:
            1 * cacheModel.movePath(sourcePaths[0], destinationPaths[0])
            1 * cacheModel.movePath(sourcePaths[1], destinationPaths[1])
            1 * cacheModel.movePath(sourcePaths[2], destinationPaths[2])
            1 * cacheModel.movePath(sourcePaths[3], destinationPaths[3])
            1 * cacheModel.movePath(sourcePaths[4], destinationPaths[4])
    }

    void "Should read 5 files"()
    {
        given:
            String[] generatedFilePaths = getFilePaths(generatedSampleFiles)
            cacheModelPerformance.putFiles(generatedSampleFiles)
            cacheModel = Stub(CacheModel.class)

        and:
            for(int i=0; i<5; i++) {
                cacheModel.read(generatedFilePaths[i]) >> generatedSampleFiles[i]
            }

        when:
            FileBasicInfo[] filesInfo = cacheModelPerformance.readFiles(generatedFilePaths)

        then:
            filesInfo == generatedSampleFiles
    }

    void "Should check if memory contains 5 specific files"()
    {
        given:
            String[] sampleFilesPaths = getFilePaths(generatedSampleFiles)

        when:
            cacheModelPerformance.containFiles(sampleFilesPaths)

        then:
            1 * cacheModel.contains(sampleFilesPaths[0])
            1 * cacheModel.contains(sampleFilesPaths[1])
            1 * cacheModel.contains(sampleFilesPaths[2])
            1 * cacheModel.contains(sampleFilesPaths[3])
            1 * cacheModel.contains(sampleFilesPaths[4])
    }

    void "Should return number of files"()
    {
        when:
            cacheModelPerformance.getNumberOfFiles()

        then:
            1 * cacheModel.getNumberOfFiles()
    }
}