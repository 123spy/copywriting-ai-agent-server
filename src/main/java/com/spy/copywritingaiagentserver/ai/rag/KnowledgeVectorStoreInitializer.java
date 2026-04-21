package com.spy.copywritingaiagentserver.ai.rag;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
public class KnowledgeVectorStoreInitializer {

    @Resource
    private KnowledgeDocumentLoader knowledgeDocumentLoader;

    @Resource
    private VectorStore pgVectorStore;

    /**
     * DashScope embedding 一次最多 10 条
     */
    private static final int BATCH_SIZE = 10;

    @PostConstruct
    public void init() {
        List<Document> rawDocs = knowledgeDocumentLoader.loadMarkdowns();
        log.info("原始文档数量: {}", rawDocs.size());

        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> splitDocs = splitter.apply(rawDocs);
        log.info("切块后文档数量: {}", splitDocs.size());

        for (int i = 0; i < splitDocs.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, splitDocs.size());
            List<Document> batch = splitDocs.subList(i, end);

            log.info("开始写入第 {} 批，范围: [{} , {})，数量: {}",
                    i / BATCH_SIZE + 1, i, end, batch.size());

            pgVectorStore.add(batch);
        }

        log.info("知识库已全部写入 PgVectorStore");
    }
}