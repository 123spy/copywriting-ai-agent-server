package com.spy.server.utils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.spy.server.constant.UserConstant.SALT;

public class AccountUtil {


    /**
     * 手机号校验
     *
     * @param userPhone
     * @return
     */
    public static boolean checkUserPhone(String userPhone) {
        if (StringUtils.isBlank(userPhone)) {
            return false;
        }

        // 中国大陆手机号正则：11 位数字，首位 1，第二位 3-9
        String regex = "^1[3-9]\\d{9}$";
        return userPhone.matches(regex);
    }

    /**
     * 账号校验
     *
     * @param userAccount
     * @return
     */
    public static boolean checkUserAccount(String userAccount) {
        if (StringUtils.isBlank(userAccount)) {
            return false;
        }
        // 长度校验
        if (userAccount.length() < 6 || userAccount.length() > 20) {
            return false;
        }
        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            return false;
        }
        return true;
    }

    /**
     * 密码校验
     *
     * @param userPassword
     * @return
     */
    public static boolean checkUserPassword(String userPassword) {
        if (StringUtils.isBlank(userPassword)) {
            return false;
        }
        // 长度校验
        if (userPassword.length() < 5 || userPassword.length() > 20) {
            return false;
        }
        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userPassword);
        if (matcher.find()) {
            return false;
        }
        return true;
    }

    /**
     * 获取加密密码
     *
     * @param userPassword
     * @return
     */
    public static String getEncryptPassword(String userPassword) {
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        return encryptPassword;
    }

    /**
     * 获取随机用户名
     *
     * @return
     */
    public static String getRandomUserName() {
        String username = "用户" + UUID.randomUUID().toString().replace("-", "").trim().substring(0, 5);
        return username;
    }
}
