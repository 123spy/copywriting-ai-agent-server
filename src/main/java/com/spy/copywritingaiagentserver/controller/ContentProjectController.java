package com.spy.copywritingaiagentserver.controller;

import com.spy.copywritingaiagentserver.ai.AgentFlow;
import com.spy.copywritingaiagentserver.ai.model.*;
import com.spy.copywritingaiagentserver.common.BaseResponse;
import com.spy.copywritingaiagentserver.common.ErrorCode;
import com.spy.copywritingaiagentserver.exception.BusinessException;
import com.spy.copywritingaiagentserver.model.ProjectStatus;
import com.spy.copywritingaiagentserver.model.domain.ContentProject;
import com.spy.copywritingaiagentserver.model.domain.ContentResult;
import com.spy.copywritingaiagentserver.model.domain.User;
import com.spy.copywritingaiagentserver.service.ContentProjectService;
import com.spy.copywritingaiagentserver.service.ContentResultService;
import com.spy.copywritingaiagentserver.service.UserService;
import com.spy.copywritingaiagentserver.utils.ResultUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/contentProject")
@Slf4j
public class ContentProjectController {

    @Resource
    private ContentProjectService contentProjectService;

    @Resource
    private AgentFlow agentFlow;

    @Resource
    private UserService userService;

    @Resource
    private ContentResultService contentResultService;

    @PostMapping("/create")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<FinalPostResult> create(@RequestBody UserRequirement userRequirement, HttpServletRequest request) {
        if (userRequirement == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }

        User loginUser = userService.getLoginUser(request);

        // 校验参数
        String platform = userRequirement.getPlatform();
        if (StringUtils.isBlank(platform)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "平台为空");
        }

        String topic = userRequirement.getTopic();
        if (StringUtils.isBlank(topic)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "主题为空");
        }

        String audience = userRequirement.getAudience();
        if (StringUtils.isBlank(audience)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户群体为空");
        }

        String tone = userRequirement.getTone();
        if (StringUtils.isBlank(tone)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "风格为空");
        }

        String productInfo = userRequirement.getProductInfo();
        if (StringUtils.isBlank(productInfo)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "产品信息为空");
        }

        String requirement = userRequirement.getRequirement();
        if (StringUtils.isBlank(requirement)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户需求为空");
        }

        // 存一下数据库ContentProject
        ContentProject contentProject = new ContentProject();
        contentProject.setUserId(loginUser.getId());
        contentProject.setProjectName(loginUser.getUserName() + "在" + platform + "的" + topic + "文案");
        contentProject.setPlatform(platform);
        contentProject.setTopic(topic);
        contentProject.setAudience(audience);
        contentProject.setTone(tone);
        // 初始不设置，这个会在后续生成的。
        contentProject.setNormalizedStyle(null);
        contentProject.setProductInfo(productInfo);
        contentProject.setRequirement(requirement);
        // 默认设置为0
        contentProject.setStatus(ProjectStatus.RUNNING.getCode());

        boolean contentProjectSaveResult = contentProjectService.save(contentProject);
        if (!contentProjectSaveResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "项目创建失败");
        }
        FinalPostResult finalPostResult = null;
        try {
            finalPostResult = agentFlow.generate(userRequirement);
            if (finalPostResult == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR);
            }
            contentProject.setStatus(ProjectStatus.SUCCESS.getCode());
            boolean contentProjectUpdateResult = contentProjectService.updateById(contentProject);
            if (!contentProjectUpdateResult) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR);
            }
        } catch (Exception e) {
            // 捕获异常
            contentProject.setStatus(ProjectStatus.FAILED.getCode());
            boolean contentProjectUpdateResult = contentProjectService.updateById(contentProject);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文案生成失败");
        }

        RequirementParseResult requirementParseResult = finalPostResult.getRequirementParseResult();
        ContentPlanResult contentPlanResult = finalPostResult.getContentPlanResult();
        CopywritingResult copywritingResult = finalPostResult.getCopywritingResult();
        VisualPromptResult visualPromptResult = finalPostResult.getVisualPromptResult();
        String imageUrl = finalPostResult.getImageUrl();
        ReviewResult reviewResult = finalPostResult.getReviewResult();

        // 存一下数据库ContentResult
        ContentResult contentResult = new ContentResult();
//        contentResult.setId();
        contentResult.setProjectId(contentProject.getId());

        // copywritingAgent实现这部分
        contentResult.setTitle(copywritingResult.getTitle());
        contentResult.setOpeningHook(copywritingResult.getOpeningHook());
        contentResult.setBody(copywritingResult.getBody());
        contentResult.setCta(copywritingResult.getCta());

        // visualPromptAgent
        contentResult.setImagePrompt(visualPromptResult.getImagePrompt());

        // reviewAgent
        contentResult.setReviewPass(reviewResult.getPass() ? 1 : 0);
        contentResult.setTitleScore(BigDecimal.valueOf(reviewResult.getTitleScore()));
        contentResult.setBodyScore(BigDecimal.valueOf(reviewResult.getBodyScore()));
        contentResult.setImagePromptScore(BigDecimal.valueOf(reviewResult.getImagePromptScore()));
        contentResult.setOverallScore(BigDecimal.valueOf(reviewResult.getOverallScore()));
        contentResult.setReviewFeedback(reviewResult.getFeedback());
        boolean contentResultSaveResult = contentResultService.save(contentResult);
        if(!contentResultSaveResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "结果保存失败");
        }
        return ResultUtil.success(finalPostResult);
    }

}
