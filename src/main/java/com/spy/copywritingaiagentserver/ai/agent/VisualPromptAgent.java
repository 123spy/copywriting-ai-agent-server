package com.spy.copywritingaiagentserver.ai.agent;

import com.spy.copywritingaiagentserver.ai.model.ContentPlanResult;
import com.spy.copywritingaiagentserver.ai.model.CopywritingResult;
import com.spy.copywritingaiagentserver.ai.model.RequirementParseResult;
import com.spy.copywritingaiagentserver.ai.model.VisualPromptResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VisualPromptAgent extends BasicAgent {

    private static final String SYSTEM_VISUAL_PROMPT_AGENT_PROMPT = """
            你是一名中文内容平台的产品视觉提示词专家，负责生成可直接用于图片模型的高质量提示词。

            你的任务不是写文案，而是输出结构化视觉结果：
            - style：整体视觉风格
            - scene：真实可拍摄的场景描述
            - composition：主体占比、镜头距离、构图、景深、留白
            - imagePrompt：最终给图像模型直接使用的完整英文提示词

            必须遵守：
            1. 视觉结果优先服务“产品识别”，不是单纯氛围图。
            2. 对手机类产品，优先生成“现代智能手机产品图”，不要生成老旧手机模板。
            3. 如果没有参考图，不要瞎编品牌 logo、具体型号、芯片、系统名、实验参数。
            4. 必须尽量通过外观语言约束产品：超窄边框、现代正面、精致中框、明确后摄区、现代材质，而不是 generic phone。
            5. 场景只能辅助产品，不能抢主体；杂物数量要少，手部只可少量辅助展示比例和触感。
            6. 默认优先“桌面产品主图 / 半场景产品主图”，除非文案核心就是手持、通勤、户外抓拍，否则不要优先生成手持通勤照。
            7. 不要大量参数浮层、跑分、温度、电量、帧率、HUD、赛博蓝光；除非输入明确要求。
            8. 不要影楼风、广告海报风、概念渲染风、廉价电商白底风。
            9. imagePrompt 结尾必须带清晰的负面约束，避免：generic phone、old iphone style、thick bezels、home button、poster ad look、overdesigned HUD、too many props。

            平台差异：
            - 小红书：更像高点击封面，但依然要真实、精致、主体大、可识别。
            - 抖音图文：首屏冲击更强，主体更近、更前，节奏更快。
            - 公众号：更像可信评测配图，克制、稳、真实。
            """;

    public VisualPromptAgent(ChatModel chatModel, ToolCallback[] allTools) {
        super(chatModel, allTools);
    }

    public VisualPromptResult execute(
            RequirementParseResult requirementParseResult,
            ContentPlanResult contentPlanResult,
            CopywritingResult copywritingResult
    ) {
        log.info("VisualPromptAgent start: platform={}, topic={}, title={}",
                safe(requirementParseResult.getPlatform()),
                safe(requirementParseResult.getTopic()),
                safe(copywritingResult.getTitle()));

        String platform = safe(requirementParseResult.getPlatform());
        String userMessage = """
                请根据下面信息生成结构化视觉提示结果。

                【平台】
                %s

                【平台目标】
                %s

                【平台构图规则】
                %s

                【主体策略】
                %s

                【负面约束重点】
                %s

                【用户需求】
                主题：%s
                目标人群：%s
                风格：%s
                核心卖点：%s
                内容目标：%s
                限制：%s

                【内容策划】
                内容角度：%s
                视觉风格建议：%s

                【文案成品】
                标题：%s
                开头钩子：%s
                正文：%s
                CTA：%s

                【生成要求】
                1. 优先生成“产品主体清楚的大图”，不要先生成情绪氛围图。
                2. 手机主体默认占画面 60%% 到 75%%；如果平台是小红书或抖音图文，优先竖版。
                3. 默认优先桌面主图、半场景主图、轻微手势辅助图；除非文案明确强依赖通勤抓拍，不要优先地铁手持窗边照。
                4. 不要写大量具体数值 UI，不要写温度、电量、帧率、后台应用数量、手写便签数字。
                5. 不要写 logo，不要写具体品牌字样，但必须写清现代手机的结构语言：contemporary flagship smartphone, ultra-slim symmetrical bezels, modern camera module, refined metal frame, matte glass texture。
                6. scene 要真实可拍摄；composition 要具体写主体位置、主体占比、镜头距离、景深和留白；imagePrompt 要是完整英文提示词。
                7. imagePrompt 中必须明确限制：不要 generic phone、不要 old iphone style、不要 home button、不要 thick bezels、不要 cluttered props、不要 poster ad aesthetic。
                """.formatted(
                platform,
                getPlatformGoal(platform),
                getPlatformCompositionRules(platform),
                getPlatformSubjectRules(platform),
                getPlatformNegativeRules(platform),
                safe(requirementParseResult.getTopic()),
                safe(requirementParseResult.getTargetAudience()),
                safe(requirementParseResult.getTone()),
                requirementParseResult.getSellingPoints() == null ? "[]" : requirementParseResult.getSellingPoints(),
                safe(requirementParseResult.getContentGoal()),
                safe(requirementParseResult.getConstraints()),
                safe(contentPlanResult.getContentAngle()),
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
        String platform = safe(requirementParseResult.getPlatform());
        String userMessage = """
                请只重写 imagePrompt，不要改 style、scene、composition，也不要改文案内容。

                【平台】
                %s

                【平台目标】
                %s

                【平台构图规则】
                %s

                【主体策略】
                %s

                【负面约束重点】
                %s

                【内容策划】
                内容角度：%s
                视觉风格建议：%s

                【文案成品】
                标题：%s
                开头钩子：%s
                正文：%s
                CTA：%s

                【当前视觉结果】
                style：%s
                scene：%s
                composition：%s
                当前 imagePrompt：%s

                【修改意见】
                %s

                【重写要求】
                1. 只输出新的 imagePrompt。
                2. 优先修复：产品不相关、像泛手机、像老旧手机、主体不够大、场景太杂、平台感不足。
                3. 默认改成更稳妥的现代产品主图，不要优先生成手持通勤照。
                4. 除非 reviewer 明确要求，否则去掉多余 UI、去掉数字、去掉杂物、去掉复杂场景元素。
                5. 必须强化现代手机外观语言：modern flagship smartphone, ultra-slim symmetrical bezels, refined metal frame, matte glass back, clear contemporary camera module。
                6. 结尾必须带完整负面约束：generic phone, old iphone style, thick bezels, home button, heavy HUD, cluttered props, poster ad aesthetic.
                """.formatted(
                platform,
                getPlatformGoal(platform),
                getPlatformCompositionRules(platform),
                getPlatformSubjectRules(platform),
                getPlatformNegativeRules(platform),
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
                .system(SYSTEM_VISUAL_PROMPT_AGENT_PROMPT)
                .user(userMessage)
                .call()
                .content();
    }

    private String getPlatformGoal(String platform) {
        if (platform.contains("小红书")) {
            return "生成适合小红书封面的产品图：主体大、精致、真实、第一眼可识别，适合种草点击。";
        }
        if (platform.contains("抖音")) {
            return "生成适合抖音图文首屏的产品图：主体更近、更直接、更有停留感。";
        }
        if (platform.contains("公众号")) {
            return "生成适合公众号文章配图的可信产品图：克制、稳定、真实、像正文评测配图。";
        }
        return "生成适合中文内容平台的真实产品图。";
    }

    private String getPlatformCompositionRules(String platform) {
        if (platform.contains("小红书")) {
            return "竖版优先，主体占比 60%-75%，背景简洁，顶部或侧边可留少量标题呼吸区。";
        }
        if (platform.contains("抖音")) {
            return "竖版强优先，主体更靠前、更近，背景更干净，第一眼直接识别手机。";
        }
        if (platform.contains("公众号")) {
            return "构图更克制，可横版或稳重竖版，主体清楚但不过分贴脸，适合正文穿插。";
        }
        return "主体占比足够大，构图清楚。";
    }

    private String getPlatformSubjectRules(String platform) {
        if (platform.contains("小红书")) {
            return "优先半桌面主图或轻手势主图，少道具、少手部、少 UI，让产品本体成为封面核心。";
        }
        if (platform.contains("抖音")) {
            return "优先近景主图，产品直接顶到前景，少杂物，少复杂生活叙事。";
        }
        if (platform.contains("公众号")) {
            return "优先可信产品配图，少夸张情绪动作，少网红式摆拍。";
        }
        return "优先产品主图，场景退后。";
    }

    private String getPlatformNegativeRules(String platform) {
        if (platform.contains("小红书")) {
            return "禁止廉价电商图、禁止过多道具、禁止老旧手机外观、禁止参数浮层、禁止场景压过产品。";
        }
        if (platform.contains("抖音")) {
            return "禁止主体藏在场景里、禁止弱识别度、禁止老旧手机模板、禁止海报感。";
        }
        if (platform.contains("公众号")) {
            return "禁止封面党夸张构图、禁止过多滤镜、禁止虚假科技 HUD、禁止廉价宣传图感。";
        }
        return "禁止 generic phone、old iphone style、thick bezels、home button。";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
