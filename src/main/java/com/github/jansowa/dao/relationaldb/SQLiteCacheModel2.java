package com.github.jansowa.dao.relationaldb;

import com.github.jansowa.dao.CacheModel;
import com.github.jansowa.domain.FileBasicInfo;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;

//one table for each folder
public class SQLiteCacheModel2 implements CacheModel {
    @Getter @Setter private long maxNumberOfFiles;
    private String cacheModelPath;
    private Connection connection;
    private final Properties pooledStatements;

    public SQLiteCacheModel2(long maxNumberOfFiles, String cacheModelPath){
        this.maxNumberOfFiles = maxNumberOfFiles;
        this.cacheModelPath = cacheModelPath;
        pooledStatements = new Properties();
        pooledStatements
                .setProperty("MaxPooledStatements", "10");
        getConnection();
    }

    @Override
    public void put(FileBasicInfo file) {
        String folderPath = getFolderFromPath(file.getFilePath());
        createTableForFolder(folderPath);

        getConnection();
        PreparedStatement putStatement = null;
        Date lastUsageTime = new Date();

        try{
            putStatement = connection.prepareStatement(
                    "INSERT INTO `" + folderPath + "` VALUES(?, ?, ?, ?, ?, ?);"
            );
            putStatement.setString(1, null);
            putStatement.setString(2, file.getName());
            putStatement.setString(3, file.getExtension());
            putStatement.setString(4, file.getUrl());
            putStatement.setLong(5, file.getCreationTime().getTime());
            putStatement.setLong(6, lastUsageTime.getTime());
            putStatement.execute();

            if(getNumberOfFiles()>maxNumberOfFiles){
                removeOldestFile();
            }
        } catch (SQLException e) {
            log(e);
        } finally {
            SQLiteHelper.close(putStatement);
        }
    }

    @Override
    public void remove(String filePath) {
        getConnection();
        PreparedStatement removeStatement = null;
        String folderPath = getFolderFromPath(filePath);

        try{
            removeStatement = connection.prepareStatement(
                    "DELETE FROM `"+ folderPath +"` "+
                    "WHERE name = ? "+
                    "AND extension = ?");
            removeStatement.setString(1, getNameFromPath(filePath));
            removeStatement.setString(2, getExtensionFromPath(filePath));
            removeStatement.execute();

            removeTableIfEmpty(folderPath);
        } catch (SQLException e) {
            log(e);
        } finally {
            SQLiteHelper.close(removeStatement);
        }
    }

    @Override
    public boolean contains(String filePath) {
        getConnection();
        Statement containsStatement = null;
        ResultSet filesWithGivenPath = null;
        boolean isFileInDB = false;
        String folderPath = getFolderFromPath(filePath);
        String name = getNameFromPath(filePath);
        String extension = getExtensionFromPath(filePath);
        String selectQuery = "SELECT id FROM '" + folderPath + "' "+
                "WHERE name = '" + name + "' " +
                "AND extension ";
        if(extension==null){
            selectQuery += "IS NULL";
        } else{
            selectQuery += "= '"+extension+"'";
        }

        try{
            containsStatement = connection.createStatement();
            filesWithGivenPath = containsStatement.executeQuery(selectQuery);
            isFileInDB = filesWithGivenPath.next();
        } catch (SQLException e) {
            System.out.println("There is no file with path: "+filePath);
        } finally{
            SQLiteHelper.close(containsStatement);
            SQLiteHelper.close(filesWithGivenPath);
        }
        return isFileInDB;
    }

    @Override
    public void movePath(String sourcePath, String destinationPath) {
        if(contains(sourcePath)) {
            moveSingleFile(sourcePath, destinationPath);
        }
        else{
            moveWholeFolder(sourcePath, destinationPath);
        }
    }

    public void closeConnection(){
        SQLiteHelper.close(connection);
    }

