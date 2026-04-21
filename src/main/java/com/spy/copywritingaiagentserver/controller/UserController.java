package com.spy.copywritingaiagentserver.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.spy.copywritingaiagentserver.annotation.AuthCheck;
import com.spy.copywritingaiagentserver.common.BaseResponse;
import com.spy.copywritingaiagentserver.common.DeleteRequest;
import com.spy.copywritingaiagentserver.common.ErrorCode;
import com.spy.copywritingaiagentserver.constant.UserConstant;
import com.spy.copywritingaiagentserver.exception.BusinessException;
import com.spy.copywritingaiagentserver.model.domain.User;
import com.spy.copywritingaiagentserver.model.dto.user.*;
import com.spy.copywritingaiagentserver.model.vo.UserVO;
import com.spy.copywritingaiagentserver.service.UserService;
import com.spy.copywritingaiagentserver.utils.ResultUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;


    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest, HttpServletRequest request) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();

        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long result = userService.userRegister(userAccount, userPassword);
        log.info("用户注册成功：用户账号={}，用户ID={}", userAccount, result);
        return ResultUtil.success(result);
    }

    @PostMapping("/login")
    public BaseResponse<UserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();

        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.userLogin(userAccount, userPassword, request);
        log.info("账号密码登录成功：用户账号={}，用户ID={}", userAccount, user.getId());
        UserVO userVO = userService.getUserVO(user);
        return ResultUtil.success(userVO);
    }

    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUserAllowNull(request);
        int result = userService.userLogout(request);
        log.info("用户退出登录成功：用户ID={}", loginUser == null ? null : loginUser.getId());
        return ResultUtil.success(result);
    }

    @GetMapping("/get/login")
    public BaseResponse<UserVO> getLoginUser(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtil.success(userService.getUserVO(user));
    }

    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        if (userAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = userService.addUser(userAddRequest);
        return ResultUtil.success(id);
    }

    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.removeById(deleteRequest.getId());
        if (!result) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtil.success(true);
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest, HttpServletRequest request) {
        if (userUpdateRequest == null || userUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User oldUser = userService.getById(userUpdateRequest.getId());
        if (oldUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Boolean result = userService.updateUser(userUpdateRequest);
        return ResultUtil.success(result);
    }

    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtil.success(user);
    }

    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtil.success(userService.getUserVO(user));
    }

    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<User>> listUserByPage(@RequestBody UserQueryRequest userQueryRequest, HttpServletRequest request) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int current = userQueryRequest.getCurrent();
        int pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, pageSize), userService.getQueryWrapper(userQueryRequest));
        return ResultUtil.success(userPage);
    }

    @PostMapping("/list/page/vo")
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest, HttpServletRequest request) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Page<UserVO> userVOPage = userService.listUserVOByPage(userQueryRequest);
        return ResultUtil.success(userVOPage);
    }

    @PostMapping("/edit")
    public BaseResponse<Boolean> editUserInfo(@RequestBody UserUpdateMyInfoRequest userUpdateMyInfoRequest, HttpServletRequest request) {
        if (userUpdateMyInfoRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        userUpdateMyInfoRequest.setId(loginUser.getId());
        Boolean result = userService.updateUserMyInfo(userUpdateMyInfoRequest);
        return ResultUtil.success(result);
    }
}
