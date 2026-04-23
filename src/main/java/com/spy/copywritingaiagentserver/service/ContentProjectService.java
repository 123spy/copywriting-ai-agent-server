package com.spy.copywritingaiagentserver.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.spy.copywritingaiagentserver.model.domain.ContentProject;
import com.spy.copywritingaiagentserver.model.dto.contentproject.ContentProjectQueryRequest;
import com.spy.copywritingaiagentserver.model.dto.contentproject.ContentProjectUpdateRequest;

public interface ContentProjectService extends IService<ContentProject> {

    Wrapper<ContentProject> getQueryWrapper(ContentProjectQueryRequest contentProjectQueryRequest, Long userId);

    ContentProject getOwnedProject(Long projectId, Long userId);

    Boolean updateProject(ContentProjectUpdateRequest contentProjectUpdateRequest, Long userId);

    Boolean deleteProject(Long projectId, Long userId);
}
