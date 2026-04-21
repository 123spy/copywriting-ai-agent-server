package com.spy.copywritingaiagentserver.workflow.state;

public final class WorkflowStateKeys {



    private WorkflowStateKeys() {}

    public static final String USER_REQUIREMENT = "userRequirement";
    public static final String REQUIREMENT_PARSE_RESULT = "requirementParseResult";
    public static final String CONTENT_PLAN_RESULT = "contentPlanResult";
    public static final String COPYWRITING_RESULT = "copywritingResult";
    public static final String VISUAL_PROMPT_RESULT = "visualPromptResult";

    public static final String IMAGE_URL = "imageUrl";
    public static final String REVIEW_RESULT = "reviewResult";

    public static final String RAG_REFERENCE = "ragReference";

    public static final String TITLE_REWRITTEN = "titleRewritten";
    public static final String CTA_REWRITTEN = "ctaRewritten";
    public static final String IMAGE_PROMPT_REWRITTEN = "imagePromptRewritten";

    public static final String FINAL_RESULT = "finalResult";

    public static final String TRACE_LOGS = "traceLogs";

    public static final String CURRENT_RETRY_COUNT = "current_retry_count";
}