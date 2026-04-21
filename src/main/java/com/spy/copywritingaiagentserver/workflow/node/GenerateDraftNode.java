package com.spy.copywritingaiagentserver.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.spy.copywritingaiagentserver.ai.agent.CopywriterAgent;
import com.spy.copywritingaiagentserver.ai.agent.PlannerAgent;
import com.spy.copywritingaiagentserver.ai.model.ContentPlanResult;
import com.spy.copywritingaiagentserver.ai.model.CopywritingResult;
import com.spy.copywritingaiagentserver.ai.model.RequirementParseResult;
import com.spy.copywritingaiagentserver.workflow.state.WorkflowStateKeys;
import com.spy.copywritingaiagentserver.workflow.state.WorkflowStateReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GenerateDraftNode {

    private final CopywriterAgent copywriterAgent;
    private final WorkflowStateReader reader;

    public Map<String, Object> execute(OverAllState state) {
        RequirementParseResult requirementParseResult =
                reader.get(state, WorkflowStateKeys.REQUIREMENT_PARSE_RESULT, RequirementParseResult.class);

        ContentPlanResult contentPlanResult =
                reader.get(state, WorkflowStateKeys.CONTENT_PLAN_RESULT, ContentPlanResult.class);

        String reference = reader.getString(state, WorkflowStateKeys.RAG_REFERENCE);

        CopywritingResult copywritingResult = copywriterAgent.execute(requirementParseResult, contentPlanResult, reference);


        return Map.of(
                WorkflowStateKeys.COPYWRITING_RESULT, copywritingResult,
                WorkflowStateKeys.TRACE_LOGS, List.of("generateDraft finished")
        );
    }

}
