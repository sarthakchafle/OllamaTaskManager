package com.agentic.taskmanager;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class DatabaseLogger {
    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void logDatabaseInfo() throws Exception {
        System.out.println("JDBC URL: " + dataSource.getConnection().getMetaData().getURL());
        System.out.println("User: " + dataSource.getConnection().getMetaData().getUserName());
    }
}