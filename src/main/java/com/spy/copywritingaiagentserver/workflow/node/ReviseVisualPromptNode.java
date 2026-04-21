package com.spy.copywritingaiagentserver.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.spy.copywritingaiagentserver.ai.agent.VisualPromptAgent;
import com.spy.copywritingaiagentserver.ai.model.ContentPlanResult;
import com.spy.copywritingaiagentserver.ai.model.CopywritingResult;
import com.spy.copywritingaiagentserver.ai.model.RequirementParseResult;
import com.spy.copywritingaiagentserver.ai.model.ReviewResult;
import com.spy.copywritingaiagentserver.ai.model.VisualPromptResult;
import com.spy.copywritingaiagentserver.ai.service.ImageGenerationService;
import com.spy.copywritingaiagentserver.workflow.state.WorkflowStateKeys;
import com.spy.copywritingaiagentserver.workflow.state.WorkflowStateReader;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ReviseVisualPromptNode {

    private final VisualPromptAgent visualPromptAgent;
    private final WorkflowStateReader reader;
    private final ImageGenerationService imageGenerationService;

    public Map<String, Object> execute(OverAllState state) {
        RequirementParseResult requirementParseResult =
                reader.get(state, WorkflowStateKeys.REQUIREMENT_PARSE_RESULT, RequirementParseResult.class);
        ContentPlanResult contentPlanResult =
                reader.get(state, WorkflowStateKeys.CONTENT_PLAN_RESULT, ContentPlanResult.class);
        CopywritingResult copywritingResult =
                reader.get(state, WorkflowStateKeys.COPYWRITING_RESULT, CopywritingResult.class);
        VisualPromptResult visualPromptResult =
                reader.get(state, WorkflowStateKeys.VISUAL_PROMPT_RESULT, VisualPromptResult.class);
        ReviewResult reviewResult =
                reader.get(state, WorkflowStateKeys.REVIEW_RESULT, ReviewResult.class);
        String oldImageUrl =
                reader.getString(state, WorkflowStateKeys.IMAGE_URL);

        String rewrittenImagePrompt = visualPromptResult.getImagePrompt();
        String newImageUrl = oldImageUrl;

        if(reviewResult.isRewriteImagePrompt()) {
            rewrittenImagePrompt = visualPromptAgent.rewriteImagePrompt(
                    requirementParseResult,
                    contentPlanResult,
                    copywritingResult,
                    visualPromptResult,
                    reviewResult.getFeedback()
            );

            VisualPromptResult newVisualPromptResult = new VisualPromptResult();
            BeanUtils.copyProperties(visualPromptResult,newVisualPromptResult);
            newVisualPromptResult.setImagePrompt(rewrittenImagePrompt);
            newImageUrl = imageGenerationService.generate(newVisualPromptResult);
        }







        return Map.of(
                WorkflowStateKeys.IMAGE_URL, newImageUrl,
                WorkflowStateKeys.IMAGE_PROMPT_REWRITTEN, rewrittenImagePrompt
        );
    }
}