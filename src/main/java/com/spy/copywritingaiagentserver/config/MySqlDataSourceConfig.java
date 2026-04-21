package com.spy.copywritingaiagentserver.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class MySqlDataSourceConfig {

    @Bean(name = "mysqlDataSourceProperties")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSourceProperties mysqlDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "mysqlDataSource")
    @Primary
    public DataSource mysqlDataSource() {
        return mysqlDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }
}
