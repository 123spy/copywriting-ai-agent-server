package com.spy.copywritingaiagentserver.ai.agent;

import com.spy.copywritingaiagentserver.ai.model.ContentPlanResult;
import com.spy.copywritingaiagentserver.ai.model.CopywritingResult;
import com.spy.copywritingaiagentserver.ai.model.RequirementParseResult;
import com.spy.copywritingaiagentserver.ai.model.ReviewResult;
import com.spy.copywritingaiagentserver.ai.model.VisualPromptResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReviewerAgent extends BasicAgent {

    private static final String SYSTEM_REVIEWER_AGENT_PROMPT = """
            你是一名严格的内容审核员，职责是发现问题，不是鼓励或润色。

            审核原则：
            1. 先找问题，再评分，最后给结论。
            2. 对真实性、可验证性、平台适配度、图文一致性和产品相关性都要严格。
            3. 用户原始输入中已经给出的品牌名、主题设定、卖点描述，可以视为任务输入，不要直接判定为模型虚构。
            4. 只有模型自行新增、且输入中没有给出、也没有可靠依据支持的信息，才判定为虚构或不实风险。
            5. 对图片提示词的审核重点，不只是“文案一致”，还包括：是否像真实产品图、是否突出产品主体、是否容易生成成泛手机模板、是否明显偏成老旧 iPhone/老旧安卓外观。

            评分维度：
            - titleScore：标题吸引力、平台语感、可信度，范围 0 到 10
            - bodyScore：正文真实性、结构清晰度、表达稳健性，范围 0 到 10
            - imagePromptScore：视觉提示词与文案一致性、产品相关性、平台适配度、可执行性，范围 0 到 10
            - overallScore：整体分数，范围 0 到 10

            评分参考：
            - 9-10：成熟可用，几乎无明显短板
            - 7-8.9：整体较好，但仍有优化点
            - 5-6.9：可读但问题明显，不能直接通过
            - 0-4.9：存在明显硬伤或方向错误

            pass 的硬规则：
            1. 只要 titleScore、bodyScore、imagePromptScore 任一项小于 7.0，则 pass=false。
            2. 只有三项全部大于等于 7.0 且 overallScore 大于等于 7.0 时，才允许 pass=true。
            3. 只要还存在明显真实性风险、合规风险、正文原则性问题、图片明显不像目标产品、平台视觉明显不适配，pass 仍然必须是 false。
            4. 如果任一 rewrite 标记为 true，则 pass 必须是 false。

            局部重写判断原则：
            1. 标题有问题，置 rewriteTitle=true。
            2. 正文存在事实风险、不可验证强结论、空泛对比、结构性硬伤时，置 rewriteBody=true。
            3. CTA 存在导购风险、过硬推销、行动指令不自然时，置 rewriteCta=true。
            4. 图片提示词与文案不一致、产品主体不突出、容易生成成泛手机、明显像老旧手机、平台不匹配时，置 rewriteImagePrompt=true。

            输出要求：
            1. 输出 titleScore、bodyScore、imagePromptScore、overallScore、pass。
            2. 输出一段总 feedback，必须具体、直接、可执行。
            3. 输出 rewriteTitle、rewriteBody、rewriteCta、rewriteImagePrompt 四个布尔字段。
            4. 输出 titleFeedback、bodyFeedback、ctaFeedback、imagePromptFeedback 四个定向修改建议。
            5. 如果某字段无需修改，对应 feedback 可以为空字符串，对应 rewrite 设为 false。
            6. 只输出结构化结果，不要额外解释。
            """;

    public ReviewerAgent(ChatModel chatModel, ToolCallback[] allTools) {
        super(chatModel, allTools);
    }

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
                请审核下面这组生成结果。

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

        ReviewResult reviewResult = this.getChatClient()
                .prompt()
                .system(SYSTEM_REVIEWER_AGENT_PROMPT)
                .user(userMessage)
                .call()
                .entity(ReviewResult.class);

        normalizeReviewResult(reviewResult);

        log.info("ReviewerAgent done: pass={}, titleScore={}, bodyScore={}, imagePromptScore={}, overallScore={}",
                reviewResult.getPass(),
                reviewResult.getTitleScore(),
                reviewResult.getBodyScore(),
                reviewResult.getImagePromptScore(),
                reviewResult.getOverallScore());

        return reviewResult;
    }

    private void normalizeReviewResult(ReviewResult reviewResult) {
        if (reviewResult == null) {
            return;
        }

        double titleScore = clampScore(reviewResult.getTitleScore());
        double bodyScore = clampScore(reviewResult.getBodyScore());
        double imagePromptScore = clampScore(reviewResult.getImagePromptScore());
        double overallScore = roundScore(titleScore * 0.2 + bodyScore * 0.45 + imagePromptScore * 0.35);

        reviewResult.setTitleScore(titleScore);
        reviewResult.setBodyScore(bodyScore);
        reviewResult.setImagePromptScore(imagePromptScore);
        reviewResult.setOverallScore(overallScore);

        boolean hasRewrite =
                reviewResult.isRewriteTitle()
                        || reviewResult.isRewriteBody()
                        || reviewResult.isRewriteCta()
                        || reviewResult.isRewriteImagePrompt();
        boolean hardPass =
                titleScore >= 7.0
                        && bodyScore >= 7.0
                        && imagePromptScore >= 7.0
                        && overallScore >= 7.0
                        && !hasRewrite;
        reviewResult.setPass(Boolean.TRUE.equals(reviewResult.getPass()) && hardPass);
    }

    private double clampScore(double score) {
        if (Double.isNaN(score) || Double.isInfinite(score)) {
            return 0.0;
        }
        if (score < 0) {
            return 0.0;
        }
        if (score > 10) {
            return 10.0;
        }
        return roundScore(score);
    }

    private double roundScore(double score) {
        return Math.round(score * 10.0) / 10.0;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
