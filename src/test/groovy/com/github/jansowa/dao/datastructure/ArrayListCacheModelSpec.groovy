package com.github.jansowa.dao.datastructure

import com.github.jansowa.dao.CacheModel
import com.github.jansowa.domain.FileBasicInfo
import spock.lang.Shared
import spock.lang.Specification

class ArrayListCacheModelSpec extends Specification{
    @Shared ArrayListCacheModel cacheModel
    @Shared private FileBasicInfo[] sampleData
    static final double MAX_STORAGE = 2
    static final String CACHE_PATH = "ArrayListCacheModelTest.ser"

    def setupSpec()
    {
        sampleData = new FileBasicInfo[3]
        sampleData[0] = generateFileBasicInfo("sample0", "/test/")
        sampleData[1] = generateFileBasicInfo("sample1", "/test/")
        sampleData[2] = generateFileBasicInfo("sample2", "/test/")
    }

    def setup()
    {
        cacheModel = new ArrayListCacheModel(MAX_STORAGE, CACHE_PATH)
    }

    def cleanup()
    {
        cacheModel.removeFromDevice()
    }

    private static FileBasicInfo generateFileBasicInfo(String fileName, String folderPath){
        Date creationDate = new Date()
        return new FileBasicInfo(fileName, folderPath+fileName+".txt", "txt", "http://example.com/"+fileName+".txt", creationDate, creationDate)
    }

    void "Should upload file"()
    {
        when:
            cacheModel.put(sampleData[0])

        then:
            cacheModel.contains(sampleData[0].getFilePath())
            cacheModel.getNumberOfFiles()==1
    }

    void "Should delete uploaded file"()
    {
        given:
            cacheModel.put(sampleData[0])

        when:
            cacheModel.remove(sampleData[0].getFilePath())

        then:
            !cacheModel.contains(sampleData[0].getFilePath())
            cacheModel.getNumberOfFiles()==0
    }

    void "Should read file"()
    {
        given:
            cacheModel.put(sampleData[0])
        when:
            Optional<FileBasicInfo> downloadedInfo = cacheModel.read(sampleData[0].getFilePath())
        then:
            downloadedInfo.get().creationTime == sampleData[0].creationTime
            downloadedInfo.get().name == sampleData[0].name
            cacheModel.getNumberOfFiles()==1
    }

    void "Read method should return Optional with null if there is no file in given path"()
    {
        given:
            String readTestPath = "/test/read"

        when:
            Optional<FileBasicInfo> downloadedInfo= cacheModel.read(readTestPath)

        then:
           Optional.ofNullable(null) == downloadedInfo
            cacheModel.getNumberOfFiles()==0
    }

    void "Contain method should return true if model contains file"()
    {
        given:
            cacheModel.put(sampleData[0])

        when:
            boolean result = cacheModel.contains(sampleData[0].getFilePath())

        then:
            result
            cacheModel.getNumberOfFiles()==1
    }

    void "Contain method should return false if model doesn't contain file"()
    {
        given:
            String containsTestPath = "/test/contains"

        when:
            boolean result = cacheModel.contains(containsTestPath)

        then:
            !result
            cacheModel.getNumberOfFiles()==0
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
            !cacheModel.contains(sampleData[0].getFilePath())
            cacheModel.getNumberOfFiles()==1
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

    void "Should save cache on device"()
    {
        given:
            File cacheModelFile = new File(CACHE_PATH)
            cacheModel.put(sampleData[0])
        when:
            cacheModel.saveData()
        then:
            cacheModelFile.exists()
            cacheModel.getNumberOfFiles()==1
    }

    void "Should load cache from device"()
    {
        given:
            cacheModel.put(sampleData[0])
            cacheModel.put(sampleData[1])
            cacheModel.saveData()
            ArrayListCacheModel loadedCacheModel = new ArrayListCacheModel(MAX_STORAGE, CACHE_PATH)
        when:
            loadedCacheModel.loadData()
        then:
            loadedCacheModel != null
            loadedCacheModel
                    .read(sampleData[0]
                    .filePath)
                    .get()
                    .getName() ==
                    sampleData[0].getName()
            loadedCacheModel
                    .read(sampleData[1]
                    .filePath)
                    .get()
                    .getCreationTime() ==
                    sampleData[1].getCreationTime()
            loadedCacheModel.getNumberOfFiles()==2
    }

    void "Should remove cache from device"()
    {
        given:
            cacheModel.saveData()
        when:
            cacheModel.removeFromDevice()
            File cacheModelFile = new File(CACHE_PATH)
        then:
            !cacheModelFile.exists()
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
}