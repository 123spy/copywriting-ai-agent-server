package com.spy.copywritingaiagentserver.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.spy.copywritingaiagentserver.ai.agent.RequirementAgent;
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
public class ParseRequirementNode {

    private final RequirementAgent requirementAgent;
    private final WorkflowStateReader reader;

    public Map<String, Object> execute(OverAllState state) {
        UserRequirement userRequirement = reader.get(state, WorkflowStateKeys.USER_REQUIREMENT, UserRequirement.class);

        RequirementParseResult requirementParseResult = requirementAgent.execute(userRequirement);

        return Map.of(
                WorkflowStateKeys.REQUIREMENT_PARSE_RESULT, requirementParseResult,
                WorkflowStateKeys.TRACE_LOGS, List.of("parseRequirement finished")
        );
    }

}
