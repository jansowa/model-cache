package com.github.jansowa.dao.nonrelationaldb

import com.github.jansowa.domain.NitriteFileBasicInfo
import spock.lang.Shared
import spock.lang.Specification

class NitriteCacheModel2Spec extends Specification{
    @Shared NitriteCacheModel2 cacheModel
    @Shared private NitriteFileBasicInfo[] sampleData
    static final long MAX_NUMBER_OF_FILES = 1000
    static final String CACHE_PATH = "NitriteCacheModel2Test.db"

    def setupSpec()
    {
        cacheModel = new NitriteCacheModel2(MAX_NUMBER_OF_FILES, CACHE_PATH)
        sampleData = new NitriteFileBasicInfo[3]
        sampleData[0] = generateFileBasicInfo("sample0", "/test/")
        sampleData[1] = generateFileBasicInfo("sample1", "/test/")
        sampleData[2] = generateFileBasicInfo("sample2", "/test/")
    }

    def setup()
    {
        cacheModel.removeAllData()
    }

    def cleanupSpec()
    {
        cacheModel.removeFromDevice()
    }

    private static NitriteFileBasicInfo generateFileBasicInfo(String fileName, String folderPath){
        Date creationDate = new Date()
        return new NitriteFileBasicInfo(fileName, folderPath+fileName+".txt", "txt", "http://example.com/"+fileName+".txt", creationDate, creationDate)
    }

    void "Should upload file"()
    {
        when:
            cacheModel.put(sampleData[0])

        then:
            cacheModel.contains(sampleData[0].getFilePath())
    }

    void "Should delete uploaded file"()
    {
        given:
            cacheModel.put(sampleData[0])

        when:
            cacheModel.remove(sampleData[0].getFilePath())

        then:
            !cacheModel.contains(sampleData[0].getFilePath())
    }

    void "Should read file"()
    {
        given:
            cacheModel.put(sampleData[0])

        when:
            Optional<NitriteFileBasicInfo> downloadedInfo = cacheModel.read(sampleData[0].getFilePath())

        then:
            downloadedInfo.get().name == sampleData[0].name
            downloadedInfo.get().filePath == sampleData[0].filePath
            downloadedInfo.get().extension == sampleData[0].extension
            downloadedInfo.get().url == sampleData[0].url
            downloadedInfo.get().creationTime == sampleData[0].creationTime
    }

    void "Read method should return Optional with null if there is no file in given path"()
    {
        given:
            String readTestPath = "/test/read"

        when:
            Optional<NitriteFileBasicInfo> downloadedInfo= cacheModel.read(readTestPath)

        then:
           Optional.ofNullable(null) == downloadedInfo
    }

    void "Contain method should return true if model contains file"()
    {
        given:
            cacheModel.put(sampleData[0])

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
            cacheModel.put(sampleData[0])

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
            NitriteFileBasicInfo generatedFile

            for(int i=0; i<5; i++){
                generatedFile = generateFileBasicInfo("file"+i, sourceFolder +paths[i])
                cacheModel.put(generatedFile)
            }

        when:
            cacheModel.movePath(sourceFolder, destinationFolder)

        then:
            cacheModel.contains(destinationFolder+paths[0]+"file0.txt")
            cacheModel.contains(destinationFolder+paths[1]+"file1.txt")
            cacheModel.contains(destinationFolder+paths[2]+"file2.txt")
            cacheModel.contains(destinationFolder+paths[3]+"file3.txt")
            cacheModel.contains(destinationFolder+paths[4]+"file4.txt")
    }

    void "Should get number of stored files"()
    {
        given:
            cacheModel.put(sampleData[0])
            cacheModel.put(sampleData[1])
            cacheModel.put(sampleData[2])

        when:
            int numberOfFiles = cacheModel.getNumberOfFiles()

        then:
            numberOfFiles == 3
    }

    void "Should remove all data"()
    {
        given:
            cacheModel.put(sampleData[0])
            cacheModel.put(sampleData[1])

        when:
            cacheModel.removeAllData()

        then:
            !cacheModel.contains(sampleData[0].getFilePath())
            !cacheModel.contains(sampleData[1].getFilePath())
            cacheModel.getNumberOfFiles()==0
    }

    void "Should get size of cache model in bytes"()
    {
        given:
            cacheModel.put(sampleData[0])

        when:
            double size = cacheModel.getSizeInBytes()

        then:
            size>0
    }

    void "Should remove two oldest files"()
    {
        given:
            cacheModel.put(sampleData[0])
            Thread.sleep(10)
            cacheModel.put(sampleData[1])
            Thread.sleep(10)
            cacheModel.put(sampleData[2])

        when:
            cacheModel.removeOldestFile()
            cacheModel.removeOldestFile()

        then:
            !cacheModel.contains(sampleData[0].getFilePath())
            !cacheModel.contains(sampleData[1].getFilePath())
            cacheModel.contains(sampleData[2].getFilePath())
            cacheModel.getNumberOfFiles()==1
    }
}