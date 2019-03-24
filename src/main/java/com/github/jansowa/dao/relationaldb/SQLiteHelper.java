package com.github.jansowa.dao.relationaldb;

import java.sql.*;
import java.util.logging.Logger;

public final class SQLiteHelper {
    private SQLiteHelper(){
        throw new IllegalStateException("Utility class");
    }

    public static void close(Connection connection){
        try {
            connection.close();
        } catch (SQLException e) {
            log(e);
        }
    }

    public static void close(Statement statement){
        try{
            if(statement != null) {
                statement.close();
            }
        } catch (SQLException e) {
            log(e);
        }
    }

    public static void close(PreparedStatement preparedStatement){
        try{
            if(preparedStatement != null){
                preparedStatement.close();
            }
        } catch (SQLException e) {
            log(e);
        }
    }

    public static void close(ResultSet resultSet){
        if(resultSet != null){
            try {
                resultSet.close();
            } catch (SQLException e) {
                log(e);
            }
        }
    }

    private static void log(Exception e){
        Logger logger = Logger.getLogger("SQLite close logger");
        logger.warning(e.toString());
    }
}