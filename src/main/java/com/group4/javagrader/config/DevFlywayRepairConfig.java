package com.group4.javagrader.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;

@Configuration
@Profile("dev")
public class DevFlywayRepairConfig {

    @Bean
    public FlywayMigrationStrategy devFlywayMigrationStrategy() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
