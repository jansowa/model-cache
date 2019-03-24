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

import java.util.*;
import java.util.logging.Logger;

public class HashMapCacheModel implements CacheModel, Serializable{
    @Getter private long maxNumberOfFiles;
    private String cacheModelPath;
    private HashMap<String, FileBasicInfo> storedFiles;

    public HashMapCacheModel(long maxNumberOfFiles, String cacheModelPath){
        this.maxNumberOfFiles = maxNumberOfFiles;
        this.cacheModelPath = cacheModelPath;
        this.storedFiles = new HashMap<>();
    }

    @Override
    public void put(FileBasicInfo file) {
        storedFiles.put(file.getFilePath(), file);
        if(getNumberOfFiles()>getMaxNumberOfFiles()) {
            remove(findOldestFile());
        }
        saveData();
    }

    @Override
    public void remove(String filePath) {
        storedFiles.remove(filePath);
        saveData();
    }

    @Override
    public boolean contains(String filePath) {
        return storedFiles.containsKey(filePath);
    }

    @Override
    public void movePath(String sourcePath, String destinationPath) {
        ArrayList<String> pathsToRemove = new ArrayList<>();
        HashMap<String, FileBasicInfo> entriesToAdd = new HashMap<>();
        storedFiles
                .keySet()
                .stream()
                .filter(path -> path.startsWith(sourcePath))
                .forEach(path -> {
                    pathsToRemove.add(path);
                    String finalPath = destinationPath +
                            path.substring(sourcePath.length());
                    FileBasicInfo fileToAdd = storedFiles
                                    .get(path)
                                    .withLastUsageTime(new Date())
                                    .withFilePath(finalPath);
                    entriesToAdd.put(finalPath, fileToAdd);
                });
        storedFiles.putAll(entriesToAdd);
        storedFiles.keySet().removeAll(pathsToRemove);
        saveData();
    }

    @Override
    public Optional<FileBasicInfo> read(String filePath) {
        if (!storedFiles.containsKey(filePath)) {
            return Optional.ofNullable(null);
        }
        FileBasicInfo downloadedFile = storedFiles.get(filePath);
        downloadedFile.withLastUsageTime(new Date());
        storedFiles.remove(filePath);
        storedFiles.put(filePath, downloadedFile);
        return Optional.ofNullable(storedFiles.get(filePath));
    }

    @Override
    public int getNumberOfFiles() {
        return storedFiles.size();
    }

    @Override
    public void removeAllData() {
        storedFiles.clear();
    }

    @Override
    public long getSizeInBytes(){
        File cacheModel = new File(cacheModelPath);
        return cacheModel.length();
    }

    @Override
    public void setMaxNumberOfFiles(long maxNumberOfFiles){
        this.maxNumberOfFiles = maxNumberOfFiles;
        saveData();
    }

    public void removeFromDevice(){
        File cacheModel = new File(cacheModelPath);
        if(!cacheModel.delete()){
            System.out.println("File "+cacheModelPath+" doesn't exist!");
        }
    }

    protected void saveData(){
        try (ObjectOutputStream objectOutputStream =
                     new ObjectOutputStream(
                             new FileOutputStream(cacheModelPath))){
            objectOutputStream.writeObject(storedFiles);
        } catch (IOException e) {
            Logger logger = Logger.getLogger(ArrayListCacheModel.class.getName());
            logger.warning("HashMap cache can't save data into device.");
        }
    }

    protected void loadData(){
        try(ObjectInputStream objectInputStream =
                    new ObjectInputStream(
                            new FileInputStream(cacheModelPath))){

            this.storedFiles = (HashMap<String, FileBasicInfo>) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            Logger logger = Logger.getLogger(ArrayListCacheModel.class.getName());
            logger.warning("HashMap cache can't load data from device.");
        }
    }

    private String findOldestFile(){
        return storedFiles
                .values()
                .stream()
                .min((file1, file2) -> file1.getLastUsageTime().compareTo(file2.getLastUsageTime()))
                .map(FileBasicInfo::getFilePath)
                .orElse(null);
    }
}