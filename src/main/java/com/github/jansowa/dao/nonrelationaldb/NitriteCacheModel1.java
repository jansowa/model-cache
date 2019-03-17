package com.github.jansowa.dao.nonrelationaldb;

import com.github.jansowa.dao.CacheModel;
import com.github.jansowa.domain.FileBasicInfo;
import lombok.Getter;
import lombok.Setter;
import org.dizitart.no2.*;
import org.dizitart.no2.filters.Filters;

import java.io.File;
import java.util.Date;
import java.util.Optional;

//Cache model with Nitrite database with single document for each file
//and one collection for all documents
public class NitriteCacheModel1 implements CacheModel {
    private String cacheModelPath;
    @Getter @Setter private long maxNumberOfFiles;
    private Nitrite database;
    private NitriteCollection files;

    public NitriteCacheModel1(long maxNumberOfFiles, String cacheModelPath){
        this.maxNumberOfFiles = maxNumberOfFiles;
        this.cacheModelPath = cacheModelPath;
        initiateDatabase();
    }

    @Override
    public void put(FileBasicInfo file) {
        Document fileDocument = fileBasicInfoToDocument(file);
        fileDocument.replace("lastUsageTime", new Date());
        files.insert(fileDocument);
        if(getNumberOfFiles()>maxNumberOfFiles){
            removeOldestFile();
        }
    }

    @Override
    public void remove(String filePath) {
        files.remove(Filters.eq("filePath", filePath));
    }

    @Override
    public boolean contains(String filePath) {
        Cursor foundFiles = files.find(Filters.eq("filePath", filePath));
        return foundFiles.size()>0;
    }

    @Override
    public void movePath(String sourcePath, String destinationPath) {
        Cursor filesToMove = files.find(Filters.regex("filePath", "^" + sourcePath + ".*"));
        for (Document singleFile: filesToMove) {
            String currentFilePath = (String) singleFile.get("filePath");
            String finalFilePath = destinationPath + currentFilePath.substring(sourcePath.length());
            files.update(Filters.eq("filePath", currentFilePath),
                    Document.createDocument("filePath", finalFilePath)
                            .put("lastUsageTime", new Date()));
        }
    }

    @Override
    public Optional<FileBasicInfo> read(String filePath) {
        Cursor foundFiles = files.find(Filters.eq("filePath", filePath));
        Document fileDocument = foundFiles.firstOrDefault();
        if(fileDocument == null){
            return Optional.empty();
        }
        FileBasicInfo fileInfos = documentToFileBasicInfo(fileDocument);
        return Optional.ofNullable(fileInfos);
    }

    @Override
    public int getNumberOfFiles() {
        return (int) files.size();
    }

    @Override
    public void removeAllData() {
        removeFromDevice();
        initiateDatabase();
    }

    @Override
    public long getSizeInBytes() {
        File cacheModel = new File(cacheModelPath);
        return cacheModel.length();
    }

    @Override
    public void removeFromDevice() {
        File cacheModel = new File(cacheModelPath);
        if(!cacheModel.delete()){
            System.out.println("File "+cacheModelPath+" doesn't exist!");
        }
    }

    private void initiateDatabase(){
        database = Nitrite
                .builder()
                .compressed()
                .filePath(cacheModelPath)
                .openOrCreate("user", "password");
        files = database.getCollection("files");
        files.createIndex("filePath", IndexOptions.indexOptions(IndexType.Unique, true));
    }

    private Document fileBasicInfoToDocument(FileBasicInfo fileInfos){
        return Document
                .createDocument("name", fileInfos.getName())
                .put("filePath", fileInfos.getFilePath())
                .put("extension", fileInfos.getExtension())
                .put("url", fileInfos.getUrl())
                .put("creationTime", fileInfos.getCreationTime())
                .put("lastUsageTime", fileInfos.getLastUsageTime());
    }

    private FileBasicInfo documentToFileBasicInfo(Document fileDocument){
        return FileBasicInfo.builder()
                .name((String) fileDocument.get("name"))
                .filePath((String) fileDocument.get("filePath"))
                .extension((String) fileDocument.get("extension"))
                .url((String) fileDocument.get("url"))
                .creationTime((Date) fileDocument.get("creationTime"))
                .lastUsageTime((Date) fileDocument.get("lastUsageTime"))
                .build();
    }

    private void removeOldestFile() {
        Cursor lastFile = files.find(FindOptions
                .sort("lastUsageTime", SortOrder.Ascending)
                .thenLimit(0, 1));
        files.remove(lastFile.firstOrDefault());
    }
}