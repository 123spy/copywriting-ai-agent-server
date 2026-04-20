package com.spy.copywritingaiagentserver.agent.model;

import lombok.Data;

@Data
public class ReviewResult {

    private boolean pass;

    private double titleScore;
    private double bodyScore;
    private double imagePromptScore;
    private double overallScore;

    private String feedback;

    /**
     * 是否需要重写标题
     */
    private boolean rewriteTitle;

    /**
     * 是否需要重写 CTA
     */
    private boolean rewriteCta;

    /**
     * 是否需要重写图片提示词
     */
    private boolean rewriteImagePrompt;

    /**
     * 标题重写意见
     */
    private String titleFeedback;

    /**
     * CTA 重写意见
     */
    private String ctaFeedback;

    /**
     * 图片提示词重写意见
     */
    private String imagePromptFeedback;
}