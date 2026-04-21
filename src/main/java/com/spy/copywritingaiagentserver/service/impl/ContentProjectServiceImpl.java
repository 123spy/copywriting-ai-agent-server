package com.spy.copywritingaiagentserver.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spy.copywritingaiagentserver.model.domain.ContentProject;
import com.spy.copywritingaiagentserver.service.ContentProjectService;
import com.spy.copywritingaiagentserver.mapper.ContentProjectMapper;
import org.springframework.stereotype.Service;

/**
* @author OUC
* @description 针对表【content_project(内容创作项目表)】的数据库操作Service实现
* @createDate 2026-04-20 13:47:34
*/
@Service
public class ContentProjectServiceImpl extends ServiceImpl<ContentProjectMapper, ContentProject>
    implements ContentProjectService{

}




