package com.github.jansowa.dao.nonrelationaldb;

import com.github.jansowa.domain.NitriteFileBasicInfo;

import lombok.Getter;
import lombok.Setter;

import org.dizitart.no2.FindOptions;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.SortOrder;
import org.dizitart.no2.objects.Cursor;
import org.dizitart.no2.objects.ObjectRepository;
import org.dizitart.no2.objects.filters.ObjectFilters;

import java.io.File;
import java.util.Date;
import java.util.Optional;

public class NitriteCacheModel2 {
    private String cacheModelPath;
    @Getter @Setter private long maxNumberOfFiles;
    private Nitrite database;
    private ObjectRepository<NitriteFileBasicInfo> files;

    public NitriteCacheModel2(long maxNumberOfFiles, String cacheModelPath){
        this.maxNumberOfFiles = maxNumberOfFiles;
        this.cacheModelPath = cacheModelPath;
        initiateDatabase();
    }

    public void put(NitriteFileBasicInfo file) {
        file.setLastUsageTime(new Date());
        files.insert(file);
        if(maxNumberOfFiles<getNumberOfFiles()){
            removeOldestFile();
        }
    }

    public void remove(String filePath) {
        files.remove(ObjectFilters.eq("filePath", filePath));
    }

    public boolean contains(String filePath) {
        Cursor<NitriteFileBasicInfo> foundFiles = files.find(ObjectFilters.eq("filePath", filePath));

        NitriteFileBasicInfo singleFile = foundFiles.firstOrDefault();
        if(singleFile==null){
            return false;
        }
        singleFile.setLastUsageTime(new Date());
        files.update(singleFile);
        return true;
    }

    public void movePath(String sourcePath, String destinationPath) {
        Cursor<NitriteFileBasicInfo> filesToMove = files.find(ObjectFilters.regex("filePath", "^" + sourcePath + ".*"));
        for(NitriteFileBasicInfo singleFile: filesToMove){
            String currentFilePath = singleFile.getFilePath();
            String finalFilePath = destinationPath + currentFilePath.substring(sourcePath.length());
            NitriteFileBasicInfo finalInfo = new NitriteFileBasicInfo(
                    singleFile.getName(),
                    finalFilePath,
                    singleFile.getExtension(),
                    singleFile.getUrl(),
                    singleFile.getCreationTime(),
                    new Date());
            files.update(ObjectFilters.eq("filePath", currentFilePath),
                    finalInfo);
        }
    }

    public Optional<NitriteFileBasicInfo> read(String filePath) {
        Cursor<NitriteFileBasicInfo> foundFiles = files.find(ObjectFilters.eq("filePath", filePath));
        NitriteFileBasicInfo fileInfos = foundFiles.firstOrDefault();
        if(fileInfos!=null){
            fileInfos.setLastUsageTime(new Date());
            files.update(fileInfos);
        }
        return Optional.ofNullable(fileInfos);
    }

    public int getNumberOfFiles() {
        return (int) files.size();
    }

    public void removeAllData() {
        removeFromDevice();
        initiateDatabase();
    }

    public long getSizeInBytes() {
        File cacheModel = new File(cacheModelPath);
        return cacheModel.length();
    }

    public void removeFromDevice() {
        File cacheModel = new File(cacheModelPath);
        if(!cacheModel.delete()){
            System.out.println("File "+cacheModelPath+" doesn't exist!");
        }
    }

    private void initiateDatabase() {
        database = Nitrite
                .builder()
                .compressed()
                .filePath(cacheModelPath)
                .openOrCreate("user", "password");
        files = database.getRepository(NitriteFileBasicInfo.class);
    }

    private void removeOldestFile(){
        NitriteFileBasicInfo oldestFile = files.find(FindOptions.sort("lastUsageTime", SortOrder.Ascending))
                .firstOrDefault();
        files.remove(oldestFile);
    }
}