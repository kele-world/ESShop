package com.w.esshop.service;

import com.w.esshop.dto.ProductReviewVO;
import com.w.esshop.dto.ReviewDTO;

/**
 * 卖家评价服务
 *
 * 二手商品一件只交易一次，评价挂在商品上没有意义。
 * 因此评价对象是「卖家」，用于建立用户信用体系。
 */
public interface ProductReviewService {

    /** 提交评价（仅已完成订单的买家可评价卖家） */
    void submit(Long userId, ReviewDTO dto);

    /** 查询某卖家收到的评价 */
    ProductReviewVO listBySeller(Long sellerId, Long page, Long size);

    /** 检查某订单是否已评价 */
    boolean hasReviewed(Long orderId);

    /** 卖家平均评分（无评价返回 0） */
    Double avgRating(Long sellerId);

    /** 卖家收到的评价总数 */
    long reviewCount(Long sellerId);
}
