package com.w.esshop.service;

import com.w.esshop.entity.Banner;

import java.util.List;

/**
 * 首页运营位服务
 */
public interface BannerService {

    /**
     * 获取当前生效的运营位（前台展示用）
     *
     * 过滤条件：已启用 + 在生效时间范围内
     */
    List<Banner> listActive();

    /** 全部运营位（管理后台用，含已停用和过期的） */
    List<Banner> listAll();

    void add(Banner banner);

    void update(Banner banner);

    void delete(Long id);
}
