package com.w.esshop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 私聊消息
 */
@Data
@TableName("private_message")
public class PrivateMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long conversationId;

    private Long senderId;

    private Long receiverId;

    private String content;

    /** 0-未读, 1-已读 */
    private Integer isRead;

    /**
     * 发送方是否删除了这条消息
     *
     * 注意：这里刻意不使用 @TableLogic 全局逻辑删除。
     * 聊天记录的删除是「单方隐藏」——删除只对自己生效，
     * 对方仍能看到，数据也不会从数据库中清除。
     * 因此用两个标记分别记录双方各自的删除状态。
     */
    private Integer senderDeleted;

    /** 接收方是否删除了这条消息 */
    private Integer receiverDeleted;

    private LocalDateTime createTime;

    // ---- 非数据库字段 ----
    @TableField(exist = false)
    private String senderName;

    @TableField(exist = false)
    private String senderAvatar;
}
