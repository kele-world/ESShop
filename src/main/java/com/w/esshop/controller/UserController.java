package com.w.esshop.controller;

import com.w.esshop.common.Result;
import com.w.esshop.dto.LoginDTO;
import com.w.esshop.dto.LoginVO;
import com.w.esshop.dto.RegisterDTO;
import com.w.esshop.dto.UserVO;
import com.w.esshop.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@Tag(name = "用户模块", description = "注册、登录、个人信息管理")
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        userService.register(dto);
        return Result.success("注册成功");
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        LoginVO vo = userService.login(dto);
        return Result.success("登录成功", vo);
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        userService.logout(userId);
        return Result.success("已退出");
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public Result<UserVO> info(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(userService.getCurrentUser(userId));
    }

    @Operation(summary = "更新个人信息")
    @PutMapping("/info")
    public Result<Void> updateInfo(@RequestBody UserVO vo, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        userService.updateUser(userId, vo);
        return Result.success("更新成功");
    }
}