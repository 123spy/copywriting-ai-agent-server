package com.spy.copywritingaiagentserver.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.spy.copywritingaiagentserver.model.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.spy.copywritingaiagentserver.model.dto.user.UserAddRequest;
import com.spy.copywritingaiagentserver.model.dto.user.UserQueryRequest;
import com.spy.copywritingaiagentserver.model.dto.user.UserUpdateMyInfoRequest;
import com.spy.copywritingaiagentserver.model.dto.user.UserUpdateRequest;
import com.spy.copywritingaiagentserver.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
* @author OUC
* @description 针对表【user(用户表)】的数据库操作Service
* @createDate 2026-04-20 13:47:26
*/
public interface UserService extends IService<User> {

    long userRegister(String userAccount, String userPassword);

    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    UserVO getUserVO(User user);

    int userLogout(HttpServletRequest request);

    User getLoginUser(HttpServletRequest request);

    Long addUser(UserAddRequest userAddRequest);

    Boolean updateUser(UserUpdateRequest userUpdateRequest);

    Wrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    List<UserVO> getUserVO(List<User> records);

    Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest);

    Boolean updateUserMyInfo(UserUpdateMyInfoRequest userUpdateMyInfoRequest);

    User getLoginUserAllowNull(HttpServletRequest request);

    User userLoginByPhone(String userPhone, HttpServletRequest request);
}
