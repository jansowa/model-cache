package com.github.jansowa.model

import com.github.jansowa.dao.CacheModel
import com.github.jansowa.domain.FileBasicInfo
import spock.lang.Shared
import spock.lang.Specification

class CacheModelPerformanceTest extends Specification {
    CacheModel cacheModel
    CacheModelPerformance cacheModelPerformance
    @Shared FileBasicInfo[] generatedSampleFiles

    def setup()
    {
        cacheModel = Mock()
        cacheModelPerformance = new CacheModelPerformance(cacheModel)
    }

    def setupSpec() {
        generatedSampleFiles = new FileBasicInfo[5] //don't change number of files under 5 - it would broke tests
        Date creationDate
        for (int i = 0; i < 5; i++) {
            creationDate = new Date()
            generatedSampleFiles[i] = new FileBasicInfo("file" + 1, "/test/file" + i + ".txt", "txt", "http://example.com/test/file" + i + ".txt", creationDate, creationDate)
        }
    }

    String[] getFilePaths(FileBasicInfo[] files){
        int numberOfFiles = files.length
        String[] paths = new String[numberOfFiles]
        for(int i=0; i<numberOfFiles; i++){
            paths[i] = files[i].getFilePath()
        }
        return paths
    }

    String addTextToPath(String path, String textToAdd){
        return path.substring(0, 1) +
                        textToAdd +
                        path.substring(1, path.length())
    }

    boolean areAllFieldsNotNull(FileBasicInfo file){
        return file.lastModifiedTime != null &&
                file.creationTime != null &&
                file.name != null &&
                file.extension != null &&
                file.url != null &&
                file.filePath != null
    }

    void "Should return time between startStopwatch and checkStopwatchTimeInMilis"()
    {
        given:
            cacheModelPerformance.startStopwatch()
            Thread.sleep(1000)
        when:
            long time = cacheModelPerformance.checkStopwatchTimeInMilis()

        then:
            time>=1000
    }

    void "Should generate 3 sample data"()
    {
        when:
            FileBasicInfo[] files = cacheModelPerformance.generateFileBasicInfos(10)
        then:
            files != null
            files.length == 3
            files[0] != null
            areAllFieldsNotNull(files[0])
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
            for(int i=0; i<5; i++)
                destinationPaths[i] = addTextToPath(sourcePaths[i], "a")

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
            for(int i=0; i<5; i++)
                cacheModel.read(generatedFilePaths[i]) >> Optional.ofNullable(generatedSampleFiles[i])

        when:
            Optional<FileBasicInfo>[] filesInfo = cacheModelPerformance.readFiles(generatedFilePaths)

        then:
            filesInfo[0].get() == generatedSampleFiles[0]
            filesInfo[1].get() == generatedSampleFiles[1]
            filesInfo[2].get() == generatedSampleFiles[2]
            filesInfo[3].get() == generatedSampleFiles[3]
            filesInfo[4].get() == generatedSampleFiles[4]
    }

    void "Should check if memory contains 5 specific files"()
    {
        given:
            String[] sampleFilesPaths = getFilePaths(generatedSampleFiles)

        when:
            cacheModelPerformance.containFiles(sampleFilesPaths)
            boolean[] emptyPaths = cacheModelPerformance.containFiles(['randomPath1', 'randomPath2'] as String[])
        then:
            1 * cacheModel.contains(sampleFilesPaths[0])
            1 * cacheModel.contains(sampleFilesPaths[1])
            1 * cacheModel.contains(sampleFilesPaths[2])
            1 * cacheModel.contains(sampleFilesPaths[3])
            1 * cacheModel.contains(sampleFilesPaths[4])
            emptyPaths == [false, false] as boolean[]
    }

    void "Should return number of files"()
    {
        when:
            cacheModelPerformance.getNumberOfFiles()

        then:
            1 * cacheModel.getNumberOfFiles()
    }
}