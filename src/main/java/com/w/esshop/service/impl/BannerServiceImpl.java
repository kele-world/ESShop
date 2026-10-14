package com.w.esshop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.w.esshop.entity.Banner;
import com.w.esshop.mapper.BannerMapper;
import com.w.esshop.service.BannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BannerServiceImpl implements BannerService {

    @Autowired
    private BannerMapper bannerMapper;

    @Override
    public List<Banner> listActive() {
        LocalDateTime now = LocalDateTime.now();
        return bannerMapper.selectList(new LambdaQueryWrapper<Banner>()
                .eq(Banner::getEnabled, 1)
                // start_time 为空表示立即生效，不为空则要求已到生效时间
                .and(w -> w.isNull(Banner::getStartTime).or().le(Banner::getStartTime, now))
                // end_time 为空表示永久有效，不为空则要求还没过期
                .and(w -> w.isNull(Banner::getEndTime).or().ge(Banner::getEndTime, now))
                .orderByAsc(Banner::getSort));
    }

    @Override
    public List<Banner> listAll() {
        return bannerMapper.selectList(new LambdaQueryWrapper<Banner>()
                .orderByAsc(Banner::getSort)
                .orderByDesc(Banner::getCreateTime));
    }

    @Override
    public void add(Banner banner) {
        if (banner.getEnabled() == null) {
            banner.setEnabled(1);
        }
        if (banner.getSort() == null) {
            banner.setSort(0);
        }
        banner.setCreateTime(LocalDateTime.now());
        banner.setUpdateTime(LocalDateTime.now());
        bannerMapper.insert(banner);
    }

    @Override
    public void update(Banner banner) {
        banner.setUpdateTime(LocalDateTime.now());
        bannerMapper.updateById(banner);
    }

    @Override
    public void delete(Long id) {
        bannerMapper.deleteById(id);
    }
}
