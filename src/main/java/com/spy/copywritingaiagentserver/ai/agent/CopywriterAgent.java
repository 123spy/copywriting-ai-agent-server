package com.spy.copywritingaiagentserver.ai.agent;

import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.spy.copywritingaiagentserver.ai.model.ContentPlanResult;
import com.spy.copywritingaiagentserver.ai.model.CopywritingResult;
import com.spy.copywritingaiagentserver.ai.model.RequirementParseResult;
import com.spy.copywritingaiagentserver.ai.service.KnowledgeRetrievalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CopywriterAgent extends BasicAgent {


    private final KnowledgeRetrievalService knowledgeRetrievalService;


    public CopywriterAgent(ChatModel chatModel, ToolCallback[] allTools, KnowledgeRetrievalService knowledgeRetrievalService) {
        super(chatModel, allTools);

        this.knowledgeRetrievalService = knowledgeRetrievalService;
    }

    private static final String SYSTEM_COPYWRITER_AGENT_PROMPT = """
        你是一个“内容文案写作专家”。
        
        你的任务是根据用户需求、内容策划方案，以及外部搜索到的资料，生成最终可发布的文案内容。
        
        你必须遵守以下工具使用规则：
        1. 只要任务涉及品牌、产品、竞品、市场信息、平台表达方式、网页资料，你必须优先调用网页搜索工具。
        2. 如果搜索结果中出现明显相关的网页链接，你应继续调用网页读取工具获取页面内容后，再进行写作。
        3. 本地知识库资料只能作为补充参考，不能在没有网页搜索的情况下直接当作唯一依据。
        4. 如果没有调用网页搜索工具，不要直接输出最终文案。
        5. 如果搜索不到有效信息，可以说明“未检索到可靠网页资料”，再基于已有输入谨慎生成，但不能虚构事实。
        
        你的写作规则：
        1. 必须严格基于输入的用户需求、内容策划方案、参考资料写作，不要偏题。
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
        """;

    public CopywritingResult execute(RequirementParseResult requirementParseResult, ContentPlanResult contentPlanResult) {
        log.info("CopywriterAgent start: platform={}, topic={}, angle={}",
                safe(requirementParseResult.getPlatform()),
                safe(requirementParseResult.getTopic()),
                safe(contentPlanResult.getContentAngle()));

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
请先使用网页搜索工具检索与主题、品牌、产品、平台表达相关的网页资料。
如果搜索结果里有明显相关的链接，请继续使用网页读取工具获取页面正文内容。
完成搜索和读取后，再根据下面的信息生成最终文案。

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

要求：
1. 必须先检索网页资料，再写文案
2. 如果网页资料与本地参考资料冲突，以更可靠、更新的网页信息为准
3. 不要跳过工具直接写结果
4. 最终只输出结构化文案结果
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

        CopywritingResult copywritingResult = this.getChatClient()
                .prompt()
                .system(SYSTEM_COPYWRITER_AGENT_PROMPT)
                .user(userMessage)
                .call()
                .entity(CopywritingResult.class);

        log.info("CopywriterAgent done: title={}, hasBody={}, hasCta={}",
                safe(copywritingResult.getTitle()),
                copywritingResult.getBody() != null && !copywritingResult.getBody().isBlank(),
                copywritingResult.getCta() != null && !copywritingResult.getCta().isBlank());

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

        return this.getChatClient().prompt()
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

        return this.getChatClient().prompt()
                .user(userMessage)
                .call()
                .content();
    }



    private String safe(String value) {
        return value == null ? "" : value;
    }
}
