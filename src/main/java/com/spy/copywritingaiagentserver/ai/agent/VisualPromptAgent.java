package com.spy.copywritingaiagentserver.ai.agent;

import com.spy.copywritingaiagentserver.ai.model.ContentPlanResult;
import com.spy.copywritingaiagentserver.ai.model.CopywritingResult;
import com.spy.copywritingaiagentserver.ai.model.RequirementParseResult;
import com.spy.copywritingaiagentserver.ai.model.VisualPromptResult;
import com.spy.copywritingaiagentserver.ai.service.KnowledgeRetrievalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VisualPromptAgent extends BasicAgent{


    public VisualPromptAgent(ChatModel chatModel, ToolCallback[] allTools) {
        super(chatModel, allTools);
    }

    private static final String SYSTEM_VISUAL_PROMPT_AGENT_PROMPT = """
            你是一个“视觉提示词设计专家”。

            你的任务不是写文案，也不是生成图片，
            而是根据用户需求、内容策划和最终文案内容，
            生成适合图片生成模型使用的结构化视觉提示信息。

            你必须遵守以下规则：
            1. 只输出视觉相关内容，不要输出正文文案、标题优化建议或解释说明。
            2. 输出结果必须结构化、明确、便于程序直接接收。
            3. 图片风格必须与用户需求、内容策划和文案风格保持一致。
            4. 图片内容要突出真实场景感、主题感和平台适配感。
            5. 不要虚构未提供的品牌名、价格、实验数据、人物身份或专业认证。
            6. 如果文案中没有明确提到某些细节，可以做合理视觉补充，但必须与整体主题一致。
            7. 图片方向要优先服务于“内容展示”和“平台氛围”，不要过度追求夸张广告感。
            8. 视觉风格必须与目标平台调性一致，例如小红书更适合生活化、清新、真实感的视觉表达。

            你输出的字段必须围绕以下内容：
            - style：图片整体视觉风格
            - scene：画面场景描述
            - composition：构图建议
            - imagePrompt：最终给图片模型使用的完整提示词

            对字段要求如下：
            1. style：
               - 一句话
               - 概括整体画面风格、色调、氛围
               - 例如：温柔清新、生活化、奶油色调、自然光
            2. scene：
               - 一句话
               - 描述画面主体场景、关键元素、环境氛围
            3. composition：
               - 一句话
               - 描述镜头视角、主体位置、背景处理、构图方式
            4. imagePrompt：
               - 一段适合图片模型直接使用的完整提示词
               - 要包含主体、场景、风格、色调、构图、氛围
               - 尽量具体，但不要过于冗长
               - 要与文案内容和用户需求高度一致

            你的输出必须能直接服务于后续图片生成模块。
            """;

    public VisualPromptResult execute(
            RequirementParseResult requirementParseResult,
            ContentPlanResult contentPlanResult,
            CopywritingResult copywritingResult
    ) {
        log.info("VisualPromptAgent start: platform={}, topic={}, title={}",
                safe(requirementParseResult.getPlatform()),
                safe(requirementParseResult.getTopic()),
                safe(copywritingResult.getTitle()));

        String userMessage = """
                请根据下面的信息，生成结构化视觉提示结果：
                
                【用户需求】
                平台：%s
                主题：%s
                目标人群：%s
                风格：%s
                核心卖点：%s
                内容目标：%s
                额外限制：%s

                【内容策划】
                内容角度：%s
                开头钩子策略：%s
                核心表达点：%s
                内容结构建议：%s
                CTA策略：%s
                视觉风格建议：%s

                【文案成品】
                标题：%s
                开头钩子：%s
                正文：%s
                CTA：%s

                请根据参考资料输出结构化视觉提示结果。
                """.formatted(
                safe(requirementParseResult.getPlatform()),
                safe(requirementParseResult.getTopic()),
                safe(requirementParseResult.getTargetAudience()),
                safe(requirementParseResult.getTone()),
                requirementParseResult.getSellingPoints() == null ? "[]" : requirementParseResult.getSellingPoints(),
                safe(requirementParseResult.getContentGoal()),
                safe(requirementParseResult.getConstraints()),
                safe(contentPlanResult.getContentAngle()),
                safe(contentPlanResult.getHookStrategy()),
                contentPlanResult.getCorePoints() == null ? "[]" : contentPlanResult.getCorePoints(),
                safe(contentPlanResult.getStructureAdvice()),
                safe(contentPlanResult.getCtaStrategy()),
                safe(contentPlanResult.getVisualStyle()),
                safe(copywritingResult.getTitle()),
                safe(copywritingResult.getOpeningHook()),
                safe(copywritingResult.getBody()),
                safe(copywritingResult.getCta())
        );

        VisualPromptResult visualPromptResult = this.getChatClient()
                .prompt()
                .system(SYSTEM_VISUAL_PROMPT_AGENT_PROMPT)
                .user(userMessage)
                .call()
                .entity(VisualPromptResult.class);

        log.info("VisualPromptAgent done: style={}, hasImagePrompt={}",
                safe(visualPromptResult.getStyle()),
                visualPromptResult.getImagePrompt() != null && !visualPromptResult.getImagePrompt().isBlank());

        return visualPromptResult;
    }

    public String rewriteImagePrompt(
            RequirementParseResult requirementParseResult,
            ContentPlanResult contentPlanResult,
            CopywritingResult copywritingResult,
            VisualPromptResult visualPromptResult,
            String feedback
    ) {

        String userMessage = """
            请根据下面的信息，只重写图片提示词，不要改文案内容。

            【用户需求】
            平台：%s
            主题：%s
            目标人群：%s
            风格：%s

            【内容策划】
            内容角度：%s
            视觉风格建议：%s

            【文案成品】
            标题：%s
            开头钩子：%s
            正文：%s
            CTA：%s

            【当前视觉提示结果】
            风格：%s
            场景：%s
            构图：%s
            当前图片提示词：%s

            【评审意见】
            %s

            要求：
            1. 只输出新的 imagePrompt
            2. 要和文案主题、风格、场景更加一致
            3. 要更适合图片模型生成
            4. 不要输出解释
            """.formatted(
                safe(requirementParseResult.getPlatform()),
                safe(requirementParseResult.getTopic()),
                safe(requirementParseResult.getTargetAudience()),
                safe(requirementParseResult.getTone()),
                safe(contentPlanResult.getContentAngle()),
                safe(contentPlanResult.getVisualStyle()),
                safe(copywritingResult.getTitle()),
                safe(copywritingResult.getOpeningHook()),
                safe(copywritingResult.getBody()),
                safe(copywritingResult.getCta()),
                safe(visualPromptResult.getStyle()),
                safe(visualPromptResult.getScene()),
                safe(visualPromptResult.getComposition()),
                safe(visualPromptResult.getImagePrompt()),
                safe(feedback)
        );

        return this.getChatClient().prompt()
                .user(userMessage)
                .call()
                .content();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
