package com.github.jansowa.dao.datastructure;

import com.github.jansowa.dao.CacheModel;
import com.github.jansowa.domain.FileBasicInfo;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;


//TODO(IMPORTANT!!!) serialize data like in ArrayListCacheModel! Make save and load protected functions!
public class HashMapCacheModel implements CacheModel, Serializable{
    private transient double maxStorageMB;
    private transient String cacheModelPath;
    private HashMap<String, FileBasicInfo> storedFiles;

    public HashMapCacheModel(double maxStorageMB, String cacheModelPath){
        this.maxStorageMB = maxStorageMB;
        this.cacheModelPath = cacheModelPath;
        this.storedFiles = new HashMap<>();
    }

    @Override
    public void put(FileBasicInfo file) {
        storedFiles.put(file.getFilePath(), file);
        while(getSizeInBytes()>1048576*maxStorageMB)
            remove(findEldestFile());
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
        if(storedFiles.containsKey(sourcePath)) {
            moveSingleFile(sourcePath, destinationPath);
        }
        else{
            moveWholeFolder(sourcePath, destinationPath);
        }
        saveData();
    }

    private void moveSingleFile(String sourcePath, String destinationPath) {
        FileBasicInfo fileToMove = storedFiles.remove(sourcePath);
        fileToMove = fileToMove.withLastUsageTime(new Date()).withFilePath(destinationPath);
        storedFiles.put(destinationPath, fileToMove);
    }

    private void moveWholeFolder(String sourcePath, String destinationPath) {
        int sourcePathLength = sourcePath.length();
        ArrayList<String> pathsToMove = new ArrayList<>();
        for (String key:storedFiles.keySet()) {
            if(key.substring(0, sourcePathLength).equals(sourcePath)){
                pathsToMove.add(key);
            }
        }
        for (String path: pathsToMove) {
            moveSingleFile(path, destinationPath+path.substring(sourcePathLength));
        }

    }

    @Override
    public Optional<FileBasicInfo> read(String filePath) {
        if (!storedFiles.containsKey(filePath))
            return Optional.ofNullable(null);
        FileBasicInfo downloadedFile = storedFiles.get(filePath);
        downloadedFile.withLastUsageTime(new Date());
        storedFiles.remove(filePath);
        storedFiles.put(filePath, downloadedFile);
        return Optional.ofNullable(storedFiles.get(filePath)); //TODO what if filePath empty?
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
    public double getSizeInBytes(){
        File cacheModel = new File(cacheModelPath);
        return cacheModel.length();
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

    private String findEldestFile(){
        Date eldestDate = new Date(2200, 1, 1);
        String pathOfEldestFile = null;
        for (FileBasicInfo file: storedFiles.values()) {
            if(file.getLastUsageTime().getTime()<eldestDate.getTime()){
                eldestDate = file.getLastUsageTime();
                pathOfEldestFile = file.getFilePath();
            }
        }
        return pathOfEldestFile;
    }
}
