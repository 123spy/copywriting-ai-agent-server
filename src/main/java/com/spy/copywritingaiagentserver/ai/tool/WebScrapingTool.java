package com.spy.copywritingaiagentserver.ai.tool;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Slf4j
public class WebScrapingTool {

    private static final int DEFAULT_TIMEOUT_MS = 10000;
    private static final int DEFAULT_MAX_CHARS = 4000;

    @Tool(description = "Read a web page and return its title and cleaned main text content.")
    public String scrapeWebPage(
            @ToolParam(description = "URL of the web page to read") String url,
            @ToolParam(description = "Maximum number of characters to keep") Integer maxChars) {
        log.info("<--------调用WebScrapingTool-------->");
        if (url == null || url.trim().isEmpty()) {
            return "URL cannot be empty.";
        }

        if (!isSafeUrl(url)) {
            return "Unsafe or unsupported URL.";
        }

        int finalMaxChars = (maxChars == null || maxChars <= 0)
                ? DEFAULT_MAX_CHARS
                : Math.min(maxChars, 12000);

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(DEFAULT_TIMEOUT_MS)
                    .followRedirects(true)
                    .ignoreContentType(true)
                    .get();

            String title = doc.title() == null ? "" : doc.title().trim();
            String content = extractMainText(doc);

            if (content.isBlank()) {
                return "Page content is empty or unreadable.";
            }

            if (content.length() > finalMaxChars) {
                content = content.substring(0, finalMaxChars) + "...";
            }

            return "Title: " + title + "\n"
                    + "URL: " + url + "\n"
                    + "Content:\n" + content;

        } catch (IOException e) {
            return "Error scraping web page: " + e.getMessage();
        } catch (Exception e) {
            return "Unexpected scrape error: " + e.getMessage();
        }
    }

    private String extractMainText(Document doc) {
        List<String> selectors = List.of(
                "article",
                "main",
                "[role=main]",
                ".article",
                ".post-content",
                ".entry-content",
                ".content"
        );

        for (String selector : selectors) {
            Element element = doc.selectFirst(selector);
            if (element != null) {
                String text = normalizeText(element.text());
                if (!text.isBlank()) {
                    return text;
                }
            }
        }

        return doc.body() == null ? "" : normalizeText(doc.body().text());
    }

    private String normalizeText(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }

    private boolean isSafeUrl(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();

            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                return false;
            }

            if (host == null) {
                return false;
            }

            host = host.toLowerCase();

            return !(host.equals("localhost")
                    || host.equals("127.0.0.1")
                    || host.equals("0.0.0.0")
                    || host.startsWith("10.")
                    || host.startsWith("192.168.")
                    || host.startsWith("172.16.")
                    || host.startsWith("172.17.")
                    || host.startsWith("172.18.")
                    || host.startsWith("172.19.")
                    || host.startsWith("172.20.")
                    || host.startsWith("172.21.")
                    || host.startsWith("172.22.")
                    || host.startsWith("172.23.")
                    || host.startsWith("172.24.")
                    || host.startsWith("172.25.")
                    || host.startsWith("172.26.")
                    || host.startsWith("172.27.")
                    || host.startsWith("172.28.")
                    || host.startsWith("172.29.")
                    || host.startsWith("172.30.")
                    || host.startsWith("172.31."));
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
