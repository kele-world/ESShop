package com.w.esshop.dto;

import lombok.Data;

import java.util.List;

/**
 * 商品评价汇总
 */
@Data
public class ProductReviewVO {

    /** 平均分（保留1位小数） */
    private Double avgRating;

    /** 评价总数 */
    private Long total;

    /** 评价列表 */
    private List<Item> reviews;

    @Data
    public static class Item {
        private Long id;
        private Long userId;
        private String userName;
        private String userAvatar;
        private Integer rating;
        private String content;
        private String createTime;
        /** 交易的商品标题，让评价有上下文 */
        private String productTitle;
    }
}
