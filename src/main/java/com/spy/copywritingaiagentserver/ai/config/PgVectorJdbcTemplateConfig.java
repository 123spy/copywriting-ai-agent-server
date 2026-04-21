package com.spy.copywritingaiagentserver.ai.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class PgVectorJdbcTemplateConfig {

    @Bean(name = "pgVectorJdbcTemplate")
    public JdbcTemplate pgVectorJdbcTemplate(
            @Qualifier("pgVectorDataSource") DataSource dataSource
    ) {
        return new JdbcTemplate(dataSource);
    }
}