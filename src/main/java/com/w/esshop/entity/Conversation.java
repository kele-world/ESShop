package com.w.esshop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天会话
 */
@Data
@TableName("conversation")
public class Conversation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long buyerId;

    private Long sellerId;

    private Long productId;

    /** 最后一条消息预览 */
    private String lastMessage;

    private LocalDateTime lastTime;

    /** 买家未读数 */
    private Integer buyerUnread;

    /** 卖家未读数 */
    private Integer sellerUnread;

    /**
     * 买家是否从会话列表中移除了这个会话
     *
     * 不用 @TableLogic 全局删除：移除只对自己生效，
     * 对方列表里照常保留，之后对方再发消息会话会重新出现。
     */
    private Integer buyerDeleted;

    /** 卖家是否从会话列表中移除了这个会话 */
    private Integer sellerDeleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    // ---- 非数据库字段（展示用） ----
    @TableField(exist = false)
    private String productTitle;

    @TableField(exist = false)
    private String productImage;

    /** 对话对方的昵称 */
    @TableField(exist = false)
    private String peerName;

    @TableField(exist = false)
    private String peerAvatar;

    /** 当前用户在这个会话里的未读数 */
    @TableField(exist = false)
    private Integer unread;

    /** 对话对方ID（根据当前用户是买家还是卖家决定） */
    @TableField(exist = false)
    private Long peerId;
}
