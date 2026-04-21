package com.spy.copywritingaiagentserver.ai.agent;

import com.spy.copywritingaiagentserver.ai.model.ContentPlanResult;
import com.spy.copywritingaiagentserver.ai.model.RequirementParseResult;
import com.spy.copywritingaiagentserver.ai.service.KnowledgeRetrievalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PlannerAgent extends BasicAgent{

    public PlannerAgent(ChatModel chatModel, ToolCallback[] allTools) {
        super(chatModel, allTools);
    }

    private static final String SYSTEM_PLANNER_AGENT_PROMPT = """
            你是一个“内容策划专家”。

            你的任务不是直接写最终文案，而是根据用户需求，
            先制定一份清晰、可执行的内容策划方案，
            供后续的 CopywriterAgent 和 VisualPromptAgent 使用。

            你必须遵守以下规则：
            1. 只做内容策划，不要直接输出完整标题、正文、CTA、图片提示词。
            2. 输出内容必须结构化、明确、可执行。
            3. 内容策划必须围绕用户需求展开，不要跑题。
            4. 要结合平台、人群、风格、卖点，给出适合的表达策略。
            5. 输出结果要围绕以下字段：
               - contentAngle：内容切入角度
               - hookStrategy：开头钩子策略
               - corePoints：核心表达点列表
               - structureAdvice：内容结构建议
               - ctaStrategy：结尾互动/转化策略
               - visualStyle：视觉风格建议

            对字段的要求：
            - contentAngle：一句话说明整篇内容的核心切入点
            - hookStrategy：一句话说明开头如何抓住目标用户
            - corePoints：输出为列表，建议 3~5 条
            - structureAdvice：一句话说明整篇内容结构
            - ctaStrategy：一句话说明结尾如何引导互动或转化
            - visualStyle：一句话说明图片整体视觉方向

            你的输出必须严格贴合输入需求，并便于后续 Agent 继续处理。
            
            在输出内容策划方案之前，必须先调用网页搜索工具。
            如果搜索到高相关链接，优先读取网页内容，再制定策划方案。
            禁止仅凭常识直接输出策划。
            """;

    public ContentPlanResult execute(RequirementParseResult requirementParseResult) {
        log.info("PlannerAgent start: platform={}, topic={}, tone={}",
                safe(requirementParseResult.getPlatform()),
                safe(requirementParseResult.getTopic()),
                safe(requirementParseResult.getTone()));

        String userMessage = """
                请根据下面的结构化需求，制定内容策划方案：

                【结构化需求】
                平台：%s
                主题：%s
                目标人群：%s
                风格：%s
                核心卖点：%s
                内容目标：%s
                额外限制：%s

                请根据参考资料输出结构化策划结果。
                """.formatted(
                safe(requirementParseResult.getPlatform()),
                safe(requirementParseResult.getTopic()),
                safe(requirementParseResult.getTargetAudience()),
                safe(requirementParseResult.getTone()),
                requirementParseResult.getSellingPoints() == null ? "[]" : requirementParseResult.getSellingPoints(),
                safe(requirementParseResult.getContentGoal()),
                safe(requirementParseResult.getConstraints())
        );

        ContentPlanResult contentPlanResult = this.getChatClient()
                .prompt()
                .system(SYSTEM_PLANNER_AGENT_PROMPT)
                .user(userMessage)
                .call()
                .entity(ContentPlanResult.class);

        log.info("PlannerAgent done: contentAngle={}, corePointsCount={}",
                safe(contentPlanResult.getContentAngle()),
                contentPlanResult.getCorePoints() == null ? 0 : contentPlanResult.getCorePoints().size());

        return contentPlanResult;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
