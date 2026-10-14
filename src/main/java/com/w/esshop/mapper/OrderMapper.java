package com.w.esshop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.w.esshop.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单 Mapper
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}