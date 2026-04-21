package com.spy.copywritingaiagentserver.ai;

import com.spy.copywritingaiagentserver.ai.model.FinalPostResult;
import com.spy.copywritingaiagentserver.ai.model.UserRequirement;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OrchestratorTest {

    @Resource
    private AgentFlow agentFlow;

    @Test
    void generate() {
        UserRequirement userRequirement = new UserRequirement();
        userRequirement.setPlatform("小红书");
        userRequirement.setTopic("大米手机");
        userRequirement.setAudience("13-60岁人群");
        userRequirement.setTone("科技感十足");
        userRequirement.setProductInfo("这个手机十分的好，性价比高，采用的都是最新的材料，软件都是经过专家设计，性能好，待机时间久。");

        FinalPostResult result = agentFlow.generate(userRequirement);

    }
}