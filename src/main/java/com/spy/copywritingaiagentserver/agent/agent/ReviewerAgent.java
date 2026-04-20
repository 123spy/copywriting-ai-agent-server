package com.spy.copywritingaiagentserver.agent.agent;

import com.spy.copywritingaiagentserver.agent.model.ContentPlanResult;
import com.spy.copywritingaiagentserver.agent.model.CopywritingResult;
import com.spy.copywritingaiagentserver.agent.model.RequirementParseResult;
import com.spy.copywritingaiagentserver.agent.model.ReviewResult;
import com.spy.copywritingaiagentserver.agent.model.VisualPromptResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReviewerAgent {

    private final ChatClient chatClient;

    public ReviewerAgent(ChatModel chatModel) {
        this.chatClient = ChatClient
                .builder(chatModel)
                .build();
    }

    private static final String SYSTEM_REVIEWER_AGENT_PROMPT = """
            你是一个“内容评审专家”。

            你的任务不是重写内容，而是根据用户需求、内容策划、文案成品和视觉提示词，
            对最终结果进行质量评估，并输出结构化评审结果。

            你必须遵守以下规则：
            1. 只做评审，不要重写标题、正文、CTA、图片提示词。
            2. 输出结果必须结构化、明确，适合程序直接接收。
            3. 评审必须严格围绕输入需求进行，不要脱离目标平台、人群、风格。
            4. 反馈意见要具体，不要只说“还可以”“不够好”这种空话。
            5. 如果整体质量明显不符合目标，需要明确给出 pass=false。
            6. 你不需要假装真的看到了图片本身，只需要基于图片地址是否存在，以及视觉提示词与文案的一致性进行判断。
            7. 除了总体评审外，你还要判断以下三个局部字段是否需要修正：
               - title
               - cta
               - imagePrompt
            8. 如果某个字段存在明显问题，请将对应的 rewriteXxx 设为 true，并给出具体修改意见。
            9. 如果某个字段没有明显问题，请将对应的 rewriteXxx 设为 false，对应反馈可以为空字符串。
            10. 请谨慎触发重写，不要所有字段都一律要求重写，只有在明显存在问题时才设为 true。

            你必须围绕以下维度评审：
            1. 标题是否有吸引力，是否符合平台表达习惯
            2. 正文是否符合目标人群和指定风格
            3. 正文是否围绕主题和核心卖点展开
            4. CTA 是否自然，不生硬
            5. 图片提示词是否与文案主题、风格、场景一致
            6. 图文整体是否统一

            输出字段要求：
            - pass：布尔值，表示是否通过
            - titleScore：标题评分，0 到 10
            - bodyScore：正文评分，0 到 10
            - imagePromptScore：图片提示词评分，0 到 10
            - overallScore：总体评分，0 到 10
            - feedback：总体反馈
            - rewriteTitle：是否需要重写标题
            - rewriteCta：是否需要重写 CTA
            - rewriteImagePrompt：是否需要重写图片提示词
            - titleFeedback：标题重写意见
            - ctaFeedback：CTA 重写意见
            - imagePromptFeedback：图片提示词重写意见

            判定建议：
            - overallScore >= 9.5 且没有明显硬伤，可以 pass=true
            - overallScore < 9.5 或存在明显风格偏差、平台不匹配、图文不一致，则 pass=false

            字段级判断建议：
            - 如果标题吸引力弱、平台感弱、表达平淡，则 rewriteTitle=true
            - 如果 CTA 生硬、太弱、缺乏互动感，则 rewriteCta=true
            - 如果图片提示词抽象、场景不清、与文案不一致，则 rewriteImagePrompt=true

            你的输出必须只包含结构化评审结果。
            """;

    public ReviewResult execute(
            RequirementParseResult requirementParseResult,
            ContentPlanResult contentPlanResult,
            CopywritingResult copywritingResult,
            VisualPromptResult visualPromptResult,
            String imageUrl
    ) {

        String userMessage = """
                请对下面这组内容生成结果进行评审：

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

                【视觉提示词】
                风格：%s
                场景：%s
                构图：%s
                图片提示词：%s

                【图片结果】
                图片地址：%s

                请输出结构化评审结果。
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
                safe(copywritingResult.getCta()),
                safe(visualPromptResult.getStyle()),
                safe(visualPromptResult.getScene()),
                safe(visualPromptResult.getComposition()),
                safe(visualPromptResult.getImagePrompt()),
                safe(imageUrl)
        );

//        log.info("ReviewerAgent 输入: {}", userMessage);

        ReviewResult reviewResult = chatClient
                .prompt()
                .system(SYSTEM_REVIEWER_AGENT_PROMPT)
                .user(userMessage)
                .call()
                .entity(ReviewResult.class);

//        log.info("ReviewerAgent 输出: {}", reviewResult);

        return reviewResult;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}