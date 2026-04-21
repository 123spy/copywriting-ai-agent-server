package com.spy.copywritingaiagentserver.workflow.graph;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.spy.copywritingaiagentserver.ai.model.ReviewResult;
import com.spy.copywritingaiagentserver.workflow.node.*;
import com.spy.copywritingaiagentserver.workflow.state.WorkflowStateKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 主流程工厂
 */
@Configuration
@RequiredArgsConstructor
public class CopywritingGraphFactory {

    /**
     * 下属全部节点
     */
    private final ParseRequirementNode parseRequirementNode;
    private final PlanNode planNode;
    private final RetrieveNode retrieveNode;
    private final GenerateDraftNode generateDraftNode;
    private final GenerateVisualPromptNode generateVisualPromptNode;
    private final GenerateImageNode generateImageNode;
    private final ReviewNode reviewNode;
    private final FinalizeNode finalizeNode;

    /**
     * 创建线程
     * @return
     */
    @Bean
    public ExecutorService workflowExecutor() {
        return Executors.newFixedThreadPool(4);
    }

    /**
     * 将节点，绑定到执行器上
     * @param workflowExecutor
     * @return
     */
    @Bean
    public RunnableConfig mainGraphRunnableConfig(
            @Qualifier("workflowExecutor") ExecutorService workflowExecutor,
            @Qualifier("revisionExecutor") ExecutorService revisionExecutor
    ) {
        return RunnableConfig.builder()
                .addParallelNodeExecutor("planNode", workflowExecutor)
                .addParallelNodeExecutor("retrieveNode", workflowExecutor)
                .addParallelNodeExecutor("reviseTitle", revisionExecutor)
                .addParallelNodeExecutor("reviseCTA", revisionExecutor)
                .addParallelNodeExecutor("reviseVisualPrompt", revisionExecutor)
                .build();
    }

    // 子图
    private final ReviseDispatchNode reviseDispatchNode;
    private final ReviseTitleNode reviseTitleNode;
    private final ReviseCTANode reviseCTANode;
    private final ReviseVisualPromptNode reviseVisualPromptNode;
    private final MergeRevisionNode mergeRevisionNode;

    @Bean
    public ExecutorService revisionExecutor() {
        return Executors.newFixedThreadPool(3);
    }

    @Bean
    public RunnableConfig revisionRunnableConfig(@Qualifier("revisionExecutor") ExecutorService revisionExecutor) {
        return RunnableConfig.builder()
                .addParallelNodeExecutor("reviseTitle", revisionExecutor)
                .addParallelNodeExecutor("reviseCTA", revisionExecutor)
                .addParallelNodeExecutor("reviseVisualPrompt", revisionExecutor)
                .build();
    }

    @Bean
    public CompiledGraph copywritingGraph() throws GraphStateException {
        return new StateGraph(this::keyStrategies)
                .addNode("parseRequirement", AsyncNodeAction.node_async(parseRequirementNode::execute))
                .addNode("planNode", AsyncNodeAction.node_async(planNode::execute))
                .addNode("retrieveNode", AsyncNodeAction.node_async(retrieveNode::execute))
                .addNode("generateDraft", AsyncNodeAction.node_async(generateDraftNode::execute))
                .addNode("generateVisualPrompt", AsyncNodeAction.node_async(generateVisualPromptNode::execute))
                .addNode("generateImage", AsyncNodeAction.node_async(generateImageNode::execute))
                .addNode("review", AsyncNodeAction.node_async(reviewNode::execute))
                .addNode("finalize", AsyncNodeAction.node_async(finalizeNode::execute))

                .addNode("reviseDispatch", AsyncNodeAction.node_async(reviseDispatchNode::execute))
                .addNode("reviseTitle", AsyncNodeAction.node_async(reviseTitleNode::execute))
                .addNode("reviseCTA", AsyncNodeAction.node_async(reviseCTANode::execute))
                .addNode("reviseVisualPrompt", AsyncNodeAction.node_async(reviseVisualPromptNode::execute))
                .addNode("mergeRevision", AsyncNodeAction.node_async(mergeRevisionNode::execute))

                .addEdge(StateGraph.START, "parseRequirement")
                .addEdge("parseRequirement", "planNode")
                .addEdge("parseRequirement", "retrieveNode")
                .addEdge("planNode", "generateDraft")
                .addEdge("retrieveNode", "generateDraft")
                .addEdge("generateDraft", "generateVisualPrompt")
                .addEdge("generateVisualPrompt", "generateImage")
                .addEdge("generateImage", "review")
                .addConditionalEdges(
                        "review",
                        AsyncEdgeAction.edge_async(reviewRoute()),
                        Map.of(
                                "pass", "finalize",
                                "revise", "reviseDispatch"
                        )
                )
                .addEdge("reviseDispatch", "reviseTitle")
                .addEdge("reviseDispatch", "reviseCTA")
                .addEdge("reviseDispatch", "reviseVisualPrompt")

                .addEdge("reviseTitle", "mergeRevision")
                .addEdge("reviseCTA", "mergeRevision")
                .addEdge("reviseVisualPrompt", "mergeRevision")

                .addEdge("mergeRevision", "review")


                .addEdge("finalize", StateGraph.END)
                .compile();

    }

    private Map<String, KeyStrategy> keyStrategies() {
        Map<String, KeyStrategy> strategies = new HashMap<>();

        strategies.put(WorkflowStateKeys.TRACE_LOGS, new AppendStrategy());

        strategies.put(WorkflowStateKeys.REQUIREMENT_PARSE_RESULT, new ReplaceStrategy());
        strategies.put(WorkflowStateKeys.CONTENT_PLAN_RESULT, new ReplaceStrategy());
        strategies.put(WorkflowStateKeys.RAG_REFERENCE, new ReplaceStrategy());
        strategies.put(WorkflowStateKeys.COPYWRITING_RESULT, new ReplaceStrategy());
        strategies.put(WorkflowStateKeys.VISUAL_PROMPT_RESULT, new ReplaceStrategy());
        strategies.put(WorkflowStateKeys.IMAGE_URL, new ReplaceStrategy());
        strategies.put(WorkflowStateKeys.REVIEW_RESULT, new ReplaceStrategy());
        strategies.put(WorkflowStateKeys.FINAL_RESULT, new ReplaceStrategy());

        strategies.put(WorkflowStateKeys.TITLE_REWRITTEN, new ReplaceStrategy());
        strategies.put(WorkflowStateKeys.CTA_REWRITTEN, new ReplaceStrategy());
        strategies.put(WorkflowStateKeys.IMAGE_PROMPT_REWRITTEN, new ReplaceStrategy());
        // 当前步数，设置为覆盖
        strategies.put(WorkflowStateKeys.CURRENT_RETRY_COUNT, new ReplaceStrategy());

        return strategies;
    }

    private EdgeAction reviewRoute() {
        return state -> {
            Object reviewResultObj = state.value(WorkflowStateKeys.REVIEW_RESULT).orElse(null);
            if (reviewResultObj == null) {
                return "revise";
            }

            Integer current_retry_count = (Integer) state.value(WorkflowStateKeys.CURRENT_RETRY_COUNT).orElse(null);
            if(current_retry_count == null) {
                return "pass";
            }

            var reviewResult = (ReviewResult) reviewResultObj;
            if(Boolean.TRUE.equals(reviewResult.getPass())) {
                return "pass";
            }else {
                if(current_retry_count <= 3) return "revise";
                else return "pass";
            }
//            return Boolean.TRUE.equals(reviewResult.getPass()) ? "pass" : "revise";
        };
    }
}
