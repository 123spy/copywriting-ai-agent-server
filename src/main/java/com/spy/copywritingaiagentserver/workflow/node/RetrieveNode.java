package com.spy.copywritingaiagentserver.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.spy.copywritingaiagentserver.ai.agent.PlannerAgent;
import com.spy.copywritingaiagentserver.ai.model.ContentPlanResult;
import com.spy.copywritingaiagentserver.ai.model.RequirementParseResult;
import com.spy.copywritingaiagentserver.ai.service.KnowledgeRetrievalService;
import com.spy.copywritingaiagentserver.workflow.state.WorkflowStateKeys;
import com.spy.copywritingaiagentserver.workflow.state.WorkflowStateReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RetrieveNode {

    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final WorkflowStateReader reader;

    public Map<String, Object> execute(OverAllState state) {
        RequirementParseResult requirementParseResult = reader.get(state, WorkflowStateKeys.REQUIREMENT_PARSE_RESULT, RequirementParseResult.class);

        String ragQuery = """
                平台：%s
                主题：%s
                风格：%s
                内容策划
                平台规则
                文案结构
                开头钩子
                CTA
                """.formatted(
                requirementParseResult.getPlatform(),
                requirementParseResult.getTopic(),
                requirementParseResult.getTone()
        );
        String references = knowledgeRetrievalService.searchAsText(ragQuery, 6);

        return Map.of(
                WorkflowStateKeys.RAG_REFERENCE, references,
                WorkflowStateKeys.TRACE_LOGS, List.of("retrieveNode finished")
        );
    }

}
