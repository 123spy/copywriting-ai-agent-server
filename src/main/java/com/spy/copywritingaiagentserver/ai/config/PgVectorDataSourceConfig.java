package com.spy.copywritingaiagentserver.ai.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class PgVectorDataSourceConfig {

    @Bean(name = "pgVectorDataSourceProperties")
    @ConfigurationProperties(prefix = "pgvector.datasource")
    public DataSourceProperties pgVectorDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "pgVectorDataSource")
    public DataSource pgVectorDataSource() {
        return pgVectorDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }
}