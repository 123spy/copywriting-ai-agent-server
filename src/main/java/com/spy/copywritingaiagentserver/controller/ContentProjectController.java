package com.spy.copywritingaiagentserver.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.spy.copywritingaiagentserver.ai.AgentFlow;
import com.spy.copywritingaiagentserver.ai.model.CopywritingResult;
import com.spy.copywritingaiagentserver.ai.model.FinalPostResult;
import com.spy.copywritingaiagentserver.ai.model.ReviewResult;
import com.spy.copywritingaiagentserver.ai.model.UserRequirement;
import com.spy.copywritingaiagentserver.ai.model.VisualPromptResult;
import com.spy.copywritingaiagentserver.common.BaseResponse;
import com.spy.copywritingaiagentserver.common.DeleteRequest;
import com.spy.copywritingaiagentserver.common.ErrorCode;
import com.spy.copywritingaiagentserver.exception.BusinessException;
import com.spy.copywritingaiagentserver.model.ProjectStatus;
import com.spy.copywritingaiagentserver.model.domain.ContentProject;
import com.spy.copywritingaiagentserver.model.domain.ContentResult;
import com.spy.copywritingaiagentserver.model.domain.ProjectImage;
import com.spy.copywritingaiagentserver.model.domain.User;
import com.spy.copywritingaiagentserver.model.dto.contentproject.ContentProjectQueryRequest;
import com.spy.copywritingaiagentserver.model.dto.contentproject.ContentProjectUpdateRequest;
import com.spy.copywritingaiagentserver.model.vo.ContentProjectDetailVO;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

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
        this.validateUserRequirement(userRequirement);
        User loginUser = userService.getLoginUser(request);

        String platform = userRequirement.getPlatform();
        String topic = userRequirement.getTopic();
        String audience = userRequirement.getAudience();
        String tone = userRequirement.getTone();
        String productInfo = userRequirement.getProductInfo();
        String requirement = userRequirement.getRequirement();

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
            if (finalPostResult.getCopywritingResult() == null
                    || finalPostResult.getVisualPromptResult() == null
                    || finalPostResult.getReviewResult() == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "generation result is incomplete");
            }
            contentProject.setStatus(ProjectStatus.SUCCESS.getCode());
            boolean contentProjectUpdateResult = contentProjectService.updateById(contentProject);
            if (!contentProjectUpdateResult) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "update project status failed");
            }
        } catch (Exception e) {
            log.error("content generation failed, projectId={}", contentProject.getId(), e);
            contentProject.setStatus(ProjectStatus.FAILED.getCode());
            contentProjectService.updateById(contentProject);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "content generation failed");
        }

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
        contentResult.setTitleScore(this.toBigDecimal(reviewResult.getTitleScore()));
        contentResult.setBodyScore(this.toBigDecimal(reviewResult.getBodyScore()));
        contentResult.setImagePromptScore(this.toBigDecimal(reviewResult.getImagePromptScore()));
        contentResult.setOverallScore(this.toBigDecimal(reviewResult.getOverallScore()));
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

    @GetMapping("/get")
    public BaseResponse<ContentProject> getById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        ContentProject contentProject = contentProjectService.getOwnedProject(id, loginUser.getId());
        return ResultUtil.success(contentProject);
    }

    @GetMapping("/get/detail")
    public BaseResponse<ContentProjectDetailVO> getDetailById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        ContentProject contentProject = contentProjectService.getOwnedProject(id, loginUser.getId());
        return ResultUtil.success(this.buildContentProjectDetail(contentProject));
    }

    @PostMapping("/list/page")
    public BaseResponse<Page<ContentProject>> listProjectByPage(
            @RequestBody ContentProjectQueryRequest contentProjectQueryRequest,
            HttpServletRequest request
    ) {
        if (contentProjectQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int current = contentProjectQueryRequest.getCurrent();
        int pageSize = contentProjectQueryRequest.getPageSize();
        if (current <= 0 || pageSize <= 0 || pageSize > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "invalid pagination params");
        }

        User loginUser = userService.getLoginUser(request);
        Page<ContentProject> page = contentProjectService.page(
                new Page<>(current, pageSize),
                contentProjectService.getQueryWrapper(contentProjectQueryRequest, loginUser.getId())
        );
        return ResultUtil.success(page);
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateProject(
            @RequestBody ContentProjectUpdateRequest contentProjectUpdateRequest,
            HttpServletRequest request
    ) {
        if (contentProjectUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Boolean result = contentProjectService.updateProject(contentProjectUpdateRequest, loginUser.getId());
        return ResultUtil.success(result);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteProject(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Boolean result = contentProjectService.deleteProject(deleteRequest.getId(), loginUser.getId());
        return ResultUtil.success(result);
    }

    private void validateUserRequirement(UserRequirement userRequirement) {
        if (userRequirement == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "request body is null");
        }
        if (StringUtils.isBlank(userRequirement.getPlatform())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "platform is blank");
        }
        if (StringUtils.isBlank(userRequirement.getTopic())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "topic is blank");
        }
        if (StringUtils.isBlank(userRequirement.getAudience())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "audience is blank");
        }
        if (StringUtils.isBlank(userRequirement.getTone())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "tone is blank");
        }
        if (StringUtils.isBlank(userRequirement.getProductInfo())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "product info is blank");
        }
        if (StringUtils.isBlank(userRequirement.getRequirement())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "requirement is blank");
        }
    }

    private ContentProjectDetailVO buildContentProjectDetail(ContentProject contentProject) {
        List<ContentResult> contentResultList = contentResultService.list(
                new QueryWrapper<ContentResult>()
                        .eq("projectId", contentProject.getId())
                        .orderByDesc("id")
        );
        List<ProjectImage> projectImageList = projectImageService.list(
                new QueryWrapper<ProjectImage>()
                        .eq("projectId", contentProject.getId())
                        .orderByDesc("id")
        );

        ContentProjectDetailVO contentProjectDetailVO = new ContentProjectDetailVO();
        contentProjectDetailVO.setContentProject(contentProject);
        contentProjectDetailVO.setContentResultList(contentResultList);
        contentProjectDetailVO.setProjectImageList(projectImageList);
        return contentProjectDetailVO;
    }

    private BigDecimal toBigDecimal(Number value) {
        return value == null ? null : BigDecimal.valueOf(value.doubleValue());
    }
}
