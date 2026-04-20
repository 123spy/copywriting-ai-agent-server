package com.spy.copywritingaiagentserver.agent.agent;

import com.spy.copywritingaiagentserver.agent.model.RequirementParseResult;
import com.spy.copywritingaiagentserver.agent.model.UserRequirement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RequirementAgent {

    private final ChatClient chatClient;

    public RequirementAgent(ChatModel chatModel) {
        this.chatClient = ChatClient
                .builder(chatModel)
                .build();
    }

    private static final String SYSTEM_REQUIREMENT_AGENT_PROMPT = """
            你是一个“内容创作需求解析专家”。
            
            你的任务不是写文案，而是把用户输入的原始创作需求，整理成结构化结果，
            供后续的 PlannerAgent、CopywriterAgent、VisualPromptAgent 使用。
            
            你必须遵守以下规则：
            1. 只做需求解析，不要生成正文、标题、CTA、图片提示词。
            2. 输出内容必须结构化、明确、简洁，适合程序直接接收。
            3. 如果用户输入里有模糊表达，请结合上下文做合理归纳，但不要虚构明显不存在的信息。
            4. 如果某项信息缺失，可以结合语义做温和推断，但要尽量保守。
            5. 输出结果要围绕以下字段：
               - platform：内容发布平台，请只输出（小红书、公众号、抖音图文）这三种，如果用户输出的是其他的平台，那么就输出一个这三个中和这个平台相近的即可。
               - topic：内容主题
               - targetAudience：目标人群
               - tone：内容风格
               - sellingPoints：核心卖点列表
               - contentGoal：内容目标，例如种草、转化、品牌展示、引流
               - constraints：额外限制，例如避免硬广感、口语化、真实分享感
            
            对字段的要求：
            - platform：尽量标准化
            - topic：提炼成一句清晰主题
            - targetAudience：尽量具体
            - tone：提炼成 1~2 个风格短语
            - sellingPoints：输出为列表，尽量 3~5 条
            - contentGoal：只保留一个最主要目标
            - constraints：尽量提炼为一句话
            
            你的输出必须严格贴合用户需求，便于后续 Agent 继续处理。
            """;

    public RequirementParseResult execute(UserRequirement userRequirement) {

        String userMessage = """
                请解析下面的用户创作需求：

                平台：%s
                主题：%s
                目标人群：%s
                风格：%s
                产品信息：%s

                请输出结构化结果。
                """.formatted(
                safe(userRequirement.getPlatform()),
                safe(userRequirement.getTopic()),
                safe(userRequirement.getAudience()),
                safe(userRequirement.getTone()),
                safe(userRequirement.getProductInfo())
        );

//        log.info("RequirementAgent 输入: {}", userMessage.toString());

        RequirementParseResult requirementParseResult = chatClient
                .prompt()
                .system(SYSTEM_REQUIREMENT_AGENT_PROMPT)
                .user(userMessage)
                .call()
                .entity(RequirementParseResult.class);

//        log.info("RequirementAgent 输出: {}", requirementParseResult.toString());

        return requirementParseResult;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}