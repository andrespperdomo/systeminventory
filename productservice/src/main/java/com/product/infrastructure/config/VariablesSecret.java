package com.product.infrastructure.config;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class VariablesSecret {
    
    @PostConstruct 
    public void checkSecrets() throws Exception {
        String databaseUrl = System.getenv("DATABASE_URL");
        String databaseUsername = System.getenv("DATABASE_USERNAME");
        String databasePassword = System.getenv("DATABASE_PASSWORD");

        System.out.println("DATABASE_URL: " + databaseUrl);

        if (databaseUrl == null || databaseUsername == null || databasePassword == null) {
            throw new RuntimeException("Database environment variables are not set.");
        }
    }
}
