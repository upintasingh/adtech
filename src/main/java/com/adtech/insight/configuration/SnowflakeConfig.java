package com.adtech.insight.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class SnowflakeConfig {
    @Bean(name = "snowflakeDataSource")
    @ConfigurationProperties(prefix = "snowflake")
    public DataSource snowflakeDataSource() {
        return DataSourceBuilder.create().build();
    }
}
