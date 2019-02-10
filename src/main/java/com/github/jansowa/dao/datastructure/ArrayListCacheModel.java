package com.github.jansowa.dao.datastructure;

import com.github.jansowa.dao.CacheModel;
import com.github.jansowa.domain.FileBasicInfo;
import lombok.Getter;

import java.io.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        int numberOfFiles = getNumberOfFiles();
        for (int i = 0; i < numberOfFiles; i++) {
            if(storedFiles.get(i).getFilePath().equals(filePath)){
                storedFiles.remove(i);
                saveData();
                break;
            }
        }
    }

    @Override
    public boolean contains(String filePath) {
        for (FileBasicInfo file: storedFiles) {
            if(file.getFilePath().equals(filePath)){
                return true;
            }
        }
        return false;
    }

    @Override
    public void movePath(String sourcePath, String destinationPath) {
        int index = findIndexByPath(sourcePath);
        if (index>0) {
            moveSingleFile(destinationPath, index);
        }
        else {
            moveWholeFolder(sourcePath, destinationPath);
        }
        saveData();
    }

    @Override
    public Optional<FileBasicInfo> read(String filePath) {
        int index = findIndexByPath(filePath);
        if(index==-1)
            return Optional.empty();
        FileBasicInfo downloadedFile = storedFiles
                                        .get(index)
                                        .withLastUsageTime(new Date());
        storedFiles.add(downloadedFile);
        storedFiles.remove(index);
        saveData();
        return Optional.of(downloadedFile);
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

    protected void loadData(){
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

    private int findIndexByPath(String filePath){
        int numberOfFiles = getNumberOfFiles();

        for (int i = numberOfFiles-1; i >= 0; i--)
            if(storedFiles
                    .get(i)
                    .getFilePath()
                    .equals(filePath))
                return i;

        return -1;
    }

    private void moveSingleFile(String destinationPath, int index) {
        FileBasicInfo fileToMove = storedFiles
                .get(index)
                .withFilePath(destinationPath)
                .withLastUsageTime(new Date());
        storedFiles.add(fileToMove);
        storedFiles.remove(index);
    }

    private void moveWholeFolder(String sourcePath, String destinationPath){
        int sourcePathLength = sourcePath.length();
        int numberOfFiles = getNumberOfFiles();
        for(int i=0; i<numberOfFiles; i++){
            String singleFilePath = storedFiles
                    .get(i)
                    .getFilePath();
            if(singleFilePath.length()>=sourcePathLength && singleFilePath
                    .substring(0, sourcePathLength)
                    .equals(sourcePath)){
                moveSingleFile(destinationPath+
                        singleFilePath
                        .substring(sourcePathLength), i);
                i--;
                numberOfFiles--;
            }
        }
    }
}