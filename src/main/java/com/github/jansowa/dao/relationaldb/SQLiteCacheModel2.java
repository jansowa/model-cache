package com.github.jansowa.dao.relationaldb;

import com.github.jansowa.dao.CacheModel;
import com.github.jansowa.domain.FileBasicInfo;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.logging.Logger;

//one table for each folder
public class SQLiteCacheModel2 implements CacheModel {
    @Getter @Setter private long maxNumberOfFiles;
    private String cacheModelPath;
    private Connection connection;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");

    public SQLiteCacheModel2(long maxNumberOfFiles, String cacheModelPath){
        this.maxNumberOfFiles = maxNumberOfFiles;
        this.cacheModelPath = cacheModelPath;
        getConnection();
    }

    @Override
    public void put(FileBasicInfo file) {
    
    }

    @Override
    public void remove(String filePath) {

    }

    @Override
    public boolean contains(String filePath) {
        return false;
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
        return 0;
    }

    @Override
    public void removeAllData() {

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

    private void getConnection(){
        try{
            if(connection == null || connection.isClosed()) {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:" + cacheModelPath);
            }
        } catch (ClassNotFoundException | SQLException e) {
            Logger logger = Logger.getLogger("getConnectionLogger");
            logger.warning(e.toString());
        }
    }

    private void createTableForFolder(String folderPath){
        getConnection();
        PreparedStatement createTableStatement = null;
        try{
            createTableStatement = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS ?(id INTEGER PRIMARY KEY,"+
                            "name TEXT,"+
                            "filePath TEXT,"+
                            "extension TEXT,"+
                            "url TEXT,"+
                            "creationTime INTEGER,"+
                            "lastUsageTime INTEGER);");
            createTableStatement.setString(1, folderPath);
            createTableStatement.execute();
        } catch (SQLException e){
            Logger logger = Logger.getLogger("createTableForFolder method logger");
            logger.warning(e.toString());
        } finally {
            try {
                if (createTableStatement != null) {
                    createTableStatement.close();
                }
            } catch (SQLException e){
                Logger logger = Logger.getLogger("createTableForFolder method logger");
                logger.warning(e.toString());
            }
        }
    }

    protected String getFolderFromPath(String path){
        int lastIndexOfFolder = path.lastIndexOf('/');
        return path.substring(0, lastIndexOfFolder);
    }
}
