package com.spy.server.constant;

public interface UserConstant {
    /**
     * 用户登录态键
     */
    String USER_LOGIN_STATE = "userLoginState";

    /**
     * 盐值，混淆密码
     */
    String SALT = "abc_123";

    /**
     * 默认用户头像
     */
    String USER_AVATAR_URL = "https://gw.alipayobjects.com/zos/rmsportal/KDpgvguMpGfqaHPjicRK.svg";

    /**
     * 默认角色
     */
    String DEFAULT_ROLE = "user";

    /**
     * 管理员角色
     */
    String ADMIN_ROLE = "admin";

    /**
     * 店铺管理者角色
     */
    String MANAGER_ROLE = "manager";
}
