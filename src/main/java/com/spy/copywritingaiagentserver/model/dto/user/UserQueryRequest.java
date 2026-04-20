package com.spy.server.model.dto.user;

import com.spy.server.common.PageRequest;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户表
 * @TableName user
 */
@Data
public class UserQueryRequest extends PageRequest implements Serializable {
    /**
     * 主键 id
     */
    private Long id;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 用户密码（加密存储）
     */
    private String userPassword;

    /**
     * 用户手机号
     */
    private String userPhone;

    /**
     * 用户头像
     */
    private String avatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/manager/admin
     */
    private String userRole;

    /**
     * 账号状态：0-正常 1-封禁
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
}