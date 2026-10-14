package com.w.esshop.service;

import com.w.esshop.common.PageResult;
import com.w.esshop.dto.LoginDTO;
import com.w.esshop.dto.LoginVO;
import com.w.esshop.dto.RegisterDTO;
import com.w.esshop.dto.UserVO;
import com.w.esshop.entity.User;

/**
 * 用户服务接口
 */
public interface UserService {

    /** 注册 */
    void register(RegisterDTO dto);

    /** 登录，返回 Token */
    LoginVO login(LoginDTO dto);

    /** 退出登录 */
    void logout(Long userId);

    /** 获取当前用户信息 */
    UserVO getCurrentUser(Long userId);

    /** 更新用户信息 */
    void updateUser(Long userId, UserVO vo);

    /** 根据ID获取用户 */
    User getById(Long userId);

    // ==================== 管理员方法 ====================

    /** 分页查询用户(管理员) */
    PageResult<User> adminPageUsers(Long page, Long size, String keyword);

    /** 更新用户状态(管理员) */
    void adminUpdateStatus(Long id, Integer status);

    /** 统计用户总数 */
    long countUsers();
}