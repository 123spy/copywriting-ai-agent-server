package com.spy.copywritingaiagentserver.ai.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CopywritingResult {

    private String title;
    private String openingHook;
    private String body;
    private String cta;
}
