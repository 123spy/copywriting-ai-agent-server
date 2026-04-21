package com.spy.copywritingaiagentserver.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.spy.copywritingaiagentserver.ai.model.*;
import com.spy.copywritingaiagentserver.workflow.state.WorkflowStateKeys;
import com.spy.copywritingaiagentserver.workflow.state.WorkflowStateReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ReviseDispatchNode {

    private final WorkflowStateReader reader;

    public Map<String, Object> execute(OverAllState state) {

        return Map.of(
                WorkflowStateKeys.TRACE_LOGS, List.of("finalize finished")
        );
    }
}