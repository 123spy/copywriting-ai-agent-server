package com.spy.copywritingaiagentserver.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 内容生成结果表
 * @TableName content_result
 */
@TableName(value ="content_result")
@Data
public class ContentResult {
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
     * 标题
     */
    private String title;

    /**
     * 开头钩子
     */
    private String openingHook;

    /**
     * 正文
     */
    private String body;

    /**
     * CTA
     */
    private String cta;

    /**
     * 最终图片提示词
     */
    private String imagePrompt;

    /**
     * 是否通过评审：0-否 1-是
     */
    private Integer reviewPass;

    /**
     * 标题评分
     */
    private BigDecimal titleScore;

    /**
     * 正文评分
     */
    private BigDecimal bodyScore;

    /**
     * 图片提示词评分
     */
    private BigDecimal imagePromptScore;

    /**
     * 总体评分
     */
    private BigDecimal overallScore;

    /**
     * 评审反馈
     */
    private String reviewFeedback;

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