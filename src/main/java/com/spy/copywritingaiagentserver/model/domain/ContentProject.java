package com.spy.copywritingaiagentserver.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 内容创作项目表
 * @TableName content_project
 */
@TableName(value ="content_project")
@Data
public class ContentProject {
    /**
     * 主键 id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 目标平台：xiaohongshu/wechat/douyin
     */
    private String platform;

    /**
     * 主题
     */
    private String topic;

    /**
     * 目标人群
     */
    private String audience;

    /**
     * 用户原始风格描述
     */
    private String tone;

    /**
     * 归一化风格标签
     */
    private String normalizedStyle;

    /**
     * 产品信息
     */
    private String productInfo;

    /**
     * 用户完整原始需求文本
     */
    private String requirement;

    /**
     * 项目状态：0:INIT/1:RUNNING/2:SUCCESS/3:FAILED
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 逻辑删除：0-未删 1-已删
     */
    private Integer isDelete;
}