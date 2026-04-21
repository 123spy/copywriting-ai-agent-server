package com.spy.copywritingaiagentserver.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.spy.copywritingaiagentserver.ai.model.VisualPromptResult;
import com.spy.copywritingaiagentserver.ai.service.ImageGenerationService;
import com.spy.copywritingaiagentserver.workflow.state.WorkflowStateKeys;
import com.spy.copywritingaiagentserver.workflow.state.WorkflowStateReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GenerateImageNode {

    private final ImageGenerationService imageGenerationService;
    private final WorkflowStateReader reader;

    public Map<String, Object> execute(OverAllState state) {
        VisualPromptResult visualPromptResult =
                reader.get(state, WorkflowStateKeys.VISUAL_PROMPT_RESULT, VisualPromptResult.class);

        String imageUrl = imageGenerationService.generate(visualPromptResult);

        return Map.of(
                WorkflowStateKeys.IMAGE_URL, imageUrl,
                WorkflowStateKeys.TRACE_LOGS, List.of("generateImage finished")
        );
    }
}