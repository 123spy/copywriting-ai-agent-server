package com.spy.copywritingaiagentserver.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.spy.copywritingaiagentserver.ai.model.CopywritingResult;
import com.spy.copywritingaiagentserver.ai.model.VisualPromptResult;
import com.spy.copywritingaiagentserver.workflow.state.WorkflowStateKeys;
import com.spy.copywritingaiagentserver.workflow.state.WorkflowStateReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MergeRevisionNode {

    private final WorkflowStateReader reader;

    public Map<String, Object> execute(OverAllState state) {
        CopywritingResult originalCopywritingResult =
                reader.get(state, WorkflowStateKeys.COPYWRITING_RESULT, CopywritingResult.class);

        VisualPromptResult originalVisualPromptResult =
                reader.get(state, WorkflowStateKeys.VISUAL_PROMPT_RESULT, VisualPromptResult.class);

        String rewrittenTitle =
                reader.getString(state, WorkflowStateKeys.TITLE_REWRITTEN);

        String rewrittenBody =
                reader.getString(state, WorkflowStateKeys.BODY_REWRITTEN);

        String rewrittenCta =
                reader.getString(state, WorkflowStateKeys.CTA_REWRITTEN);

        String rewrittenImagePrompt =
                reader.getString(state, WorkflowStateKeys.IMAGE_PROMPT_REWRITTEN);

        CopywritingResult mergedCopywritingResult = CopywritingResult.builder()
                .title(isNotBlank(rewrittenTitle) ? rewrittenTitle : originalCopywritingResult.getTitle())
                .openingHook(originalCopywritingResult.getOpeningHook())
                .body(isNotBlank(rewrittenBody) ? rewrittenBody : originalCopywritingResult.getBody())
                .cta(isNotBlank(rewrittenCta) ? rewrittenCta : originalCopywritingResult.getCta())
                .build();

        VisualPromptResult mergedVisualPromptResult = VisualPromptResult.builder()
                .style(originalVisualPromptResult.getStyle())
                .scene(originalVisualPromptResult.getScene())
                .composition(originalVisualPromptResult.getComposition())
                .imagePrompt(isNotBlank(rewrittenImagePrompt)
                        ? rewrittenImagePrompt
                        : originalVisualPromptResult.getImagePrompt())
                .build();

        return Map.of(
                WorkflowStateKeys.COPYWRITING_RESULT, mergedCopywritingResult,
                WorkflowStateKeys.VISUAL_PROMPT_RESULT, mergedVisualPromptResult,
                WorkflowStateKeys.TRACE_LOGS, List.of("mergeRevision finished")
        );
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
