package com.spy.copywritingaiagentserver.ai.agent;

import com.spy.copywritingaiagentserver.ai.model.ContentPlanResult;
import com.spy.copywritingaiagentserver.ai.model.CopywritingResult;
import com.spy.copywritingaiagentserver.ai.model.RequirementParseResult;
import com.spy.copywritingaiagentserver.ai.model.ReviewResult;
import com.spy.copywritingaiagentserver.ai.model.VisualPromptResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReviewerAgent extends BasicAgent{

    public ReviewerAgent(ChatModel chatModel, ToolCallback[] allTools) {
        super(chatModel, allTools);
    }

    private static final String SYSTEM_REVIEWER_AGENT_PROMPT = """
        你是一个“严格内容审核员”，不是鼓励型评委，也不是夸奖型顾问。
        
        你的职责不是润色赞美，而是尽可能发现文案和视觉提示词中的问题，并做严格评分。
        
        你必须遵守以下规则：
        1. 先找问题，再打分，最后给结论。
        2. 不要因为整体流畅就给高分。
        3. 只有在某一项明显优秀、几乎没有改进空间时，才允许给 1.8 分以上。
        4. 只要存在明显问题，就必须扣分，不要做“可接受范围内不扣分”的宽松处理。
        5. 对品牌识别弱、平台适配不足、CTA偏硬或偏弱、视觉与文案主题不一致等问题，必须明确指出。
        6. 如果存在任何一项明显短板，即使整体不错，也不能直接判为高分通过。
        
        请从以下 5 个维度分别评分，每项 0~2 分：
        - titleScore：标题吸引力与平台适配度
        - hookScore：开头钩子的代入感与抓人程度
        - bodyScore：正文结构、卖点表达、真实感
        - ctaScore：CTA自然度与互动感
        - visualScore：视觉提示词与文案主题、平台风格的一致性
        
        评分标准：
        - 0.0~0.9：明显不合格
        - 1.0~1.4：有明显缺陷
        - 1.5~1.7：合格但仍有改进空间
        - 1.8~2.0：非常优秀，只有在几乎无明显缺点时才可给出
        
        输出要求：
        1. 输出每项分数
        2. 输出 overallScore（总分，满分10）
        3. 输出 pass（是否通过）
        4. 输出 problems：明确列出至少 1~5 个问题；如果没有明显问题，也要写出最可能影响效果的薄弱点
        5. 输出 feedback：简洁、具体、可执行
        
        通过规则：
        - 只要任一单项低于 1.5，则 pass=false
        - 只要 overallScore 低于 8.8，则 pass=false
        - 只有五项都比较强，且没有明显短板时，才允许 pass=true
        """;

    public ReviewResult execute(
            RequirementParseResult requirementParseResult,
            ContentPlanResult contentPlanResult,
            CopywritingResult copywritingResult,
            VisualPromptResult visualPromptResult,
            String imageUrl
    ) {
        log.info("ReviewerAgent start: platform={}, topic={}, imageGenerated={}",
                safe(requirementParseResult.getPlatform()),
                safe(requirementParseResult.getTopic()),
                imageUrl != null && !imageUrl.isBlank());

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

        ReviewResult reviewResult = this.getChatClient()
                .prompt()
                .system(SYSTEM_REVIEWER_AGENT_PROMPT)
                .user(userMessage)
                .call()
                .entity(ReviewResult.class);

        log.info("ReviewerAgent done: pass={}, overallScore={}",
                reviewResult.getPass(),
                reviewResult.getOverallScore());

//        log.info("ReviewerAgent 输出: {}", reviewResult);

        return reviewResult;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
