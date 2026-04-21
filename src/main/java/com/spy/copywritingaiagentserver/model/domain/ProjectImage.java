package com.spy.copywritingaiagentserver.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 项目图片表
 * @TableName project_image
 */
@TableName(value ="project_image")
@Data
public class ProjectImage {
    /**
     * 主键 id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 项目 id
     */
    private Long projectId;

    /**
     * 结果 id
     */
    private Long resultId;

    /**
     * 图片地址
     */
    private String imageUrl;
}