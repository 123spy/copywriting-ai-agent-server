package com.spy.server.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户表
 * @TableName user
 */
@Data
public class UserLoginByPhoneRequest implements Serializable {
    /**
     * 用户电话
     */
    private String userPhone;

    /**
     * 验证码
     */
    private String code;

}