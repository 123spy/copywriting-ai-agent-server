package com.spy.copywritingaiagentserver;

import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@EnableScheduling
public class CopywritingAiAgentServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CopywritingAiAgentServerApplication.class, args);
    }

}
