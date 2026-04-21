package com.spy.copywritingaiagentserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spy.copywritingaiagentserver.common.ErrorCode;
import com.spy.copywritingaiagentserver.constant.CommonConstant;
import com.spy.copywritingaiagentserver.constant.UserConstant;
import com.spy.copywritingaiagentserver.exception.BusinessException;
import com.spy.copywritingaiagentserver.model.domain.User;
import com.spy.copywritingaiagentserver.model.dto.user.UserAddRequest;
import com.spy.copywritingaiagentserver.model.dto.user.UserQueryRequest;
import com.spy.copywritingaiagentserver.model.dto.user.UserUpdateMyInfoRequest;
import com.spy.copywritingaiagentserver.model.dto.user.UserUpdateRequest;
import com.spy.copywritingaiagentserver.model.vo.UserVO;
import com.spy.copywritingaiagentserver.service.UserService;
import com.spy.copywritingaiagentserver.mapper.UserMapper;
import com.spy.copywritingaiagentserver.utils.AccountUtil;
import com.spy.copywritingaiagentserver.utils.SqlUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.spy.copywritingaiagentserver.constant.UserConstant.USER_AVATAR_URL;
import static com.spy.copywritingaiagentserver.constant.UserConstant.USER_LOGIN_STATE;

/**
* @author OUC
* @description 针对表【user(用户表)】的数据库操作Service实现
* @createDate 2026-04-20 13:47:26
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Override
    public long userRegister(String userAccount, String userPassword) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (!AccountUtil.checkUserAccount(userAccount)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号非法");
        }
        if (!AccountUtil.checkUserPassword(userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码非法");
        }
        synchronized (userAccount.intern()) {
            // 账户不能重复
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userAccount", userAccount);
            long count = this.count(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
            }
            // 2. 加密
            String encryptPassword = AccountUtil.getEncryptPassword(userPassword);
//            String encryptPassword = userPassword;
            // 3. 插入数据
            User user = new User();

            user.setUserName(AccountUtil.getRandomUserName());
            user.setUserAccount(userAccount);
            user.setAvatar(USER_AVATAR_URL);
            user.setUserPassword(encryptPassword);

            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "数据添加失败");
            }
            return user.getId();
        }
    }

    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            return null;
        }
        if (!AccountUtil.checkUserAccount(userAccount)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号非法");
        }
        if (!AccountUtil.checkUserPassword(userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码非法");
        }
        // 2. 加密
        String encryptPassword = AccountUtil.getEncryptPassword(userPassword);
        // String encryptPassword = userPassword;
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        // queryWrapper.eq("userPassword", encryptPassword);
        User user = this.getOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号未注册!");
        }
        // 检测密码
        if(!StringUtils.equals(encryptPassword, user.getUserPassword())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 3. 用户脱敏
        User safetyUser = getSafetyUser(user);
        // 4. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);
        return user;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public int userLogout(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    @Override
    public Long addUser(UserAddRequest userAddRequest) {
        String userName = userAddRequest.getUserName();
        String userAccount = userAddRequest.getUserAccount();
        String userPassword = userAddRequest.getUserPassword();
        String userPhone = userAddRequest.getUserPhone();
        String avatar = userAddRequest.getAvatar();
        String userProfile = userAddRequest.getUserProfile();
        String userRole = userAddRequest.getUserRole();
        Integer status = userAddRequest.getStatus();


        User user = new User();
        user.setUserName(StringUtils.isNotBlank(userName) ? userName : AccountUtil.getRandomUserName());

        if (!AccountUtil.checkUserAccount(userAccount)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        user.setUserAccount(userAccount);

        user.setAvatar(StringUtils.isNotBlank(avatar) ? avatar : USER_AVATAR_URL);

        // 密码加密配置
        if (!AccountUtil.checkUserPassword(userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        String encryptPassword = AccountUtil.getEncryptPassword(userPassword);
        user.setUserPassword(encryptPassword);

        user.setUserPhone(userPhone);
        user.setUserProfile(userProfile);
        user.setUserRole(userRole);

        // 加锁
        synchronized (userAccount.intern()) {
            // 校验账号是否唯一
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userAccount", userAccount);
            long count = this.count(queryWrapper);
            if (count != 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已注册");
            }
            // 插入数据
            this.save(user);
            return user.getId();
        }
    }

    @Override
    public Boolean updateUser(UserUpdateRequest req) {
        if (req == null || req.getId() == null || req.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Long userId = req.getId();
        User oldUser = this.getById(userId);
        if (oldUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        User updateUser = new User();
        updateUser.setId(userId);

        // 用户名
        if (StringUtils.isNotBlank(req.getUserName())) {
            String userName = req.getUserName().trim();
            if (userName.length() > 20) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名过长");
            }
            updateUser.setUserName(userName);
        }

        // 手机号
        if (StringUtils.isNotBlank(req.getUserPhone())) {
            String userPhone = req.getUserPhone().trim();
            updateUser.setUserPhone(userPhone);
        }

        // 头像
        if (StringUtils.isNotBlank(req.getAvatar())) {
            updateUser.setAvatar(req.getAvatar().trim());
        }

        // 简介
        if (StringUtils.isNotBlank(req.getUserProfile())) {
            String userProfile = req.getUserProfile().trim();
            if (userProfile.length() > 500) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "简介过长");
            }
            updateUser.setUserProfile(userProfile);
        }

        // 角色
        if (StringUtils.isNotBlank(req.getUserRole())) {
            String userRole = req.getUserRole().trim();
            if (!UserConstant.DEFAULT_ROLE.equals(userRole)
                    && !UserConstant.ADMIN_ROLE.equals(userRole)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户角色非法");
            }
            updateUser.setUserRole(userRole);
        }

        // 状态
        if (req.getStatus() != null) {
            Integer status = req.getStatus();
            if (status < 0 || status > 1) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户状态非法");
            }
            updateUser.setStatus(status);
        }

        // 密码
        if (StringUtils.isNotBlank(req.getUserPassword())) {
            String userPassword = req.getUserPassword().trim();
            if (!AccountUtil.checkUserPassword(userPassword)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码非法");
            }
            // todo 后续需要加密一下
            // String encryptPassword = AccountUtil.getEncryptPassword(userPassword);
            updateUser.setUserPassword(userPassword);
        }

        boolean result = this.updateById(updateUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败");
        }
        return true;
    }

    @Override
    public Wrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if(userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userPhone = userQueryRequest.getUserPhone();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        Integer status = userQueryRequest.getStatus();
        String searchText = userQueryRequest.getSearchText();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(status != null, "status", status);
        queryWrapper.like(StringUtils.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StringUtils.isNotBlank(userAccount), "userAccount", userAccount);

        queryWrapper.like(StringUtils.isNotBlank(userPhone), "userPhone", userPhone);
        queryWrapper.like(StringUtils.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.eq(StringUtils.isNotBlank(userRole), "userRole", userRole);
        if (StringUtils.isNotBlank(searchText)) {
            queryWrapper.and(wrapper ->
                    wrapper.like("userName", searchText)
                            .or()
                            .like("userAccount", searchText)
                            .or()
                            .like("userProfile", searchText)
            );
        }

        queryWrapper.orderBy(
                SqlUtil.validSortField(sortField),
                CommonConstant.SORT_ORDER_ASC.equals(sortOrder),
                sortField
        );
        return queryWrapper;
    }

    @Override
    public List<UserVO> getUserVO(List<User> records) {
        if (CollectionUtils.isEmpty(records)) {
            return new ArrayList<>();
        }
        List<UserVO> userVOList = records.stream().map(user -> {
            return getUserVO(user);
        }).collect(Collectors.toList());
        return userVOList;
    }

    @Override
    public Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest) {
        int current = userQueryRequest.getCurrent();
        int pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = this.page(new Page<>(current, pageSize), this.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        List<UserVO> userVoList = this.getUserVO(userPage.getRecords());
        userVOPage.setRecords(userVoList);
        return userVOPage;
    }

    @Override
    public Boolean updateUserMyInfo(UserUpdateMyInfoRequest req) {
        if (req == null || req.getId() == null || req.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Long userId = req.getId();
        User oldUser = this.getById(userId);
        if (oldUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        User updateUser = new User();
        updateUser.setId(userId);

        // 用户名
        if (StringUtils.isNotBlank(req.getUserName())) {
            String userName = req.getUserName().trim();
            if (userName.length() > 20) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名过长");
            }
            updateUser.setUserName(userName);
        }

        // 手机号
        if (StringUtils.isNotBlank(req.getUserPhone())) {
            String userPhone = req.getUserPhone().trim();
            updateUser.setUserPhone(userPhone);
        }

        // 头像
        if (StringUtils.isNotBlank(req.getAvatar())) {
            updateUser.setAvatar(req.getAvatar().trim());
        }

        // 简介
        if (StringUtils.isNotBlank(req.getUserProfile())) {
            String userProfile = req.getUserProfile().trim();
            if (userProfile.length() > 500) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "简介过长");
            }
            updateUser.setUserProfile(userProfile);
        }

        boolean result = this.updateById(updateUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败");
        }
        return true;
    }

    @Override
    public User getLoginUserAllowNull(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            return null;
        }
        return currentUser;
    }

    @Override
    public User userLoginByPhone(String userPhone, HttpServletRequest request) {
        synchronized (userPhone.intern()) {
            // 查一下当前数据库是否存在这个手机号
            QueryWrapper<User> wrapper = new QueryWrapper<User>().eq("userPhone", userPhone);
            User user = this.getOne(wrapper);
            if (user == null) {
                // 如果没有这个手机号，就注册一个账号
                user = new User();
                user.setUserName(AccountUtil.getRandomUserName());
                user.setUserPhone(userPhone);
                this.save(user);
            }
            // 记录登陆状态
            // 3. 用户脱敏
            User safetyUser = getSafetyUser(user);
            // 4. 记录用户的登录态
            request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);
            return user;
        }

    }


    public User getSafetyUser(User originUser) {
        if (originUser == null) {
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUserName(originUser.getUserName());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatar(originUser.getAvatar());
        safetyUser.setUserProfile(originUser.getUserProfile());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setCreateTime(originUser.getCreateTime());
        safetyUser.setUpdateTime(originUser.getUpdateTime());
        return safetyUser;
    }

}




