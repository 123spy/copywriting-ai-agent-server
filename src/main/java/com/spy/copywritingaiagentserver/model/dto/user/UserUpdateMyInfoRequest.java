package com.spy.server.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户表
 *
 * @TableName user
 */
@Data
public class UserUpdateMyInfoRequest implements Serializable {
    /**
     * 主键 id
     */
    private Long id;

    /**
     * 用户名
     */
    private String userName;

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
}