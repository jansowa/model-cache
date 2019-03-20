package com.github.jansowa.dao.relationaldb;

import com.github.jansowa.dao.CacheModel;
import com.github.jansowa.domain.FileBasicInfo;
import lombok.Getter;
import lombok.Setter;

import java.io.File;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.Statement;

import java.util.Date;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;

//All data in one table
public class SQLiteCacheModel1 implements CacheModel {
    @Getter @Setter private long maxNumberOfFiles;
    private String cacheModelPath; //TODO fix - cacheModelPath now can't include slash
    private Connection connection;
    private final Properties pooledStatements;

    public SQLiteCacheModel1(long maxNumberOfFiles, String cacheModelPath){
        this.maxNumberOfFiles = maxNumberOfFiles;
        this.cacheModelPath = cacheModelPath;
        pooledStatements = new Properties();
        pooledStatements
                .setProperty("MaxPooledStatements", "10");
        getConnection();
    }

    @Override
    public void put(FileBasicInfo file) {
        getConnection();
        PreparedStatement putStatement = null;
        Date lastUsageTime = new Date();

        try {
            putStatement = connection
                    .prepareStatement(
                            "INSERT INTO files VALUES(?, ?, ?, ?, ?, ?, ?);");
            putStatement.setString(1, null);
            putStatement.setString(2, file.getName());
            putStatement.setString(3, file.getFilePath());
            putStatement.setString(4, file.getExtension());
            putStatement.setString(5, file.getUrl());
            putStatement.setLong(6, file.getCreationTime().getTime());
            putStatement.setLong(7, lastUsageTime.getTime());

            putStatement.execute();
            if(getNumberOfFiles() > getMaxNumberOfFiles()){
                removeOldestFile();
            }
        } catch (SQLException e) {
            log(e);
        } finally {
            SQLiteHelper.close(putStatement);
        }

    }

    protected void removeOldestFile() {
        getConnection();
        Statement removeStatement = null;

        try {
            removeStatement = connection.createStatement();
            removeStatement.execute(
                    "DELETE FROM files " +
                            "WHERE lastUsageTime = " +
                            "(SELECT min(lastUsageTime)" +
                            "FROM files)"
            );
        } catch (SQLException e){
            log(e);
        } finally {
            SQLiteHelper.close(removeStatement);
        }
    }

    @Override
    public void remove(String filePath) {
        getConnection();
        PreparedStatement removeStatement = null;

        try{
            removeStatement = connection
                    .prepareStatement(
                            "DELETE FROM files WHERE filePath=?");
            removeStatement.setString(1, filePath);
            removeStatement.execute();

        } catch (SQLException e) {
            log(e);
        } finally{
            SQLiteHelper.close(removeStatement);
        }
    }

    @Override
    public boolean contains(String filePath) {
        getConnection();
        PreparedStatement containsStatement = null;
        ResultSet filesWithGivenPath = null;
        boolean isFileInDB = false;

        try {
            containsStatement = connection.prepareStatement("SELECT id FROM files WHERE filePath=?");
            containsStatement.setString(1, filePath);
            filesWithGivenPath = containsStatement.executeQuery();
            isFileInDB = filesWithGivenPath.next();
        } catch (SQLException e) {
            log(e);
        } finally {
            SQLiteHelper.close(containsStatement);
            SQLiteHelper.close(filesWithGivenPath);
        }
        return isFileInDB;
    }

    @Override
    public void movePath(String sourcePath, String destinationPath) {
        getConnection();
        Statement moveStatement = null;

        try{
            int sourcePathLength = sourcePath.length();
            moveStatement = connection.createStatement();
            moveStatement.execute("UPDATE files "+
                    "SET filePath = '" +destinationPath+"' || substr(filePath, "+(sourcePathLength+1)+") "+
                    " WHERE substr(filePath, 1, " + sourcePathLength +") = '"+sourcePath+"'");
        } catch (SQLException e) {
            log(e);
        } finally {
            SQLiteHelper.close(moveStatement);
        }
    }


