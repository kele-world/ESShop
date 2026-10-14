package com.w.esshop.controller;

import com.w.esshop.common.Result;
import com.w.esshop.entity.Banner;
import com.w.esshop.service.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 首页运营位控制器（前台）
 */
@Tag(name = "运营位", description = "首页轮播内容")
@RestController
@RequestMapping("/api/banner")
public class BannerController {

    @Autowired
    private BannerService bannerService;

    @Operation(summary = "获取当前生效的运营位")
    @GetMapping("/list")
    public Result<List<Banner>> list() {
        return Result.success(bannerService.listActive());
    }
}
