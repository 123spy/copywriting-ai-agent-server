package com.spy.copywritingaiagentserver.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.spy.copywritingaiagentserver.ai.model.ContentPlanResult;
import com.spy.copywritingaiagentserver.ai.model.CopywritingResult;
import com.spy.copywritingaiagentserver.ai.model.RequirementParseResult;
import com.spy.copywritingaiagentserver.ai.model.ReviewResult;
import com.spy.copywritingaiagentserver.ai.model.VisualPromptResult;
import com.spy.copywritingaiagentserver.workflow.state.WorkflowStateKeys;
import com.spy.copywritingaiagentserver.workflow.state.WorkflowStateReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FinalizeNode {

    private final WorkflowStateReader reader;

    public Map<String, Object> execute(OverAllState state) {
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

        ReviewResult reviewResult =
                reader.get(state, WorkflowStateKeys.REVIEW_RESULT, ReviewResult.class);

        Map<String, Object> finalResult = new HashMap<>();
        finalResult.put("requirementParseResult", requirementParseResult);
        finalResult.put("contentPlanResult", contentPlanResult);
        finalResult.put("copywritingResult", copywritingResult);
        finalResult.put("visualPromptResult", visualPromptResult);
        finalResult.put("imageUrl", imageUrl);
        finalResult.put("reviewResult", reviewResult);

        return Map.of(
                WorkflowStateKeys.FINAL_RESULT, finalResult,
                WorkflowStateKeys.TRACE_LOGS, List.of("finalize finished")
        );
    }
}