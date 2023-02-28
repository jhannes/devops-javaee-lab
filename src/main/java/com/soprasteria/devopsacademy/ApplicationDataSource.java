package com.soprasteria.devopsacademy;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class ApplicationDataSource {

    private static final ThreadLocal<DataSource> dataSourceForThread = new ThreadLocal<>();
    private static final ThreadLocal<Connection> connectionForThread = new ThreadLocal<>();

    public static Connection currentConnection() throws SQLException {
        if (connectionForThread.get() == null) {
            connectionForThread.set(dataSourceForThread.get().getConnection());
        }
        return connectionForThread.get();
    }

    public static void close() throws SQLException {
        if (connectionForThread.get() != null) {
            connectionForThread.get().close();
        }
        connectionForThread.remove();
        dataSourceForThread.remove();
    }

    public static SQLCloseable setDataSource(DataSource dataSource) {
        dataSourceForThread.set(dataSource);
        return ApplicationDataSource::close;
    }

    public interface SQLCloseable extends AutoCloseable {
        void close() throws SQLException;
    }
}
