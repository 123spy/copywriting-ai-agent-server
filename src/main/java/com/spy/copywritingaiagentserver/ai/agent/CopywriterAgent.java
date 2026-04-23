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
            You are a senior copywriting specialist.
            Your job is to produce publishable copy based on the user requirement, content plan, and references.

            Tool usage rules:
            1. When the task involves brand, product, competitor, market, or platform expression details, use web search first.
            2. If search results contain clearly relevant pages, continue by reading those pages before writing.
            3. Local references can supplement the answer, but they should not be the only basis when web search is needed.
            4. If web search is not used when needed, do not output the final copy directly.
            5. If reliable sources cannot be found, say so implicitly through cautious writing and do not invent facts.

            Writing rules:
            1. Stay strictly aligned with the input requirement.
            2. Keep the brand name, topic setting, and selling points when they are already provided by the user.
            3. Do not invent unsupported specs, chips, versions, benchmarks, endorsements, or reviews.
            4. If a selling point is abstract, do not expand it into fake technical claims.
            5. If there is no clear test condition, avoid claims such as proven stability or measured results.
            6. If making comparisons, use clear comparison targets. Otherwise avoid vague competitor comparison.
            7. Keep the copy natural, credible, and restrained.
            8. Title, opening hook, body, and CTA must stay consistent.
            9. Output copy only. Do not output image prompt or planning notes.

            Output fields:
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
                Please search the web first for reliable information related to the topic, brand, product, and platform expression.
                If the search returns clearly relevant pages, read them before generating the final copy.

                [References]
                %s

                [User Requirement]
                Platform: %s
                Topic: %s
                Target audience: %s
                Tone: %s
                Selling points: %s
                Content goal: %s
                Constraints: %s

                [Content Plan]
                Content angle: %s
                Hook strategy: %s
                Core points: %s
                Structure advice: %s
                CTA strategy: %s
                Visual style advice: %s

                Writing requirements:
                1. Preserve user-provided settings, but do not expand them into unverifiable claims.
                2. If information is limited, write in a safer and more experience-based tone.
                3. Without clear test conditions, do not output hard conclusions or measured claims.
                4. Without clear comparison targets, do not write vague A vs B vs C comparisons.
                5. Return only the structured copy result and nothing else.
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
                Rewrite only the title. Do not change the body or CTA.

                [User Requirement]
                Platform: %s
                Topic: %s
                Target audience: %s
                Tone: %s

                [Content Plan]
                Content angle: %s
                Hook strategy: %s

                [Current Copy]
                Current title: %s
                Opening hook: %s
                Body: %s
                CTA: %s

                [Feedback]
                %s

                Requirements:
                1. Output only the new title.
                2. Keep it aligned with the current body.
                3. Make it more suitable for the platform and more credible.
                4. Do not add explanations.
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
                Rewrite only the CTA. Do not change the title or body.

                [User Requirement]
                Platform: %s
                Topic: %s
                Target audience: %s
                Tone: %s

                [Content Plan]
                CTA strategy: %s

                [Current Copy]
                Title: %s
                Body: %s
                Current CTA: %s

                [Feedback]
                %s

                Requirements:
                1. Output only the new CTA.
                2. Keep the CTA natural and restrained.
                3. Avoid unsupported promises or aggressive selling.
                4. Make it more suitable for platform interaction.
                5. Do not add explanations.
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
                Rewrite only the body. Do not change the title, opening hook, or CTA.

                [User Requirement]
                Platform: %s
                Topic: %s
                Target audience: %s
                Tone: %s
                Selling points: %s
                Content goal: %s
                Constraints: %s

                [Content Plan]
                Content angle: %s
                Hook strategy: %s
                Core points: %s
                Structure advice: %s

                [Current Copy]
                Title: %s
                Opening hook: %s
                Current body: %s
                CTA: %s

                [Feedback]
                %s

                Requirements:
                1. Output only the new body.
                2. Fix risky content first instead of doing broad stylistic rewrites.
                3. Do not invent specs, benchmarks, versions, chips, or endorsements.
                4. If evidence is weak, soften the claim and write it as a realistic experience.
                5. If there is no clear comparison target, remove vague comparison content.
                6. Preserve as much useful structure as possible.
                7. Do not add explanations.
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
