package com.spy.copywritingaiagentserver.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.spy.copywritingaiagentserver.ai.agent.ReviewerAgent;
import com.spy.copywritingaiagentserver.ai.model.ContentPlanResult;
import com.spy.copywritingaiagentserver.ai.model.CopywritingResult;
import com.spy.copywritingaiagentserver.ai.model.RequirementParseResult;
import com.spy.copywritingaiagentserver.ai.model.ReviewResult;
import com.spy.copywritingaiagentserver.ai.model.VisualPromptResult;
import com.spy.copywritingaiagentserver.workflow.state.WorkflowStateKeys;
import com.spy.copywritingaiagentserver.workflow.state.WorkflowStateReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ReviewNode {

    /**
     * 只负责，输出一个审核分数
     */
    private final ReviewerAgent reviewerAgent;
    private final WorkflowStateReader reader;

    public Map<String, Object> execute(OverAllState state) {
        Integer current_retry_count = reader.get(state, WorkflowStateKeys.CURRENT_RETRY_COUNT, Integer.class);


        RequirementParseResult requirementParseResult =
                reader.get(state, WorkflowStateKeys.REQUIREMENT_PARSE_RESULT, RequirementParseResult.class);

        ContentPlanResult contentPlanResult =
                reader.get(state, WorkflowStateKeys.CONTENT_PLAN_RESULT, ContentPlanResult.class);

        CopywritingResult copywritingResult =
                reader.get(state, WorkflowStateKeys.COPYWRITING_RESULT, CopywritingResult.class);

        VisualPromptResult visualPromptResult =
                reader.get(state, WorkflowStateKeys.VISUAL_PROMPT_RESULT, VisualPromptResult.class);

        String imageUrl =
                reader.getString(state, WorkflowStateKeys.IMAGE_URL);

        ReviewResult reviewResult = reviewerAgent.execute(
                requirementParseResult,
                contentPlanResult,
                copywritingResult,
                visualPromptResult,
                imageUrl
        );

        return Map.of(
                WorkflowStateKeys.CURRENT_RETRY_COUNT, current_retry_count + 1,
                WorkflowStateKeys.REVIEW_RESULT, reviewResult,
                WorkflowStateKeys.TRACE_LOGS, List.of("review finished")
        );
    }
}