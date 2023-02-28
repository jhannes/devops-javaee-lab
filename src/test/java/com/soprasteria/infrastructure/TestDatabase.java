package com.soprasteria.infrastructure;

import com.soprasteria.devopsacademy.ApplicationDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.sql.SQLException;
import java.util.Optional;

@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(TestDatabase.Extension.class)
public @interface TestDatabase {
    class Extension implements BeforeEachCallback, AfterEachCallback {

        private static final DataSource dataSource = createDataSource();

        private static DataSource createDataSource() {
            var url = Optional.ofNullable(System.getenv("TEST_JDBC_URL"))
                    .orElse("jdbc:postgresql://localhost:5432/postgres");
            var masterDataSource = new PGSimpleDataSource();
            masterDataSource.setUrl(url);
            masterDataSource.setUser("postgres");
            masterDataSource.setPassword(null);

            try (var connection = masterDataSource.getConnection()) {
                connection.createStatement().execute("SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'application_test'");
                connection.createStatement().execute("drop database if exists application_test");
                connection.createStatement().execute("create database application_test");
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }

            var testDataSource = new PGSimpleDataSource();
            testDataSource.setUrl(url.replaceAll("/postgres$", "/application_test"));
            testDataSource.setUser(masterDataSource.getUser());
            testDataSource.setPassword(masterDataSource.getPassword());

            Flyway.configure().dataSource(testDataSource).load().migrate();

            return testDataSource;
        }

        @Override
        public void beforeEach(ExtensionContext context) {
            ApplicationDataSource.setDataSource(dataSource);
        }

        @Override
        public void afterEach(ExtensionContext context) throws Exception {
            ApplicationDataSource.close();
        }
    }
}
