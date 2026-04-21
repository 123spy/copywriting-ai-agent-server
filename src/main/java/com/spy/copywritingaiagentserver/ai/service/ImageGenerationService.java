package com.spy.copywritingaiagentserver.ai.service;

import com.spy.copywritingaiagentserver.ai.model.VisualPromptResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ImageGenerationService {

    // todo 使用真实的图片生成服务
    public String generate(VisualPromptResult visualPromptResult) {
        log.info("ImageGenerationService start: style={}, hasPrompt={}",
                visualPromptResult == null ? "" : visualPromptResult.getStyle(),
                visualPromptResult != null
                        && visualPromptResult.getImagePrompt() != null
                        && !visualPromptResult.getImagePrompt().isBlank());
        String imageUrl = "https://mock-image-url/demo.png";
        log.info("ImageGenerationService done: imageUrl={}", imageUrl);
        return imageUrl;
    }

}
