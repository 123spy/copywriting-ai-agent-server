package com.spy.copywritingaiagentserver.model.vo;

import com.spy.copywritingaiagentserver.model.domain.ContentProject;
import com.spy.copywritingaiagentserver.model.domain.ContentResult;
import com.spy.copywritingaiagentserver.model.domain.ProjectImage;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ContentProjectDetailVO implements Serializable {

    private ContentProject contentProject;

    private List<ContentResult> contentResultList;

    private List<ProjectImage> projectImageList;

    private static final long serialVersionUID = 1L;
}
