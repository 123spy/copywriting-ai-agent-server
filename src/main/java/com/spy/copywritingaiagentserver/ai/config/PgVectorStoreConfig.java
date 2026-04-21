package com.spy.copywritingaiagentserver.ai.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class PgVectorStoreConfig {

    @Bean(name = "pgVectorStore")
    public VectorStore pgVectorStore(
            @Qualifier("pgVectorJdbcTemplate") JdbcTemplate jdbcTemplate,
            EmbeddingModel embeddingModel
    ) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(1024) // 按你的 DashScope embedding 维度改
                .initializeSchema(true)
                .build();
    }
}