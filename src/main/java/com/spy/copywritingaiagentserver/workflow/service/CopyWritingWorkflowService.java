package com.spy.copywritingaiagentserver.workflow.service;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.spy.copywritingaiagentserver.workflow.state.WorkflowStateKeys;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CopyWritingWorkflowService {

    private final CompiledGraph copywritingGraph;

    private final RunnableConfig runnableConfig;

    public CopyWritingWorkflowService(
            @Qualifier("copywritingGraph") CompiledGraph copywritingGraph,
            @Qualifier("mainGraphRunnableConfig") RunnableConfig runnableConfig
    ) {
        this.copywritingGraph = copywritingGraph;
        this.runnableConfig = runnableConfig;
    }

    public Map<String, Object> run(Map<String, Object> inputState) {

        inputState.put(WorkflowStateKeys.CURRENT_RETRY_COUNT, 0);

        copywritingGraph.invoke(inputState, runnableConfig);
        Map<String, Object> result = (Map<String, Object>) copywritingGraph
                .getState(runnableConfig)
                .state()
                .data()
                .get(WorkflowStateKeys.FINAL_RESULT);
        return result;

    }
}
