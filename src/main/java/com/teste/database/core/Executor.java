package com.teste.database.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Executor {

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driver;

    @FunctionalInterface
    public interface PersistFunctional<T> {
        T execute(Connection connection) throws SQLException;
    }

    public <T> T execute(PersistFunctional<T> persistFunctional) throws SQLException, ClassNotFoundException {
        Class.forName(driver);
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            return persistFunctional.execute(connection);
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }
}
