package com.spy.copywritingaiagentserver.agent.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class KnowledgeDocumentLoader {

    public List<Document> loadMarkdowns() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:/documents/**/*.md");

            List<Document> allDocs = new ArrayList<>();

            for (Resource resource : resources) {
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withAdditionalMetadata("source", resource.getFilename())
                        .build();

                MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
                List<Document> docs = reader.get();

                // 给每个文档补一点元数据，后面检索/过滤会很有用
                for (Document doc : docs) {
                    doc.getMetadata().put("fileName", resource.getFilename());
                    doc.getMetadata().put("resourcePath", resource.getDescription());
                }

                allDocs.addAll(docs);
            }

            return allDocs;
        } catch (IOException e) {
            throw new RuntimeException("加载 markdown 知识库失败", e);
        }
    }
}