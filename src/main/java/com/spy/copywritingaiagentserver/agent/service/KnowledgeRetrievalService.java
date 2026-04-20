package com.spy.copywritingaiagentserver.agent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KnowledgeRetrievalService {

    private final VectorStore vectorStore;

    public KnowledgeRetrievalService(VectorStore pgVectorStore) {
        this.vectorStore = pgVectorStore;
    }

    public List<Document> search(String query, int topK) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(0.4)
                .build();

        List<Document> results = vectorStore.similaritySearch(request);
        log.info("检索 query={}, topK={}, 命中数量={}", query, topK, results == null ? 0 : results.size());
        return results;
    }

    public String searchAsText(String query, int topK) {
        List<Document> docs = search(query, topK);
        if (docs == null || docs.isEmpty()) {
            return "";
        }

        return docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));
    }
}