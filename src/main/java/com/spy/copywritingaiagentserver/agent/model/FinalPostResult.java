package com.spy.copywritingaiagentserver.agent.model;

import lombok.Data;

/**
 * 最终结果类
 */
@Data
public class FinalPostResult {
    private RequirementParseResult requirementParseResult;
    private ContentPlanResult contentPlanResult;
    private CopywritingResult copywritingResult;
    private VisualPromptResult visualPromptResult;
    private String imageUrl;
    private ReviewResult reviewResult;
}