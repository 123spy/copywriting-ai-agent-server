package com.spy.copywritingaiagentserver.ai.agent;

import com.spy.copywritingaiagentserver.ai.model.ContentPlanResult;
import com.spy.copywritingaiagentserver.ai.model.CopywritingResult;
import com.spy.copywritingaiagentserver.ai.model.RequirementParseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CopywriterAgent extends BasicAgent {

    public CopywriterAgent(ChatModel chatModel, ToolCallback[] allTools) {
        super(chatModel, allTools);
    }

    private static final String SYSTEM_COPYWRITER_AGENT_PROMPT = """
        你是一名内容文案写作专家。
        你的任务是根据用户需求、内容策划方案和参考资料，生成最终可发布的文案。

        工具使用规则：
        1. 任务涉及品牌、产品、竞品、市场信息、平台表达方式时，优先调用网页搜索工具。
        2. 如果搜索结果中出现明显相关的网页链接，继续调用网页读取工具获取正文后再写作。
        3. 本地参考资料只能作为补充，不能在未搜索网页时直接作为唯一依据。
        4. 如果没有调用网页搜索工具，不要直接输出最终文案。
        5. 如果搜索不到可靠资料，可以说明未检索到可靠网页资料，但不要编造事实。

        写作规则：
        1. 严格基于输入写作，不要偏题。
        2. 用户原始输入中已经给出的品牌名、主题设定、卖点表述可以保留，不要擅自改成别的品牌或别的主题。
        3. 不能自行补充输入中没有给出、且网页资料也没有支持的具体参数、芯片、系统版本、实验数据、机构背书、用户评价。
        4. 如果某个卖点只有抽象描述，不要把它扩写成看似专业但不可验证的硬参数。
        5. 如果没有明确测试条件，不要写成“实测证明”“稳定保持”“后台保活X个应用”这类强结论。
        6. 如果要做对比，必须有明确锚点；没有明确对象时，不要写模糊竞品对比。
        7. 文案整体要自然、真实、克制，避免堆砌参数和过度推销。
        8. 标题、开头、正文、CTA 要前后一致。
        9. 不要输出图片提示词或策划建议，只输出文案成品。

        输出字段：
        - title
        - openingHook
        - body
        - cta
        """;

    public CopywritingResult execute(
            RequirementParseResult requirementParseResult,
            ContentPlanResult contentPlanResult,
            String reference) {
        log.info("CopywriterAgent start: platform={}, topic={}, angle={}",
                safe(requirementParseResult.getPlatform()),
                safe(requirementParseResult.getTopic()),
                safe(contentPlanResult.getContentAngle()));

        String userMessage = """
                请先使用网页搜索工具检索与主题、品牌、产品、平台表达相关的网页资料。
                如果搜索结果中有明显相关链接，请继续读取网页正文，再根据下面信息生成最终文案。

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

                写作要求：
                1. 用户输入中已有的设定可以保留，但不要自行扩写成无法验证的硬事实。
                2. 如果资料不足，请写得更稳妥、更像真实体验，不要硬凑参数。
                3. 没有明确测试条件时，不要写“实测续航两天”“后台保活三个应用”这类强结论。
                4. 没有明确对比对象时，不要写 A 机/B 机/C 机式模糊对比。
                5. 最终只输出结构化文案结果，不要额外解释。
                """.formatted(
                safe(reference),
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
                请只重写标题，不要改正文和 CTA。

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

                【标题修改意见】
                %s

                要求：
                1. 只输出新的标题。
                2. 保持和正文主题一致。
                3. 更符合平台语感和可信表达。
                4. 不要输出解释。
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

    public String rewriteCta(
            RequirementParseResult requirementParseResult,
            ContentPlanResult contentPlanResult,
            CopywritingResult copywritingResult,
            String feedback
    ) {
        String userMessage = """
                请只重写 CTA，不要改标题和正文。

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

                【CTA 修改意见】
                %s

                要求：
                1. 只输出新的 CTA。
                2. CTA 要自然、克制，不要过度推销。
                3. 避免形成未授权导购承诺或强销售指令。
                4. 更符合平台互动氛围。
                5. 不要输出解释。
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

    public String rewriteBody(
            RequirementParseResult requirementParseResult,
            ContentPlanResult contentPlanResult,
            CopywritingResult copywritingResult,
            String feedback
    ) {
        String userMessage = """
                请只重写正文 body，不要改标题、开头钩子和 CTA。

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

                【当前文案】
                标题：%s
                开头钩子：%s
                当前正文：%s
                CTA：%s

                【正文修改意见】
                %s

                重写要求：
                1. 只输出新的 body。
                2. 优先删除或替换评审明确指出的风险内容，不要先做泛泛润色。
                3. 对于没有依据支持的硬参数、具体实验数据、系统版本、芯片型号、机构背书，一律不要自行补充。
                4. 如果某句结论缺少测试条件，就把它改弱、改稳妥，写成真实体验感受，而不是实验性结论。
                5. 如果没有明确竞品锚点，不要保留模糊对比。
                6. 尽量保留原结构和可用信息，做局部修正，不要整段推翻。
                7. 不要输出解释。
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

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