    @Override
    public Optional<FileBasicInfo> read(String filePath) {
        getConnection();
        ResultSet readedData = null;
        PreparedStatement readStatement = null;

        try {
            readStatement = connection
                    .prepareStatement(
                            "SELECT * FROM files "+
                            "WHERE filePath = ?");
            readStatement.setString(1, filePath);
            readedData = readStatement.executeQuery();

            if(readedData.next()){
                Date lastUsageTime = new Date();
                Date creationTime = new Date(readedData
                                .getLong("creationTime"));
                FileBasicInfo readedFile = FileBasicInfo
                        .builder()
                        .name(readedData.getString("name"))
                        .filePath(readedData.getString("filePath"))
                        .extension(readedData.getString("extension"))
                        .url(readedData.getString("url"))
                        .creationTime(creationTime)
                        .lastUsageTime(lastUsageTime)
                        .build();

                updateLastUsageTime(filePath, lastUsageTime);

                return Optional.ofNullable(readedFile);
            }
        } catch (SQLException e) {
            log(e);
        } finally{
            SQLiteHelper.close(readStatement);
            SQLiteHelper.close(readedData);
        }
        return Optional.empty();
    }

    @Override
    public int getNumberOfFiles() {
        getConnection();
        Statement countStatement = null;
        ResultSet countResult = null;
        int numberOfFiles=-1;

        try {
            countStatement = connection.createStatement();
            countResult = countStatement.executeQuery("SELECT count(*) FROM files");
            countResult.next();
            numberOfFiles = countResult.getInt(1);
        } catch (SQLException e) {
            log(e);
        } finally {
            SQLiteHelper.close(countStatement);
            SQLiteHelper.close(countResult);
        }

        return numberOfFiles;
    }

    @Override
    public void removeAllData() {
        getConnection();
        Statement removeAllStatement = null;

        try {
            removeAllStatement = connection.createStatement();
            removeAllStatement.execute("DROP TABLE files");
            initialise();
        } catch (SQLException e) {
            log(e);
        } finally{
            SQLiteHelper.close(removeAllStatement);
        }
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

    public void closeConnection(){
        SQLiteHelper.close(connection);
    }

    private void getConnection(){
        Statement pragmaStatement = null;

        try{
            if(connection == null || connection.isClosed()) {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:" + cacheModelPath, pooledStatements);
                pragmaStatement = connection.createStatement();
                pragmaStatement.execute("PRAGMA synchronous = OFF");
            }
        } catch (ClassNotFoundException | SQLException e) {
            log(e);
        } finally {
            SQLiteHelper.close(pragmaStatement);
        }
        initialise();
    }

    private void initialise(){
        Statement createTableStatement = null;

        try {
            createTableStatement = connection.createStatement();
            createTableStatement.executeUpdate("CREATE TABLE IF NOT EXISTS files(id INTEGER PRIMARY KEY,"+
                                                    "name TEXT,"+
                                                    "filePath TEXT,"+
                                                    "extension TEXT,"+
                                                    "url TEXT,"+
                                                    "creationTime INTEGER,"+
                                                    "lastUsageTime INTEGER);");
        } catch (SQLException e) {
            log(e);
        } finally {
            SQLiteHelper.close(createTableStatement);
        }
    }

    private void updateLastUsageTime(String filePath, Date readDate) {
        PreparedStatement updateTimeStatement = null;
        try {
            updateTimeStatement = connection.prepareStatement(
                    "UPDATE files "+
                            "SET lastUsageTime = ? "+
                            "WHERE filePath = ?");
            updateTimeStatement.setLong(1, readDate.getTime());
            updateTimeStatement.setString(2, filePath);
            updateTimeStatement.execute();
        } catch (SQLException e) {
            log(e);
        } finally {
            SQLiteHelper.close(updateTimeStatement);
        }
    }

    private void log(Exception e){
        Logger logger = Logger.getLogger("SQLiteCacheModel1 logger");
        logger.warning(e.toString());
    }
}