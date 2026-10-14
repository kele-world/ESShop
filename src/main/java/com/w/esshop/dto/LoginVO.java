package com.w.esshop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private String role;
    private String token;
}