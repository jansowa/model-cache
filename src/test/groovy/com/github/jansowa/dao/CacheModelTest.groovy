package com.github.jansowa.dao

import com.github.jansowa.domain.FileBasicInfo
import spock.lang.Shared
import spock.lang.Specification
abstract class CacheModelTest extends Specification{
    @Shared CacheModel cacheModel
    @Shared private FileBasicInfo[] sampleData

    def setupSpec()
    {
        //cacheModel = new CacheModel() - here we will add new implementation of cacheModel
        sampleData = new FileBasicInfo[3]
        sampleData[0] = generateFileBasicInfo("sample0", "/test/")
        sampleData[1] = generateFileBasicInfo("sample1", "/test/")
        sampleData[2] = generateFileBasicInfo("sample2", "/test/")
    }

    def setup()
    {
        cacheModel.removeAllData()
    }

    private static FileBasicInfo generateFileBasicInfo(String fileName, String folderPath){
        Date creationDate = new Date()
        return new FileBasicInfo(fileName, folderPath+fileName+".txt", "txt", "http://example.com/"+fileName+".txt", creationDate, creationDate)
    }

    void "Should upload file"()
    {
        when:
            cacheModel.put(sampleData[0].getFilePath(), sampleData[0])

        then:
            cacheModel.contains(sampleData[0].getFilePath())
    }

    void "Should delete uploaded file"()
    {
        given:
            cacheModel.put(sampleData[0].getFilePath(), sampleData[0])

        when:
            cacheModel.remove(sampleData[0].getFilePath())

        then:
            !cacheModel.contains(sampleData[0].getFilePath())
    }

    void "Should read file"()
    {
        given:
            cacheModel.put(sampleData[0].getFilePath(), sampleData[0])
        when:
            Optional<FileBasicInfo> downloadedInfo = cacheModel.read(sampleData[0].getFilePath())
        then:
            downloadedInfo.get() == sampleData[0]
    }

    void "Read method should return Optional with null if there is no file in given path"()
    {
        given:
            String readTestPath = "/test/read"

        when:
            Optional<FileBasicInfo> downloadedInfo= cacheModel.read(readTestPath)

        then:
           Optional.ofNullable(null) == downloadedInfo
    }

    void "Contain method should return true if model contains file"()
    {
        given:
            cacheModel.put(sampleData[0].getFilePath(), sampleData[0])

        when:
            boolean result = cacheModel.contains(sampleData[0].getFilePath())

        then:
            result
    }

    void "Contain method should return false if model doesn't contain file"()
    {
        given:
            String containsTestPath = "/test/contains"

        when:
            boolean result = cacheModel.contains(containsTestPath)

        then:
            !result
    }

    void "Should move single file from first to second path"()
    {
        given:
            String destinationPath = "/test/destination"
            cacheModel.put(sampleData[0].getFilePath(), sampleData[0])

        when:
            cacheModel.movePath(sampleData[0].getFilePath(), destinationPath)

        then:
            cacheModel.contains(destinationPath)
    }

    void "Should move whole folder from first to second path"()
    {
        given:
            String[] paths = new String[5]
            String sourceFolder = "/test/"
            String destinationFolder = "/testDestination/"
            for(int i=0; i<3; i++){
                paths[i] = "move1/"
            }
            paths[3] = "move1/move1-1/"
            paths[4] = "move2/"
            FileBasicInfo generatedFile

            for(int i=0; i<5; i++){
                generatedFile = generateFileBasicInfo("file"+i, sourceFolder +paths[i])
                cacheModel.put(generatedFile.getFilePath(), generatedFile)
            }

        when:
            cacheModel.movePath(sourceFolder, destinationFolder)

        then:
            cacheModel.contains(destinationFolder+paths[0])
            cacheModel.contains(destinationFolder+paths[1])
            cacheModel.contains(destinationFolder+paths[2])
            cacheModel.contains(destinationFolder+paths[3])
            cacheModel.contains(destinationFolder+paths[4])
    }

    void "Should get number of stored files"()
    {
        given:
            cacheModel.put(sampleData[0].getFilePath(), sampleData[0])
            cacheModel.put(sampleData[1].getFilePath(), sampleData[1])
            cacheModel.put(sampleData[2].getFilePath(), sampleData[2])

        when:
            int numberOfFiles = cacheModel.getNumberOfFiles()

        then:
            numberOfFiles == 3
    }

    void "Should remove all data"()
    {
        given:
            cacheModel.put(sampleData[0].getFilePath(), sampleData[0])
            cacheModel.put(sampleData[1].getFilePath(), sampleData[1])

        when:
            cacheModel.removeAllData()

        then:
            !cacheModel.contains(sampleData[0].getFilePath())
            !cacheModel.contains(sampleData[1].getFilePath())
            cacheModel.getNumberOfFiles()==0
    }
}