    private void moveSingleFile(String sourcePath, String destinationPath) {
        getConnection();
        Statement selectFileStatement = null;
        ResultSet fileToMove = null;

        String sourceFolder = getFolderFromPath(sourcePath);
        String sourceName = getNameFromPath(sourcePath);
        String sourceExtension = getExtensionFromPath(sourcePath);

        String destinationName = getNameFromPath(destinationPath);
        String destinationExtension = getExtensionFromPath(destinationPath);


        try{
            selectFileStatement = connection.createStatement();
            String selectQuery = "SELECT creationTime, url " +
                    "FROM '" + sourceFolder + "' " +
                    "WHERE name = '" + sourceName +"' " +
                    "AND extension ";
            if(sourceExtension==null){
                selectQuery+="IS NULL";
            }
            else{
                selectQuery+="= '"+sourceExtension+"'";
            }
            fileToMove = selectFileStatement.executeQuery(selectQuery);

            fileToMove.next();
            Date destinationCreationTime = new Date(fileToMove.getLong("creationTime"));
            String destinationUrl = fileToMove.getString("url");
            FileBasicInfo destinationFile = FileBasicInfo.builder()
                    .name(destinationName)
                    .filePath(destinationPath)
                    .extension(destinationExtension)
                    .creationTime(destinationCreationTime)
                    .lastUsageTime(new Date())
                    .url(destinationUrl)
                    .build();
            put(destinationFile);
            remove(sourcePath);
        } catch (SQLException e) {
            log(e);
        } finally {
            SQLiteHelper.close(fileToMove);
            SQLiteHelper.close(selectFileStatement);
        }
    }

    private void moveWholeFolder(String sourceFolder, String destinationFolder){
        getConnection();
        Statement moveStatement = null;
        Statement findStatement = null;
        Statement deleteStatement = null;
        ResultSet tablesToMove = null;

        try{
            findStatement = connection.createStatement();
            tablesToMove = findStatement.executeQuery(
                "SELECT name "+
                        "FROM sqlite_master " +
                        "WHERE type = 'table' AND " +
                        "name LIKE '" + sourceFolder + "%';");
            long currentTime = new Date().getTime();
            moveStatement = connection.createStatement();
            deleteStatement = connection.createStatement();
            while(tablesToMove.next()){
                String tableToMoveName = tablesToMove.getString(1);
                moveStatement.execute(
                        "UPDATE `" + tableToMoveName + "` " +
                                "SET lastUsageTime = " + currentTime);

                String destinationTable = destinationFolder + tableToMoveName.substring(sourceFolder.length());
                deleteStatement.execute(
                        "ALTER TABLE `" + tableToMoveName + "` " +
                                "RENAME TO `" + destinationTable + "`;");
            }
        } catch (SQLException e) {
            log(e);
        } finally {
            SQLiteHelper.close(findStatement);
            SQLiteHelper.close(moveStatement);
            SQLiteHelper.close(deleteStatement);
            SQLiteHelper.close(tablesToMove);
        }

    }

