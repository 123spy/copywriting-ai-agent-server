package com.spy.copywritingaiagentserver.ai;

import com.spy.copywritingaiagentserver.ai.agent.*;
import com.spy.copywritingaiagentserver.ai.model.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RequirementAgentTest {

    @Resource
    private RequirementAgent requirementAgent;

    @Resource
    private PlannerAgent plannerAgent;

    @Resource
    private CopywriterAgent copywriterAgent;

    @Resource
    private VisualPromptAgent visualPromptAgent;

    @Resource
    private ReviewerAgent reviewerAgent;

    @Test
    void execute() {
        UserRequirement userRequirement = new UserRequirement();
        userRequirement.setPlatform("小红书");
        userRequirement.setTopic("酸奶");
        userRequirement.setAudience("13-60岁人群");
        userRequirement.setTone("轻松愉悦");
        userRequirement.setProductInfo("这个酸奶十分的好，用采用料斗十分扎实，性价比高，味道好，喝了对人也有美颜的效果");

        RequirementParseResult requirementParseResult = requirementAgent.execute(userRequirement);
        System.out.println(requirementParseResult);

        ContentPlanResult contentPlanResult = plannerAgent.execute(requirementParseResult);
        System.out.println(contentPlanResult);

        CopywritingResult copywritingResult = copywriterAgent.execute(requirementParseResult, contentPlanResult);
        System.out.println(copywritingResult);

        VisualPromptResult visualPromptResult = visualPromptAgent.execute(requirementParseResult, contentPlanResult, copywritingResult);
        System.out.println(visualPromptResult);

        ReviewResult reviewResult = reviewerAgent.execute(requirementParseResult, contentPlanResult, copywritingResult, visualPromptResult, null);
        System.out.println(reviewResult);
    }
}