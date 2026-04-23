package com.spy.copywritingaiagentserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spy.copywritingaiagentserver.common.ErrorCode;
import com.spy.copywritingaiagentserver.constant.CommonConstant;
import com.spy.copywritingaiagentserver.exception.BusinessException;
import com.spy.copywritingaiagentserver.mapper.ContentProjectMapper;
import com.spy.copywritingaiagentserver.model.ProjectStatus;
import com.spy.copywritingaiagentserver.model.domain.ContentProject;
import com.spy.copywritingaiagentserver.model.domain.ContentResult;
import com.spy.copywritingaiagentserver.model.domain.ProjectImage;
import com.spy.copywritingaiagentserver.model.dto.contentproject.ContentProjectQueryRequest;
import com.spy.copywritingaiagentserver.model.dto.contentproject.ContentProjectUpdateRequest;
import com.spy.copywritingaiagentserver.service.ContentProjectService;
import com.spy.copywritingaiagentserver.service.ContentResultService;
import com.spy.copywritingaiagentserver.service.ProjectImageService;
import com.spy.copywritingaiagentserver.utils.SqlUtil;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContentProjectServiceImpl extends ServiceImpl<ContentProjectMapper, ContentProject>
        implements ContentProjectService {

    @Resource
    private ContentResultService contentResultService;

    @Resource
    private ProjectImageService projectImageService;

    @Override
    public Wrapper<ContentProject> getQueryWrapper(ContentProjectQueryRequest contentProjectQueryRequest, Long userId) {
        if (contentProjectQueryRequest == null || userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        QueryWrapper<ContentProject> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);

        Long id = contentProjectQueryRequest.getId();
        Integer status = contentProjectQueryRequest.getStatus();
        String projectName = contentProjectQueryRequest.getProjectName();
        String platform = contentProjectQueryRequest.getPlatform();
        String topic = contentProjectQueryRequest.getTopic();
        String audience = contentProjectQueryRequest.getAudience();
        String tone = contentProjectQueryRequest.getTone();
        String normalizedStyle = contentProjectQueryRequest.getNormalizedStyle();
        String searchText = contentProjectQueryRequest.getSearchText();
        String sortField = contentProjectQueryRequest.getSortField();
        String sortOrder = contentProjectQueryRequest.getSortOrder();

        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(status != null, "status", status);
        queryWrapper.like(StringUtils.isNotBlank(projectName), "projectName", projectName);
        queryWrapper.eq(StringUtils.isNotBlank(platform), "platform", platform);
        queryWrapper.like(StringUtils.isNotBlank(topic), "topic", topic);
        queryWrapper.like(StringUtils.isNotBlank(audience), "audience", audience);
        queryWrapper.like(StringUtils.isNotBlank(tone), "tone", tone);
        queryWrapper.eq(StringUtils.isNotBlank(normalizedStyle), "normalizedStyle", normalizedStyle);

        if (StringUtils.isNotBlank(searchText)) {
            queryWrapper.and(wrapper -> wrapper.like("projectName", searchText)
                    .or()
                    .like("topic", searchText)
                    .or()
                    .like("audience", searchText)
                    .or()
                    .like("productInfo", searchText)
                    .or()
                    .like("requirement", searchText));
        }

        if (SqlUtil.validSortField(sortField)) {
            queryWrapper.orderBy(true, CommonConstant.SORT_ORDER_ASC.equals(sortOrder), sortField);
        } else {
            queryWrapper.orderByDesc("id");
        }
        return queryWrapper;
    }

    @Override
    public ContentProject getOwnedProject(Long projectId, Long userId) {
        if (projectId == null || projectId <= 0 || userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        ContentProject contentProject = this.getById(projectId);
        if (contentProject == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "project not found");
        }
        if (!userId.equals(contentProject.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "no permission for this project");
        }
        return contentProject;
    }

    @Override
    public Boolean updateProject(ContentProjectUpdateRequest request, Long userId) {
        if (request == null || request.getId() == null || request.getId() <= 0 || userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        ContentProject oldProject = this.getOwnedProject(request.getId(), userId);
        this.validateProjectEditable(oldProject);

        ContentProject updateProject = new ContentProject();
        updateProject.setId(request.getId());
        boolean hasUpdate = false;

        if (request.getProjectName() != null) {
            String projectName = StringUtils.trim(request.getProjectName());
            if (StringUtils.isBlank(projectName) || projectName.length() > 128) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "invalid projectName");
            }
            updateProject.setProjectName(projectName);
            hasUpdate = true;
        }

        if (request.getPlatform() != null) {
            String platform = StringUtils.trim(request.getPlatform());
            if (StringUtils.isBlank(platform) || platform.length() > 32) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "invalid platform");
            }
            updateProject.setPlatform(platform);
            hasUpdate = true;
        }

        if (request.getTopic() != null) {
            String topic = StringUtils.trim(request.getTopic());
            if (StringUtils.isBlank(topic) || topic.length() > 255) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "invalid topic");
            }
            updateProject.setTopic(topic);
            hasUpdate = true;
        }

        if (request.getAudience() != null) {
            String audience = StringUtils.trim(request.getAudience());
            if (StringUtils.length(audience) > 255) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "audience is too long");
            }
            updateProject.setAudience(audience);
            hasUpdate = true;
        }

        if (request.getTone() != null) {
            String tone = StringUtils.trim(request.getTone());
            if (StringUtils.length(tone) > 255) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "tone is too long");
            }
            updateProject.setTone(tone);
            hasUpdate = true;
        }

        if (request.getNormalizedStyle() != null) {
            String normalizedStyle = StringUtils.trim(request.getNormalizedStyle());
            if (StringUtils.length(normalizedStyle) > 64) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "normalizedStyle is too long");
            }
            updateProject.setNormalizedStyle(normalizedStyle);
            hasUpdate = true;
        }

        if (request.getProductInfo() != null) {
            updateProject.setProductInfo(StringUtils.trim(request.getProductInfo()));
            hasUpdate = true;
        }

        if (request.getRequirement() != null) {
            updateProject.setRequirement(StringUtils.trim(request.getRequirement()));
            hasUpdate = true;
        }

        if (!hasUpdate) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "no fields to update");
        }

        boolean result = this.updateById(updateProject);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "update project failed");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteProject(Long projectId, Long userId) {
        ContentProject contentProject = this.getOwnedProject(projectId, userId);
        this.validateProjectEditable(contentProject);

        List<ContentResult> contentResultList = contentResultService.list(
                new QueryWrapper<ContentResult>().eq("projectId", projectId)
        );
        if (!contentResultList.isEmpty()) {
            boolean removeContentResult = contentResultService.removeByIds(
                    contentResultList.stream().map(ContentResult::getId).toList()
            );
            if (!removeContentResult) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "delete project results failed");
            }
        }

        List<ProjectImage> projectImageList = projectImageService.list(
                new QueryWrapper<ProjectImage>().eq("projectId", projectId)
        );
        if (!projectImageList.isEmpty()) {
            boolean removeProjectImage = projectImageService.removeByIds(
                    projectImageList.stream().map(ProjectImage::getId).toList()
            );
            if (!removeProjectImage) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "delete project images failed");
            }
        }

        boolean removeProject = this.removeById(projectId);
        if (!removeProject) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "delete project failed");
        }
        return true;
    }

    private void validateProjectEditable(ContentProject contentProject) {
        if (contentProject != null && ProjectStatus.RUNNING.getCode() == contentProject.getStatus()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "running projects cannot be updated or deleted");
        }
    }
}
