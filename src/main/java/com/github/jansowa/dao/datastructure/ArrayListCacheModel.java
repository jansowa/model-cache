package com.github.jansowa.dao.datastructure;

import com.github.jansowa.dao.CacheModel;
import com.github.jansowa.domain.FileBasicInfo;

import lombok.Getter;

import java.io.File;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.io.Serializable;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ArrayListCacheModel implements CacheModel, Serializable {
    @Getter private long maxNumberOfFiles;
    private String cacheModelPath;
    private ArrayList<FileBasicInfo> storedFiles;

    public ArrayListCacheModel(long maxNumberOfFiles, String cacheModelPath){
        this.maxNumberOfFiles = maxNumberOfFiles;
        this.cacheModelPath = cacheModelPath;
        this.storedFiles = new ArrayList<>();
    }

    @Override
    public void put(FileBasicInfo file) {
        storedFiles.add(file);
        if (getNumberOfFiles() > getMaxNumberOfFiles()){
            storedFiles.remove(0);
            saveData();
        }
        saveData();
    }

    @Override
    public long getSizeInBytes(){
        File cacheModel = new File(cacheModelPath);
        return cacheModel.length();
    }

    @Override
    public void remove(String filePath) {
        getByPath(filePath)
                .ifPresent(storedFiles::remove);
    }

    @Override
    public boolean contains(String filePath) {
        return storedFiles
                .stream()
                .anyMatch(file -> filePath.equals(file.getFilePath()));
    }

    @Override
    public void movePath(String sourcePath, String destinationPath) {
        List<FileBasicInfo> filesToMove = storedFiles
                .stream()
                .filter(file -> file
                        .getFilePath()
                        .startsWith(sourcePath))
                .collect(Collectors.toList());
        storedFiles.removeAll(filesToMove);
        filesToMove = filesToMove
                .stream()
                .map(file -> {
                    String currentPath = file.getFilePath();
                    String finalPath = destinationPath +
                            currentPath.substring(sourcePath.length());
                    return file.withLastUsageTime(new Date())
                            .withFilePath(finalPath);
                }).collect(Collectors.toList());
        storedFiles.addAll(filesToMove);
        saveData();
    }

    @Override
    public Optional<FileBasicInfo> read(String filePath) {
        Optional<FileBasicInfo> downloadedFileOpt = getByPath(filePath);
        if(downloadedFileOpt.isPresent()){
            FileBasicInfo downloadedFile = downloadedFileOpt.get();
            storedFiles.remove(downloadedFile);
            downloadedFile = downloadedFile.withLastUsageTime(new Date());
            storedFiles.add(downloadedFile);
        }
        return downloadedFileOpt;
    }

    @Override
    public int getNumberOfFiles() {
        return storedFiles.size();
    }

    @Override
    public void removeAllData() {
        storedFiles.clear();
        saveData();
    }

    public void removeFromDevice(){
        File cacheModel = new File(cacheModelPath);
        if(!cacheModel.delete()){
            Logger logger = Logger.getLogger("removeFromDevice logger");
            logger.log(Level.INFO, "File {0} doesn't exist!", cacheModelPath);
        }
    }

    protected void saveData(){
        try (ObjectOutputStream objectOutputStream =
                     new ObjectOutputStream(
                                new FileOutputStream(cacheModelPath))){
            objectOutputStream.writeObject(storedFiles);
        } catch (IOException e) {
            Logger logger = Logger.getLogger(ArrayListCacheModel.class.getName());
            logger.warning("ArrayList cache can't save data into device.");
        }
    }

    public void loadData(){
        try(ObjectInputStream objectInputStream =
                    new ObjectInputStream(
                            new FileInputStream(cacheModelPath))){

            this.storedFiles = (ArrayList<FileBasicInfo>) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            Logger logger = Logger.getLogger(ArrayListCacheModel.class.getName());
            logger.warning("ArrayList cache can't load data from device.");
        }
    }

    public void setMaxNumberOfFiles(long maxNumberOfFiles){
        this.maxNumberOfFiles = maxNumberOfFiles;
        saveData();
    }

    public void setCacheModelPath(String cacheModelPath){
        this.cacheModelPath = cacheModelPath;
        saveData();
    }

    private Optional<FileBasicInfo> getByPath(String filePath) {
        return storedFiles.stream()
                .filter(file -> filePath.equals(file.getFilePath()))
                .findFirst();
    }
}