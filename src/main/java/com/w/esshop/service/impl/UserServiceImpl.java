package com.w.esshop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.w.esshop.common.PageResult;
import com.w.esshop.dto.LoginDTO;
import com.w.esshop.dto.LoginVO;
import com.w.esshop.dto.RegisterDTO;
import com.w.esshop.dto.UserVO;
import com.w.esshop.entity.User;
import com.w.esshop.mapper.UserMapper;
import com.w.esshop.service.UserService;
import com.w.esshop.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void register(RegisterDTO dto) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        BeanUtils.copyProperties(dto, user);
        user.setPassword(encodePassword(dto.getPassword()));
        user.setRole("USER");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        if (user.getNickname() == null || user.getNickname().isEmpty()) {
            user.setNickname(dto.getUsername());
        }
        userMapper.insert(user);
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }
        if (!matchPassword(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // Token 存入 Redis（设置过期时间）
        // 远程 Redis 抖动时不阻断登录：JWT 本身已带签名和有效期，可独立完成校验
        String redisKey = "esshop:token:" + user.getId();
        try {
            redisTemplate.opsForValue().set(redisKey, token, 86400, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Token 写入 Redis 失败，登录继续（降级为纯 JWT 校验）: {}", e.getMessage());
        }

        LoginVO vo = new LoginVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        vo.setToken(token);
        return vo;
    }

    @Override
    public void logout(Long userId) {
        String redisKey = "esshop:token:" + userId;
        try {
            redisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.warn("清除 Redis Token 失败，前端已清除本地 Token: {}", e.getMessage());
        }
    }

    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }

    @Override
    public void updateUser(Long userId, UserVO vo) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (vo.getNickname() != null) {
            user.setNickname(vo.getNickname());
        }
        if (vo.getPhone() != null) {
            user.setPhone(vo.getPhone());
        }
        if (vo.getAvatar() != null) {
            user.setAvatar(vo.getAvatar());
        }
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public User getById(Long userId) {
        return userMapper.selectById(userId);
    }

    /*
    * md5加密
    * */
    private String encodePassword(String raw) {
        return DigestUtils.md5Hex(raw);
    }

    private boolean matchPassword(String raw, String encoded) {
        return encodePassword(raw).equals(encoded);
    }

    // ==================== 管理员方法 ====================

    @Override
    public PageResult<User> adminPageUsers(Long pageNum, Long size, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(User::getUsername, keyword).or().like(User::getNickname, keyword);
        }
        wrapper.orderByDesc(User::getCreateTime);
        Page<User> p = new Page<>(pageNum, size);
        Page<User> result = userMapper.selectPage(p, wrapper);
        result.getRecords().forEach(u -> u.setPassword(null));
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public void adminUpdateStatus(Long id, Integer status) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Override
    public long countUsers() {
        return userMapper.selectCount(null);
    }
}