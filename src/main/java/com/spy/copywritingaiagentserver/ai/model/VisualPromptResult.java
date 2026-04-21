package com.spy.copywritingaiagentserver.ai.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@Builder
public class VisualPromptResult {

    private String style;
    private String scene;
    private String composition;
    private String imagePrompt;

    public VisualPromptResult() {
    }

    public VisualPromptResult(String style, String scene, String composition, String imagePrompt) {
        this.style = style;
        this.scene = scene;
        this.composition = composition;
        this.imagePrompt = imagePrompt;
    }
}