    @Override
    public Optional<FileBasicInfo> read(String filePath) {
        getConnection();
        ResultSet readedData = null;
        PreparedStatement readStatement = null;
        String folderPath = getFolderFromPath(filePath);
        String name = getNameFromPath(filePath);
        String extension = getExtensionFromPath(filePath);

        try{
            readStatement = connection.prepareStatement(
                    "SELECT url, creationTime, lastUsageTime "
                    + "FROM `" + folderPath + "`" +
                            " WHERE name = ? " +
                            "AND extension = ?;"
            );
            readStatement.setString(1, name);
            readStatement.setString(2, extension);
            readedData = readStatement.executeQuery();

            if(readedData.next()){
                Date lastUsageTime = new Date();
                Date creationTime = new Date(readedData
                        .getLong("creationTime"));
                FileBasicInfo readedFile = FileBasicInfo.builder()
                        .filePath(filePath)
                        .name(name)
                        .extension(extension)
                        .url(readedData.getString("url"))
                        .creationTime(creationTime)
                        .lastUsageTime(lastUsageTime)
                        .build();
                return Optional.ofNullable(readedFile);

            }
        } catch(SQLException e){
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
        ResultSet tablesNames = null;
        Statement selectTablesNamesStatement = null;
        int numberOfFiles = 0;

        try{
            selectTablesNamesStatement = connection.createStatement();
            tablesNames = selectTablesNamesStatement.executeQuery(
                    "SELECT name "+
                            "FROM sqlite_master " +
                            "WHERE type = 'table' AND " +
                            "name NOT LIKE 'sqlite_%';");
            while(tablesNames.next()){
                numberOfFiles += getNumberOfRows(tablesNames.getString(1));
            }
        } catch (SQLException e) {
            log(e);
        } finally {
            SQLiteHelper.close(tablesNames);
            SQLiteHelper.close(selectTablesNamesStatement);
        }
        return numberOfFiles;
    }

    private int getNumberOfRows(String tableName){
        getConnection();
        Statement countStatement = null;
        ResultSet countResult = null;
        int numberOfFiles=-1;

        try {
            countStatement = connection.createStatement();
            countResult = countStatement.executeQuery("SELECT count(*) FROM `" + tableName + "`");
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
        removeFromDevice();
        getConnection();
    }

    @Override
    public long getSizeInBytes() {
        File cacheModel = new File(cacheModelPath);
        return cacheModel.length();
    }

    @Override
    public void removeFromDevice() {
        SQLiteHelper.close(connection);
        File cacheModel = new File(cacheModelPath);
        if(!cacheModel.delete()){
            System.out.println("File "+cacheModelPath+" doesn't exist!");
        }
        File parentDirectory = cacheModel.getParentFile();
        while(parentDirectory!=null && parentDirectory.delete()){
            parentDirectory = parentDirectory.getParentFile();
        }
    }

    private void getConnection(){
        Statement pragmaStatement = null;
        try{
            if(connection == null || connection.isClosed()) {
                FileUtils.forceMkdirParent(new File(cacheModelPath));
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:" + cacheModelPath, pooledStatements);
                pragmaStatement = connection.createStatement();
                pragmaStatement.execute("PRAGMA synchronous = OFF");
            }
        } catch (ClassNotFoundException | SQLException | IOException e) {
            log(e);
        } finally {
            SQLiteHelper.close(pragmaStatement);
        }
    }

    private void createTableForFolder(String folderPath){
        getConnection();
        Statement createTableStatement = null;
        try{
            createTableStatement = connection.createStatement();
            String createQuery =
                    "CREATE TABLE IF NOT EXISTS '" + folderPath + "' " +
                            "(id INTEGER PRIMARY KEY," +
                            "name TEXT," +
                            "extension TEXT," +
                            "url TEXT," +
                            "creationTime INTEGER,"+
                            "lastUsageTime INTEGER);";
            createTableStatement.execute(createQuery);
        } catch (SQLException e){
            log(e);
        } finally {
            SQLiteHelper.close(createTableStatement);
            SQLiteHelper.close(connection);
        }
    }

    private String getFolderFromPath(String path){
        int indexOfLastSlash = path.lastIndexOf('/');
        return path.substring(0, indexOfLastSlash);
    }

    private String getNameFromPath(String path){
        int indexOfLastSlash = path.lastIndexOf('/');
        int indexOfLastDot = path.lastIndexOf('.');
        if(indexOfLastDot==-1){
            return path.substring(indexOfLastSlash+1);
        }
        return path.substring(indexOfLastSlash+1, indexOfLastDot);
    }

    private String getExtensionFromPath(String path){
        int indexOfLastDot = path.lastIndexOf('.');
        if(indexOfLastDot==-1){
            return null;
        }
        return path.substring(indexOfLastDot+1);
    }

    private void removeTableIfEmpty(String folderPath) {
        if(getNumberOfRows(folderPath)>0){
            return;
        }
        getConnection();
        Statement removeTableStatement = null;

        try{
            removeTableStatement = connection.createStatement();
            removeTableStatement.execute(
                    "DROP TABLE `" + folderPath + "`");
        } catch(SQLException e){
            log(e);
        } finally {
            SQLiteHelper.close(removeTableStatement);
            SQLiteHelper.close(connection);
        }
    }

    private void removeOldestFile(){
        getConnection();
        ResultSet tablesNames = null;
        Statement removeOldestFileStatement = null;
        ResultSet oldestInTable = null;

        try{
            removeOldestFileStatement = connection.createStatement();
            tablesNames = removeOldestFileStatement.executeQuery(
                    "SELECT name "+
                            "FROM sqlite_master " +
                            "WHERE type = 'table' AND " +
                            "name NOT LIKE 'sqlite_%';");
            String tableNameWithOldestFile = "";
            long oldestDate = new Date().getTime();
            String tableName;
            while(tablesNames.next()){
                tableName = tablesNames.getString(1);

                oldestInTable = removeOldestFileStatement.executeQuery(
                        "SELECT min(lastUsageTime)" +
                                "FROM `" + tableName + "`");
                oldestInTable.next();
                if(oldestDate>oldestInTable.getLong(1)){
                    oldestDate = oldestInTable.getLong(1);
                    tableNameWithOldestFile = tableName;
                }
            }
            removeOldestFileStatement.execute(
                    "DELETE FROM `" + tableNameWithOldestFile +"` "+
                            "WHERE lastUsageTime = "+oldestDate);
        } catch(SQLException e){
            log(e);
        } finally {
            SQLiteHelper.close(tablesNames);
            SQLiteHelper.close(removeOldestFileStatement);
            SQLiteHelper.close(oldestInTable);
        }
    }

    private void log(Exception e){
        Logger logger = Logger.getLogger("SQLiteCacheModel2 loggger");
        logger.warning(e.toString());
    }
}