package com.spy.copywritingaiagentserver.agent.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
class KnowledgeRetrievalServiceTest {

    @Resource
    private KnowledgeRetrievalService knowledgeRetrievalService;

    @Test
    void searchAsText() {
        String result = knowledgeRetrievalService.searchAsText(
                "小红书 温柔真实 减脂奶昔 内容策划",
                3
        );

        log.info("检索结果:\n{}", result);
    }
}