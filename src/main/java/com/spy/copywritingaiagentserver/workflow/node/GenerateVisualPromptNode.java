package com.spy.copywritingaiagentserver.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.spy.copywritingaiagentserver.ai.agent.VisualPromptAgent;
import com.spy.copywritingaiagentserver.ai.model.ContentPlanResult;
import com.spy.copywritingaiagentserver.ai.model.CopywritingResult;
import com.spy.copywritingaiagentserver.ai.model.RequirementParseResult;
import com.spy.copywritingaiagentserver.ai.model.VisualPromptResult;
import com.spy.copywritingaiagentserver.workflow.state.WorkflowStateKeys;
import com.spy.copywritingaiagentserver.workflow.state.WorkflowStateReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GenerateVisualPromptNode {

    private final VisualPromptAgent visualPromptAgent;
    private final WorkflowStateReader reader;

    public Map<String, Object> execute(OverAllState state) {
        RequirementParseResult requirementParseResult =
                reader.get(state, WorkflowStateKeys.REQUIREMENT_PARSE_RESULT, RequirementParseResult.class);

        ContentPlanResult contentPlanResult =
                reader.get(state, WorkflowStateKeys.CONTENT_PLAN_RESULT, ContentPlanResult.class);

        CopywritingResult copywritingResult =
                reader.get(state, WorkflowStateKeys.COPYWRITING_RESULT, CopywritingResult.class);

        VisualPromptResult visualPromptResult =
                visualPromptAgent.execute(requirementParseResult, contentPlanResult, copywritingResult);

        return Map.of(
                WorkflowStateKeys.VISUAL_PROMPT_RESULT, visualPromptResult,
                WorkflowStateKeys.TRACE_LOGS, List.of("generateVisualPrompt finished")
        );
    }
}