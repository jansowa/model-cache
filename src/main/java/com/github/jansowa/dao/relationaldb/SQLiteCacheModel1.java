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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.logging.Logger;

//All data in one table
public class SQLiteCacheModel1 implements CacheModel {
    @Getter @Setter private long maxNumberOfFiles;
    private String cacheModelPath;
    private Connection connection;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");

    public SQLiteCacheModel1(long maxNumberOfFiles, String cacheModelPath){
        this.maxNumberOfFiles = maxNumberOfFiles;
        this.cacheModelPath = cacheModelPath;
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
            Logger logger = Logger.getLogger("putFile");
            logger.warning(e.toString());
        } finally {
            try {
                if(putStatement!=null) {
                    putStatement.close();
                }
                connection.close();
            } catch (SQLException e) {
                Logger logger = Logger.getLogger("putFile method logger");
                logger.warning(e.toString());
            }

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
            Logger logger = Logger.getLogger("removeOldestFile method logger");
            logger.warning(e.toString());
        } finally {
            try {
                if (removeStatement != null) {
                    removeStatement.close();
                }
                connection.close();
            } catch (SQLException e){
                Logger logger = Logger.getLogger("removeOldestFile method logger");
                logger.warning(e.toString());
            }
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
            Logger logger = Logger.getLogger("removeMethodLogger");
            logger.warning(e.toString());
        } finally{
            try {
                if (removeStatement != null) {
                    removeStatement.close();
                }
                connection.close();
            } catch(SQLException e){
                Logger logger = Logger.getLogger("remove method logger");
                logger.warning(e.toString());
            }
        }
    }

    @Override
    public boolean contains(String filePath) {
        PreparedStatement containStatement = null;
        ResultSet filesWithGivenPath = null;
        getConnection();
        boolean isFileInDB = false;
        try {
            containStatement = connection.prepareStatement("SELECT id FROM files WHERE filePath=?");
            containStatement.setString(1, filePath);
            filesWithGivenPath = containStatement.executeQuery();
            isFileInDB = filesWithGivenPath.next();
        } catch (SQLException e) {
            Logger logger = Logger.getLogger("containsLogger");
            logger.warning("Can't execute SELECT statement on table 'files'");
        } finally {
            try {
                if(containStatement != null) {
                    containStatement.close();
                }
                if(filesWithGivenPath != null){
                    filesWithGivenPath.close();
                }
                connection.close();
            } catch(SQLException e){
                Logger logger = Logger.getLogger("contains method logger");
                logger.warning(e.toString());
            }
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
            Logger logger = Logger.getLogger("movePath method logger");
            logger.warning(e.toString());
        } finally {
            try{
                if(moveStatement!=null){
                    moveStatement.close();
                }
                connection.close();
            } catch(SQLException e){
                Logger logger = Logger.getLogger("movePath method logger");
                logger.warning(e.toString());
            }
        }
    }


    @Override
    public Optional<FileBasicInfo> read(String filePath) {
        ResultSet readedData = null;
        getConnection();
        PreparedStatement readStatement = null;
        try {
            readStatement = connection
                    .prepareStatement(
                            "SELECT * FROM files "+
                            "WHERE filePath = ?");
            readStatement.setString(1, filePath);
            readedData = readStatement.executeQuery();

            if(readedData.next()){
                Date readDate = new Date();
                Date creationDate = new Date(readedData
                                .getLong("creationTime"));
                FileBasicInfo readedFile = FileBasicInfo
                        .builder()
                        .name(readedData.getString("name"))
                        .filePath(readedData.getString("filePath"))
                        .extension(readedData.getString("extension"))
                        .url(readedData.getString("url"))
                        .creationTime(creationDate)
                        .lastUsageTime(readDate)
                        .build();

                updateLastUsageTime(filePath, readDate);

                return Optional.ofNullable(readedFile);
            }
        } catch (SQLException e) {
            Logger logger = Logger.getLogger("read method SQL logger");
            logger.warning(e.toString());
        } finally{
            try{
                if(readStatement != null){
                    readStatement.close();
                }
                if(readedData != null){
                    readedData.close();
                }
                connection.close();
            } catch (SQLException e){
                Logger logger = Logger.getLogger("read method logger");
                logger.warning(e.toString());
            }
        }
        return Optional.empty();
    }

    private void updateLastUsageTime(String filePath, Date readDate) {
        PreparedStatement updateTimeStatement = null;
        try {
            updateTimeStatement = connection.prepareStatement(
                    "UPDATE files "+
                            "SET lastUsageTime = ? "+
                            "WHERE filePath = ?");
            updateTimeStatement.setString(1, dateFormat.format(readDate));
            updateTimeStatement.setString(2, filePath);
            updateTimeStatement.execute();
        } catch (SQLException e) {
            Logger logger = Logger.getLogger("updateLastUsageTime method logger");
            logger.warning(e.toString());
        } finally {
            try{
                if(updateTimeStatement != null){
                    updateTimeStatement.close();
                }
            } catch(SQLException e) {
                Logger logger = Logger.getLogger("updateLastUsageTime logger");
                logger.warning(readDate.toString());
            }
        }
    }

    @Override
    public int getNumberOfFiles() {
        int numberOfFiles=-1;
        getConnection();
        Statement countStatement = null;
        ResultSet countResult = null;
        try {
            countStatement = connection.createStatement();
            countResult = countStatement.executeQuery("SELECT count(*) FROM files");
            countResult.next();
            numberOfFiles = countResult.getInt(1);
        } catch (SQLException e) {
            Logger logger = Logger.getLogger("getNumberOfFilesMethodLogger");
            logger.warning(e.toString());
        } finally {
            try{
                if(countStatement != null){
                    countStatement.close();
                }
                if(countResult != null){
                    countResult.close();
                }
                connection.close();
            } catch (SQLException e){
                Logger logger = Logger.getLogger("getNumberOfFiles logger");
                logger.warning(e.toString());
            }
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
            Logger logger = Logger.getLogger("removeAllData method logger");
            logger.warning(e.toString());
        } finally{
            try {
                if (removeAllStatement != null) {
                    removeAllStatement.close();
                }
                connection.close();
            } catch (SQLException e){
                Logger logger = Logger.getLogger("removeAllData method logger");
                logger.warning(e.toString());
            }
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
            Logger logger = Logger.getLogger("initialiseDbLogger");
            logger.warning(e.toString());
        } finally {
            try {
                if (createTableStatement != null) {
                    createTableStatement.close();
                }
            } catch(SQLException e){
                Logger logger = Logger.getLogger("initialise private method logger");
                logger.warning(e.toString());
            }
        }
    }
}
