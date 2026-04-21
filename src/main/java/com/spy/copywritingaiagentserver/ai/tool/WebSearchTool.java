package com.spy.copywritingaiagentserver.ai.tool;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class WebSearchTool {

    private static final String SEARCH_API_URL = "https://www.searchapi.io/api/v1/search";
    private final String apiKey;
    private final String defaultEngine;

    public WebSearchTool(String apiKey) {
        this(apiKey, "baidu");
    }

    public WebSearchTool(String apiKey, String defaultEngine) {
        this.apiKey = apiKey;
        this.defaultEngine = defaultEngine;
    }

    @Tool(description = "Search the web and return top results with title, link, and snippet.")
    public String searchWeb(
            @ToolParam(description = "Search query keyword") String query,
            @ToolParam(description = "Search engine, e.g. baidu/google/bing") String engine,
            @ToolParam(description = "Maximum number of results to return") Integer topK) {
        log.info("<--------调用WebSearchTool-------->");
        if (query == null || query.trim().isEmpty()) {
            return "Search query cannot be empty.";
        }

        if (apiKey == null || apiKey.isBlank()) {
            return "Search API key is missing.";
        }

        String finalEngine = (engine == null || engine.isBlank()) ? defaultEngine : engine.trim();
        int finalTopK = (topK == null || topK <= 0) ? 5 : Math.min(topK, 10);

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("q", query.trim());
        paramMap.put("api_key", apiKey);
        paramMap.put("engine", finalEngine);

        try {
            String response = HttpUtil.get(SEARCH_API_URL, paramMap);
            JSONObject jsonObject = JSONUtil.parseObj(response);
            JSONArray organicResults = jsonObject.getJSONArray("organic_results");

            if (organicResults == null || organicResults.isEmpty()) {
                return "No search results found.";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Query: ").append(query.trim()).append("\n");
            sb.append("Engine: ").append(finalEngine).append("\n\n");

            int limit = Math.min(finalTopK, organicResults.size());
            for (int i = 0; i < limit; i++) {
                JSONObject item = organicResults.getJSONObject(i);
                sb.append(i + 1).append(". ")
                        .append("Title: ").append(item.getStr("title", "N/A")).append("\n")
                        .append("Link: ").append(item.getStr("link", "N/A")).append("\n")
                        .append("Snippet: ").append(item.getStr("snippet", "")).append("\n\n");
            }

            return sb.toString();
        } catch (Exception e) {
            return "Error searching web: " + e.getMessage();
        }
    }
}
