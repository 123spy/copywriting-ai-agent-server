package com.spy.copywritingaiagentserver.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.spy.copywritingaiagentserver.ai.agent.CopywriterAgent;
import com.spy.copywritingaiagentserver.ai.model.ContentPlanResult;
import com.spy.copywritingaiagentserver.ai.model.CopywritingResult;
import com.spy.copywritingaiagentserver.ai.model.RequirementParseResult;
import com.spy.copywritingaiagentserver.ai.model.ReviewResult;
import com.spy.copywritingaiagentserver.workflow.state.WorkflowStateKeys;
import com.spy.copywritingaiagentserver.workflow.state.WorkflowStateReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ReviseCTANode {

    private final CopywriterAgent copywriterAgent;
    private final WorkflowStateReader reader;

    public Map<String, Object> execute(OverAllState state) {
        RequirementParseResult requirementParseResult =
                reader.get(state, WorkflowStateKeys.REQUIREMENT_PARSE_RESULT, RequirementParseResult.class);
        ContentPlanResult contentPlanResult =
                reader.get(state, WorkflowStateKeys.CONTENT_PLAN_RESULT, ContentPlanResult.class);
        CopywritingResult copywritingResult =
                reader.get(state, WorkflowStateKeys.COPYWRITING_RESULT, CopywritingResult.class);
        ReviewResult reviewResult =
                reader.get(state, WorkflowStateKeys.REVIEW_RESULT, ReviewResult.class);

        String newCta = copywritingResult.getCta();
        if(reviewResult.isRewriteCta()) {
            newCta = copywriterAgent.rewriteCta(
                    requirementParseResult,
                    contentPlanResult,
                    copywritingResult,
                    reviewResult.getFeedback()
            );
        }

        return Map.of(WorkflowStateKeys.CTA_REWRITTEN, newCta);
    }
}