package com.spy.copywritingaiagentserver.controller;

import com.spy.copywritingaiagentserver.ai.AgentFlow;
import com.spy.copywritingaiagentserver.ai.model.ContentPlanResult;
import com.spy.copywritingaiagentserver.ai.model.CopywritingResult;
import com.spy.copywritingaiagentserver.ai.model.FinalPostResult;
import com.spy.copywritingaiagentserver.ai.model.RequirementParseResult;
import com.spy.copywritingaiagentserver.ai.model.ReviewResult;
import com.spy.copywritingaiagentserver.ai.model.UserRequirement;
import com.spy.copywritingaiagentserver.ai.model.VisualPromptResult;
import com.spy.copywritingaiagentserver.common.BaseResponse;
import com.spy.copywritingaiagentserver.common.ErrorCode;
import com.spy.copywritingaiagentserver.exception.BusinessException;
import com.spy.copywritingaiagentserver.model.ProjectStatus;
import com.spy.copywritingaiagentserver.model.domain.ContentProject;
import com.spy.copywritingaiagentserver.model.domain.ContentResult;
import com.spy.copywritingaiagentserver.model.domain.ProjectImage;
import com.spy.copywritingaiagentserver.model.domain.User;
import com.spy.copywritingaiagentserver.service.ContentProjectService;
import com.spy.copywritingaiagentserver.service.ContentResultService;
import com.spy.copywritingaiagentserver.service.ProjectImageService;
import com.spy.copywritingaiagentserver.service.UserService;
import com.spy.copywritingaiagentserver.utils.ResultUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @Resource
    private ProjectImageService projectImageService;

    @PostMapping("/create")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<FinalPostResult> create(@RequestBody UserRequirement userRequirement, HttpServletRequest request) {
        if (userRequirement == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "request body is null");
        }

        User loginUser = userService.getLoginUser(request);

        String platform = userRequirement.getPlatform();
        if (StringUtils.isBlank(platform)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "platform is blank");
        }

        String topic = userRequirement.getTopic();
        if (StringUtils.isBlank(topic)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "topic is blank");
        }

        String audience = userRequirement.getAudience();
        if (StringUtils.isBlank(audience)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "audience is blank");
        }

        String tone = userRequirement.getTone();
        if (StringUtils.isBlank(tone)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "tone is blank");
        }

        String productInfo = userRequirement.getProductInfo();
        if (StringUtils.isBlank(productInfo)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "product info is blank");
        }

        String requirement = userRequirement.getRequirement();
        if (StringUtils.isBlank(requirement)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "requirement is blank");
        }

        ContentProject contentProject = new ContentProject();
        contentProject.setUserId(loginUser.getId());
        contentProject.setProjectName(loginUser.getUserName() + "-" + platform + "-" + topic + "-content");
        contentProject.setPlatform(platform);
        contentProject.setTopic(topic);
        contentProject.setAudience(audience);
        contentProject.setTone(tone);
        contentProject.setNormalizedStyle(null);
        contentProject.setProductInfo(productInfo);
        contentProject.setRequirement(requirement);
        contentProject.setStatus(ProjectStatus.RUNNING.getCode());

        boolean contentProjectSaveResult = contentProjectService.save(contentProject);
        if (!contentProjectSaveResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "create project failed");
        }

        FinalPostResult finalPostResult;
        try {
            finalPostResult = agentFlow.generate(userRequirement);
            if (finalPostResult == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "generation result is null");
            }
            contentProject.setStatus(ProjectStatus.SUCCESS.getCode());
            boolean contentProjectUpdateResult = contentProjectService.updateById(contentProject);
            if (!contentProjectUpdateResult) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "update project status failed");
            }
        } catch (Exception e) {
            contentProject.setStatus(ProjectStatus.FAILED.getCode());
            contentProjectService.updateById(contentProject);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "content generation failed");
        }

        RequirementParseResult requirementParseResult = finalPostResult.getRequirementParseResult();
        ContentPlanResult contentPlanResult = finalPostResult.getContentPlanResult();
        CopywritingResult copywritingResult = finalPostResult.getCopywritingResult();
        VisualPromptResult visualPromptResult = finalPostResult.getVisualPromptResult();
        String imageUrl = finalPostResult.getImageUrl();
        ReviewResult reviewResult = finalPostResult.getReviewResult();

        ContentResult contentResult = new ContentResult();
        contentResult.setProjectId(contentProject.getId());
        contentResult.setTitle(copywritingResult.getTitle());
        contentResult.setOpeningHook(copywritingResult.getOpeningHook());
        contentResult.setBody(copywritingResult.getBody());
        contentResult.setCta(copywritingResult.getCta());
        contentResult.setImagePrompt(visualPromptResult.getImagePrompt());
        contentResult.setReviewPass(Boolean.TRUE.equals(reviewResult.getPass()) ? 1 : 0);
        contentResult.setTitleScore(BigDecimal.valueOf(reviewResult.getTitleScore()));
        contentResult.setBodyScore(BigDecimal.valueOf(reviewResult.getBodyScore()));
        contentResult.setImagePromptScore(BigDecimal.valueOf(reviewResult.getImagePromptScore()));
        contentResult.setOverallScore(BigDecimal.valueOf(reviewResult.getOverallScore()));
        contentResult.setReviewFeedback(reviewResult.getFeedback());

        boolean contentResultSaveResult = contentResultService.save(contentResult);
        if (!contentResultSaveResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "save content result failed");
        }

        if (StringUtils.isNotBlank(imageUrl)) {
            ProjectImage projectImage = new ProjectImage();
            projectImage.setProjectId(contentProject.getId());
            projectImage.setResultId(contentResult.getId());
            projectImage.setImageUrl(imageUrl);
            boolean projectImageSaveResult = projectImageService.save(projectImage);
            if (!projectImageSaveResult) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "save project image failed");
            }
        }

        return ResultUtil.success(finalPostResult);
    }
}
