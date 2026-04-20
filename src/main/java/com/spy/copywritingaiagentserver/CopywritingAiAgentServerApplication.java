package com.spy.copywritingaiagentserver;

import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CopywritingAiAgentServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CopywritingAiAgentServerApplication.class, args);
    }

}
