package com.github.jansowa.dao.relationaldb

import com.github.jansowa.domain.FileBasicInfo
import spock.lang.Shared
import spock.lang.Specification

import java.sql.DatabaseMetaData
import java.sql.ResultSet

class SQLiteCacheModel2Spec extends Specification{
    @Shared SQLiteCacheModel2 cacheModel
    @Shared private FileBasicInfo[] sampleData
    static final long MAX_NUMBER_OF_FILES = 1000
    static final String CACHE_PATH = "SQLiteCacheModel2Test.db"

    def setupSpec()
    {
        sampleData = new FileBasicInfo[3]
        sampleData[0] = generateFileBasicInfo("sample0", "/test/")
        Thread.sleep(300)
        sampleData[1] = generateFileBasicInfo("sample1", "/test/")
        Thread.sleep(300)
        sampleData[2] = generateFileBasicInfo("sample2", "/test/")
    }

    def setup()
    {
        cacheModel = new SQLiteCacheModel2(MAX_NUMBER_OF_FILES, CACHE_PATH)
    }

    def cleanup()
    {
        cacheModel.removeFromDevice()
    }

    private static FileBasicInfo generateFileBasicInfo(String fileName, String folderPath){
        Date creationDate = new Date()
        return new FileBasicInfo(fileName, folderPath+fileName+".txt", "txt", "http://example.com/"+fileName+".txt", creationDate, creationDate)
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
    void "Should upload file"()
    {
        when:
            cacheModel.put(sampleData[0])

        then:
            cacheModel.contains(sampleData[0].getFilePath())
    }
    void "Should read file"()
    {
        given:
            cacheModel.put(sampleData[0])

        when:
            Optional<FileBasicInfo> downloadedInfo = cacheModel.read(sampleData[0].getFilePath())

        then:
            downloadedInfo.get().name == sampleData[0].name
            cacheModel.getNumberOfFiles()==1
            downloadedInfo.get().creationTime == sampleData[0].creationTime
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
            String destinationPath = "/test/destination/file.png"
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
            cacheModel.put(generateFileBasicInfo("sample4", "/anotherFolder/"))

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

    void "Should remove cache from device"()
    {
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

    void "Should remove oldest file"(){
        given:
            cacheModel.put(sampleData[0])
            cacheModel.put(sampleData[1])
            cacheModel.put(sampleData[2])

        when:
            cacheModel.removeOldestFile()
            cacheModel.removeOldestFile()

        then:
            cacheModel.contains(sampleData[2].filePath)
            !cacheModel.contains(sampleData[1].filePath)
            !cacheModel.contains(sampleData[0].filePath)
            cacheModel.getNumberOfFiles()==1
    }

    void "Should return folder from paths"()
    {
        given:
            String path1 = "/first/second/file.txt"
            String path2 = "/folder/test.jpg"
            String path3 = "/src/main/java/main.java"

        when:
            String folder1 = cacheModel.getFolderFromPath(path1)
            String folder2 = cacheModel.getFolderFromPath(path2)
            String folder3 = cacheModel.getFolderFromPath(path3)

        then:
            folder1 == "/first/second"
            folder2 == "/folder"
            folder3 == "/src/main/java"
    }

    void "Should return file name from paths"()
    {
        given:
            String path1 = "/first/second/file.txt"
            String path2 = "/folder/test.jpg"
            String path3 = "/src/main/java/main"

        when:
            String name1 = cacheModel.getNameFromPath(path1)
            String name2 = cacheModel.getNameFromPath(path2)
            String name3 = cacheModel.getNameFromPath(path3)

        then:
            name1 == "file"
            name2 == "test"
            name3 == "main"
    }

    void "Should return file extension from paths"()
    {
        given:
            String path1 = "/first/second/file.txt"
            String path2 = "/folder/test.jpg"
            String path3 = "/src/main/java/main"

        when:
            String extension1 = cacheModel.getExtensionFromPath(path1)
            String extension2 = cacheModel.getExtensionFromPath(path2)
            String extension3 = cacheModel.getExtensionFromPath(path3)

        then:
            extension1 == "txt"
            extension2 == "jpg"
            extension3 == null
    }

    void "Should add table to database"()
    {
        given:
            String tableName = "/testTable"
        when:
            cacheModel.createTableForFolder(tableName)
            cacheModel.getConnection()

        then:
            checkIfTableExists(tableName)
    }

    void "Should remove empty table and leave not empty table"()
    {
        given:
            String emptyTablePath = "/emptyTable/"
            String sampleDataFolderPath = cacheModel.getFolderFromPath(sampleData[0].getFilePath())

            cacheModel.createTableForFolder(emptyTablePath)
            cacheModel.put(sampleData[0])

        when:
            cacheModel.removeTableIfEmpty(emptyTablePath)
            cacheModel.removeTableIfEmpty(sampleDataFolderPath)

        then:
            !checkIfTableExists(emptyTablePath)
            checkIfTableExists(sampleDataFolderPath)
    }

    boolean checkIfTableExists(String tableName){
        DatabaseMetaData databaseMetaData = cacheModel.connection.getMetaData()
        ResultSet tables = databaseMetaData.getTables(null, null, tableName, null)
        return tables.next()
    }
}