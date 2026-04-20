package com.spy.copywritingaiagentserver.agent.service;

import com.spy.copywritingaiagentserver.agent.model.VisualPromptResult;
import org.springframework.stereotype.Service;

@Service
public class ImageGenerationService {

    // todo 使用真实的图片生成服务
    public String generate(VisualPromptResult visualPromptResult) {
        System.out.println("图片提示词: " + visualPromptResult.getImagePrompt());
        return "https://mock-image-url/demo.png";
    }

}