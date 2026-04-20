package com.spy.server.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户表
 * @TableName user
 */
@Data
public class UserLoginRequest implements Serializable {
    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 用户密码（加密存储）
     */
    private String userPassword;
}