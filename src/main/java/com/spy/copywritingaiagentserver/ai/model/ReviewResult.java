package com.spy.copywritingaiagentserver.ai.model;

import lombok.Data;

@Data
public class ReviewResult {

    private Boolean pass;

    private double titleScore;
    private double bodyScore;
    private double imagePromptScore;
    private double overallScore;

    private String feedback;

    private boolean rewriteTitle;
    private boolean rewriteBody;
    private boolean rewriteCta;
    private boolean rewriteImagePrompt;

    private String titleFeedback;
    private String bodyFeedback;
    private String ctaFeedback;
    private String imagePromptFeedback;
}
