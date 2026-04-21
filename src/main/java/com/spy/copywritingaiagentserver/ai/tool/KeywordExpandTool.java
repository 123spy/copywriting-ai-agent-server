package com.spy.copywritingaiagentserver.ai.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.*;

@Slf4j
public class KeywordExpandTool {

    private static final int DEFAULT_LIMIT = 12;

    private static final List<String> SUFFIX_PATTERNS = List.of(
            "官网",
            "产品",
            "软件",
            "工具",
            "平台",
            "推荐",
            "哪个好",
            "评测",
            "对比",
            "价格",
            "收费",
            "使用教程",
            "入门",
            "最佳实践"
    );

    private static final Map<String, List<String>> SYNONYM_MAP = new HashMap<>();
    private static final Map<String, List<String>> ENGLISH_MAP = new HashMap<>();

    static {
        SYNONYM_MAP.put("AI", List.of("人工智能", "智能"));
        SYNONYM_MAP.put("写作", List.of("文案", "内容创作", "文本生成"));
        SYNONYM_MAP.put("工具", List.of("软件", "平台", "助手", "应用"));
        SYNONYM_MAP.put("营销", List.of("推广", "增长", "获客"));
        SYNONYM_MAP.put("电商", List.of("商品运营", "店铺运营"));
        SYNONYM_MAP.put("小红书", List.of("种草平台"));
        SYNONYM_MAP.put("公众号", List.of("微信公众平台", "公号"));

        ENGLISH_MAP.put("AI 写作工具", List.of(
                "AI writing tool",
                "AI copywriting tool",
                "AI writing assistant"
        ));
        ENGLISH_MAP.put("AI文案生成", List.of(
                "AI copywriting",
                "AI content generator"
        ));
        ENGLISH_MAP.put("营销文案", List.of(
                "marketing copy",
                "ad copywriting"
        ));
    }

    @Tool(description = "Expand a seed keyword into multiple related search queries for better web search recall.")
    public String expandKeywords(
            @ToolParam(description = "Seed keyword, for example: AI 写作工具") String keyword,
            @ToolParam(description = "Maximum number of expanded queries to return") Integer limit) {

        log.info("<--------调用KeywordExpandTool-------->");
        if (keyword == null || keyword.trim().isEmpty()) {
            return "Keyword cannot be empty.";
        }

        String seed = normalize(keyword);
        int finalLimit = (limit == null || limit <= 0) ? DEFAULT_LIMIT : Math.min(limit, 30);

        LinkedHashSet<String> expanded = new LinkedHashSet<>();
        expanded.add(seed);

        // 1. 原词 + 常见搜索后缀
        for (String suffix : SUFFIX_PATTERNS) {
            expanded.add(seed + " " + suffix);
        }

        // 2. 同义词替换
        expanded.addAll(generateSynonymVariants(seed));

        // 3. 英文扩展
        expanded.addAll(generateEnglishVariants(seed));

        // 4. 常见搜索意图补充
        expanded.add(seed + " 官网");
        expanded.add(seed + " 推荐");
        expanded.add(seed + " 对比");
        expanded.add(seed + " 价格");
        expanded.add(seed + " 教程");
        expanded.add(seed + " 使用场景");

        // 去掉太短、重复和空字符串
        List<String> result = expanded.stream()
                .map(this::normalize)
                .filter(s -> !s.isBlank())
                .filter(s -> s.length() >= 2)
                .limit(finalLimit)
                .toList();

        if (result.isEmpty()) {
            return "No expanded keywords generated.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Expanded keywords:\n");
        for (int i = 0; i < result.size(); i++) {
            sb.append(i + 1).append(". ").append(result.get(i)).append("\n");
        }

        return sb.toString();
    }

    private List<String> generateSynonymVariants(String seed) {
        LinkedHashSet<String> variants = new LinkedHashSet<>();

        for (Map.Entry<String, List<String>> entry : SYNONYM_MAP.entrySet()) {
            String key = entry.getKey();
            if (seed.contains(key)) {
                for (String synonym : entry.getValue()) {
                    variants.add(seed.replace(key, synonym));
                }
            }
        }

        // 双词拼装，适合中文搜索
        if (seed.contains("AI") && seed.contains("写作")) {
            variants.add("AI 文案生成");
            variants.add("AI 写作软件");
            variants.add("AI 内容创作工具");
            variants.add("营销文案 AI");
        }

        return new ArrayList<>(variants);
    }

    private List<String> generateEnglishVariants(String seed) {
        LinkedHashSet<String> variants = new LinkedHashSet<>();

        for (Map.Entry<String, List<String>> entry : ENGLISH_MAP.entrySet()) {
            if (seed.contains(entry.getKey()) || entry.getKey().contains(seed)) {
                variants.addAll(entry.getValue());
            }
        }

        if (seed.contains("AI") && seed.contains("写作")) {
            variants.add("AI writing tool");
            variants.add("AI copywriting");
            variants.add("AI content generator");
            variants.add("AI writing assistant");
        }

        return new ArrayList<>(variants);
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\s+", " ").trim();
    }
}