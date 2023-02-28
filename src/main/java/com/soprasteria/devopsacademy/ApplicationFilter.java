package com.soprasteria.devopsacademy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;

public class ApplicationFilter extends HttpFilter {

    private final DataSource dataSource;

    public ApplicationFilter(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        try (var ignored = ApplicationDataSource.setDataSource(dataSource)) {
            chain.doFilter(req, res);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
