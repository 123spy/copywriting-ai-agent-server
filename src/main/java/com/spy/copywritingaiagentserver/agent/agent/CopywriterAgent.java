package com.spy.copywritingaiagentserver.agent.agent;

import com.spy.copywritingaiagentserver.agent.model.ContentPlanResult;
import com.spy.copywritingaiagentserver.agent.model.CopywritingResult;
import com.spy.copywritingaiagentserver.agent.model.RequirementParseResult;
import com.spy.copywritingaiagentserver.agent.service.KnowledgeRetrievalService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Component
public class CopywriterAgent {

    private final ChatClient chatClient;
    private final KnowledgeRetrievalService knowledgeRetrievalService;


    public CopywriterAgent(ChatModel chatModel, KnowledgeRetrievalService knowledgeRetrievalService) {
        chatClient = ChatClient
                .builder(chatModel)
                .build();
        this.knowledgeRetrievalService = knowledgeRetrievalService;
    }

    private static final String SYSTEM_COPYWRITER_AGENT_PROMPT = """
            你是一个“内容文案写作专家”。

            你的任务是根据已经整理好的用户需求和内容策划方案，
            生成最终可发布的文案内容。

            你不是需求分析师，也不是内容策划师。
            你不需要重新分析需求，也不需要重新做策划。
            你的职责是把已有的需求和策划结果，准确、自然地落成最终文案。

            你必须遵守以下规则：
            1. 必须严格基于输入的用户需求和内容策划结果写作，不要偏题。
            2. 文案风格必须贴合目标平台、目标人群和指定语气。
            3. 不要输出解释说明，不要输出分析过程，只输出最终结构化文案结果。
            4. 不要虚构未提供的产品功效、用户评价、品牌背景或实验数据。
            5. 如果输入中没有明确的品牌名、价格、参数等信息，不要擅自补充。
            6. 文案整体要自然、真实，避免过度生硬、堆砌卖点或强硬推销。
            7. 标题、开头、正文、CTA 必须前后一致，不能风格割裂。
            8. 不要输出图片提示词，不要输出策划建议，只输出文案成品。

            输出字段要求如下：
            - title：最终标题，要求简洁、有吸引力、符合平台风格
            - openingHook：开头钩子，要求自然引出主题，快速抓住用户注意力
            - body：正文主体，要求围绕策划方案展开，表达清晰、层次自然
            - cta：结尾行动引导，要求符合平台氛围，不要生硬推销

            对各字段的额外要求：
            1. title：
               - 一句话
               - 要有内容感和吸引力
               - 尽量贴合目标平台表达方式
            2. openingHook：
               - 一小段
               - 用于快速引起共鸣、好奇或代入感
            3. body：
               - 是主体内容
               - 要覆盖策划中的核心表达点
               - 要自然连贯，不要像列清单
            4. cta：
               - 一小段
               - 以互动、评论、经验分享、轻度引导为主
               - 不要太像硬广告

            你的输出必须便于后续 VisualPromptAgent 使用。
            因此整篇文案要清晰表达内容主题、场景感和风格感。
            """;

    public CopywritingResult execute(RequirementParseResult requirementParseResult, ContentPlanResult contentPlanResult) {
        String ragQuery = """
        平台：%s
        主题：%s
        风格：%s
        文案案例
        标题模板
        CTA模板
        正文表达
        """.formatted(
                safe(requirementParseResult.getPlatform()),
                safe(requirementParseResult.getTopic()),
                safe(requirementParseResult.getTone())
        );
        String references = knowledgeRetrievalService.searchAsText(ragQuery, 6);


        String userMessage = """
                请根据下面的用户需求和内容策划方案，生成最终文案内容：
                【参考资料】
                %s
                
                【用户需求】
                平台：%s
                主题：%s
                目标人群：%s
                风格：%s
                核心卖点：%s
                内容目标：%s
                额外限制：%s

                【内容策划方案】
                内容角度：%s
                开头钩子策略：%s
                核心表达点：%s
                内容结构建议：%s
                CTA策略：%s
                视觉风格建议：%s

                请参考资料输出结构化文案结果。
                """.formatted(
                references,
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
                safe(contentPlanResult.getVisualStyle())
        );

        CopywritingResult copywritingResult = chatClient
                .prompt()
                .system(SYSTEM_COPYWRITER_AGENT_PROMPT)
                .user(userMessage)
                .call()
                .entity(CopywritingResult.class);

        return copywritingResult;
    }

    public String rewriteTitle(
            RequirementParseResult requirementParseResult,
            ContentPlanResult contentPlanResult,
            CopywritingResult copywritingResult,
            String feedback
    ) {
        String userMessage = """
            请根据下面的信息，只重写标题，不要改正文和 CTA。

            【用户需求】
            平台：%s
            主题：%s
            目标人群：%s
            风格：%s

            【内容策划】
            内容角度：%s
            开头钩子策略：%s

            【当前文案】
            当前标题：%s
            开头钩子：%s
            正文：%s
            CTA：%s

            【评审意见】
            %s

            要求：
            1. 只输出新的标题
            2. 标题要更符合平台风格
            3. 标题要更有吸引力
            4. 不要输出解释
            """.formatted(
                safe(requirementParseResult.getPlatform()),
                safe(requirementParseResult.getTopic()),
                safe(requirementParseResult.getTargetAudience()),
                safe(requirementParseResult.getTone()),
                safe(contentPlanResult.getContentAngle()),
                safe(contentPlanResult.getHookStrategy()),
                safe(copywritingResult.getTitle()),
                safe(copywritingResult.getOpeningHook()),
                safe(copywritingResult.getBody()),
                safe(copywritingResult.getCta()),
                safe(feedback)
        );

        return chatClient.prompt()
                .user(userMessage)
                .call()
                .content();
    }

    public String rewriteCta(RequirementParseResult requirementParseResult, ContentPlanResult contentPlanResult, CopywritingResult copywritingResult, String feedback) {
        String userMessage = """
            请根据下面的信息，只重写 CTA，不要改标题和正文。

            【用户需求】
            平台：%s
            主题：%s
            目标人群：%s
            风格：%s

            【内容策划】
            CTA策略：%s

            【当前文案】
            标题：%s
            正文：%s
            当前 CTA：%s

            【评审意见】
            %s

            要求：
            1. 只输出新的 CTA
            2. CTA 要自然、不生硬
            3. 更符合平台互动氛围
            4. 不要输出解释
            """.formatted(
                safe(requirementParseResult.getPlatform()),
                safe(requirementParseResult.getTopic()),
                safe(requirementParseResult.getTargetAudience()),
                safe(requirementParseResult.getTone()),
                safe(contentPlanResult.getCtaStrategy()),
                safe(copywritingResult.getTitle()),
                safe(copywritingResult.getBody()),
                safe(copywritingResult.getCta()),
                safe(feedback)
        );

        return chatClient.prompt()
                .user(userMessage)
                .call()
                .content();
    }



    private String safe(String value) {
        return value == null ? "" : value;
    }
}