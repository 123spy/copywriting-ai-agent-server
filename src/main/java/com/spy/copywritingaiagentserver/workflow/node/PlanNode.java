package com.spy.copywritingaiagentserver.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.spy.copywritingaiagentserver.ai.agent.PlannerAgent;
import com.spy.copywritingaiagentserver.ai.agent.RequirementAgent;
import com.spy.copywritingaiagentserver.ai.model.ContentPlanResult;
import com.spy.copywritingaiagentserver.ai.model.RequirementParseResult;
import com.spy.copywritingaiagentserver.ai.model.UserRequirement;
import com.spy.copywritingaiagentserver.workflow.state.WorkflowStateKeys;
import com.spy.copywritingaiagentserver.workflow.state.WorkflowStateReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PlanNode {

    private final PlannerAgent plannerAgent;
    private final WorkflowStateReader reader;

    public Map<String, Object> execute(OverAllState state) {
        RequirementParseResult requirementParseResult1 = reader.get(state, WorkflowStateKeys.REQUIREMENT_PARSE_RESULT, RequirementParseResult.class);


        ContentPlanResult contentPlanResult = plannerAgent.execute(requirementParseResult1);

        return Map.of(
                WorkflowStateKeys.CONTENT_PLAN_RESULT, contentPlanResult,
                WorkflowStateKeys.TRACE_LOGS, List.of("planNode finished")
        );
    }

}
