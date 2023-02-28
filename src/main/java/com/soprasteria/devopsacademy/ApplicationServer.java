package com.soprasteria.devopsacademy;

import com.soprasteria.infrastructure.DefaultServlet;
import com.soprasteria.infrastructure.WebjarsServlet;
import jakarta.servlet.DispatcherType;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.flywaydb.core.Flyway;
import org.glassfish.jersey.servlet.ServletContainer;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.EnumSet;
import java.util.Optional;

public class ApplicationServer {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationServer.class);
    private final DataSource dataSource;

    private final Server server;

    public ApplicationServer(DataSource dataSource, int port) {
        this.dataSource = dataSource;
        this.server = new Server(port);
        server.setHandler(createWebApp());
        server.setRequestLog(new CustomRequestLog());
    }

    private ServletContextHandler createWebApp() {
        var appContext = new ServletContextHandler(null, "/");
        appContext.addServlet(new ServletHolder(new ServletContainer(new ApplicationApiConfig())), "/api/*");
        appContext.addServlet(new ServletHolder(new WebjarsServlet("swagger-ui")), "/api-doc/swagger-ui/*");
        appContext.addServlet(new ServletHolder(new DefaultServlet("/web")), "/*");
        appContext.addFilter(new FilterHolder(new ApplicationFilter(dataSource)), "/*", EnumSet.of(DispatcherType.REQUEST));
        return appContext;
    }

    private void start() throws Exception {
        logger.info("Starting...");
        server.start();
        logger.info("Started on port {}", ((NetworkConnector) server.getConnectors()[0]).getLocalPort());
    }

    public static void main(String[] args) throws Exception {
        var dataSource = new PGSimpleDataSource();
        dataSource.setUrl(Optional.ofNullable(System.getenv("JDBC_URL"))
                .orElse("jdbc:postgresql://localhost:5432/postgres"));
        dataSource.setUser(Optional.ofNullable(System.getenv("JDBC_USER"))
                .orElse("postgres"));
        dataSource.setPassword(System.getenv("JDBC_PASSWORD"));
        Flyway.configure().dataSource(dataSource).load().migrate();
        new ApplicationServer(dataSource, 8080).start();
    }
}
