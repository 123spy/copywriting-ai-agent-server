package com.spy.copywritingaiagentserver.workflow.service;

import com.spy.copywritingaiagentserver.ai.model.UserRequirement;
import com.spy.copywritingaiagentserver.ai.model.VisualPromptResult;
import com.spy.copywritingaiagentserver.workflow.state.WorkflowStateKeys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CopyWritingWorkflowServiceTest {


    @Autowired
    private CopyWritingWorkflowService copyWritingWorkflowService;

    @Test
    void run() {
        UserRequirement userRequirement = new UserRequirement();
        userRequirement.setPlatform("小红书");
        userRequirement.setTopic("大米手机");
        userRequirement.setAudience("13-60岁人群");
        userRequirement.setTone("科技感十足");
        userRequirement.setProductInfo("这个手机十分的好，性价比高，采用的都是最新的材料，软件都是经过专家设计，性能好，待机时间久。");
        userRequirement.setRequirement("好好做，要像是商业风格一样");

        HashMap<String, Object> initState = new HashMap<>();
        initState.put(WorkflowStateKeys.USER_REQUIREMENT, userRequirement);
        Map<String, Object> result = copyWritingWorkflowService.run(initState);
        VisualPromptResult visualPromptResult = (VisualPromptResult) result.get("visualPromptResult");


        System.out.println(visualPromptResult);
    }
}