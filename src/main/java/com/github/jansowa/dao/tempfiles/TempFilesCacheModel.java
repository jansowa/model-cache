package com.github.jansowa.dao.tempfiles;

import com.github.jansowa.dao.CacheModel;
import com.github.jansowa.domain.FileBasicInfo;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.Optional;
import java.util.logging.Logger;

public class TempFilesCacheModel implements CacheModel {
    private String cacheModelPath;
    @Getter @Setter private long maxNumberOfFiles;

    public TempFilesCacheModel(long maxNumberOfFiles, String cacheModelPath){
        this.cacheModelPath = cacheModelPath;
        this.maxNumberOfFiles = maxNumberOfFiles;
        try {
            FileUtils.forceMkdir(new File(cacheModelPath));
        } catch (IOException e) {
            log(e);
        }
    }

    @Override
    public void put(FileBasicInfo file) {
        File fileToSave = new File(cacheModelPath+file.getFilePath());
        String fileData = file.getUrl() + "\n"
                        + file.getCreationTime().getTime() + "\n";
        try {
            FileUtils.writeStringToFile(fileToSave, fileData, "UTF-8", false);
        } catch (IOException e) {
            log(e);
        }
        if(getNumberOfFiles()>maxNumberOfFiles){
            removeOldestFile();
        }
    }

    @Override
    public void remove(String filePath) {
        File fileToRemove = new File(cacheModelPath+filePath);
        FileUtils.deleteQuietly(fileToRemove);
        File parentDirectory = fileToRemove.getParentFile();
        while(cacheModelPath.equals(parentDirectory.getPath()) &&
                FileUtils.sizeOfDirectory(parentDirectory)==0){
            parentDirectory.delete();
            parentDirectory=parentDirectory.getParentFile();
        }
    }

    @Override
    public boolean contains(String filePath) {
        File fullPath = new File(cacheModelPath+filePath);
        return fullPath.exists();
    }

    @Override
    public void movePath(String sourcePath, String destinationPath) {

    }

    @Override
    public Optional<FileBasicInfo> read(String filePath) {
        return Optional.empty();
    }

    @Override
    public int getNumberOfFiles() {
        File cacheModelDirectory = new File(cacheModelPath);
        return countFilesInDirectory(cacheModelDirectory);
    }

    @Override
    public void removeAllData() {
        try {
            FileUtils.cleanDirectory(new File(cacheModelPath));
        } catch (IOException e) {
            log(e);
        }
    }

    @Override
    public long getSizeInBytes() {
        return FileUtils.sizeOfDirectory(new File(cacheModelPath));
    }

    @Override
    public void removeFromDevice() {
        try {
            FileUtils.deleteDirectory(new File(cacheModelPath));
        } catch (IOException e) {
            log(e);
        }
    }

    private void removeOldestFile() {
        File cacheModel = new File(cacheModelPath);
        LinkedList<File> allFiles = (LinkedList<File>) FileUtils.listFiles(
                cacheModel, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        long oldestDate = new Date().getTime();
        File oldestFile = null;
        for (File file: allFiles) {
            if(file.lastModified() < oldestDate){
                oldestDate = file.lastModified();
                oldestFile = file;
            }
        }
        remove(oldestFile
                .getPath()
                .substring(cacheModelPath.length()));
    }

    private int countFilesInDirectory(File directory){
        File[] filesInDirectory = directory.listFiles();
        int numberOfFiles = 0;
        for (File fileInDirectory : filesInDirectory)
            if (fileInDirectory.isDirectory())
                numberOfFiles += countFilesInDirectory(fileInDirectory);
            else
                numberOfFiles++;

        return numberOfFiles;
    }

    private void log(Exception e){
        Logger logger = Logger.getLogger("TempFilesCacheModel logger");
        logger.warning(e.toString());
    }
}