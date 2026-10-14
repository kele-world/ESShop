package com.w.esshop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.w.esshop.entity.Conversation;
import com.w.esshop.entity.PrivateMessage;
import com.w.esshop.entity.Product;
import com.w.esshop.entity.User;
import com.w.esshop.mapper.ConversationMapper;
import com.w.esshop.mapper.PrivateMessageMapper;
import com.w.esshop.mapper.ProductMapper;
import com.w.esshop.mapper.UserMapper;
import com.w.esshop.service.ChatMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatMessageServiceImpl implements ChatMessageService {

    @Autowired
    private ConversationMapper conversationMapper;
    @Autowired
    private PrivateMessageMapper messageMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ProductMapper productMapper;

    /** 消息内容最大长度 */
    private static final int MAX_CONTENT_LENGTH = 1000;

    @Override
    @Transactional
    public Conversation getOrCreateConversation(Long userId, Long sellerId, Long productId) {
        if (userId.equals(sellerId)) {
            throw new RuntimeException("不能和自己聊天");
        }

        // productId 为 null 时，唯一索引不生效（MySQL 允许多个 NULL），
        // 所以这里手动查一次，避免产生重复会话
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getBuyerId, userId)
                .eq(Conversation::getSellerId, sellerId);
        if (productId == null) {
            wrapper.isNull(Conversation::getProductId);
        } else {
            wrapper.eq(Conversation::getProductId, productId);
        }
        Conversation exist = conversationMapper.selectOne(wrapper);
        if (exist != null) {
            return exist;
        }

        Conversation conv = new Conversation();
        conv.setBuyerId(userId);
        conv.setSellerId(sellerId);
        conv.setProductId(productId);
        conv.setBuyerUnread(0);
        conv.setSellerUnread(0);
        conv.setLastTime(LocalDateTime.now());
        conv.setCreateTime(LocalDateTime.now());
        conv.setUpdateTime(LocalDateTime.now());
        conversationMapper.insert(conv);
        return conv;
    }

    @Override
    public List<Conversation> myConversations(Long userId) {
        // 我只参与 + 我没有移除过的会话。
        // 对方是否移除与我无关，所以只过滤自己那一侧的标记。
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<Conversation>()
                .and(w -> w
                        // 我是买家：看 buyer_deleted
                        .nested(n -> n.eq(Conversation::getBuyerId, userId)
                                      .eq(Conversation::getBuyerDeleted, 0))
                        // 我是卖家：看 seller_deleted
                        .or(n -> n.eq(Conversation::getSellerId, userId)
                                  .eq(Conversation::getSellerDeleted, 0))
                )
                .orderByDesc(Conversation::getLastTime);
        List<Conversation> list = conversationMapper.selectList(wrapper);
        if (list.isEmpty()) {
            return list;
        }

        // 批量补全对方信息、商品信息、当前用户未读数
        List<Long> peerIds = list.stream()
                .map(c -> userId.equals(c.getBuyerId()) ? c.getSellerId() : c.getBuyerId())
                .distinct().toList();
        Map<Long, User> userMap = userMapper.selectBatchIds(peerIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<Long> productIds = list.stream().map(Conversation::getProductId)
                .filter(id -> id != null).distinct().toList();
        Map<Long, Product> productMap = productIds.isEmpty() ? Collections.emptyMap()
                : productMapper.selectBatchIds(productIds).stream()
                    .collect(Collectors.toMap(Product::getId, p -> p));

        list.forEach(c -> {
            boolean isBuyer = userId.equals(c.getBuyerId());
            Long peerId = isBuyer ? c.getSellerId() : c.getBuyerId();
            c.setPeerId(peerId);
            c.setUnread(isBuyer ? c.getBuyerUnread() : c.getSellerUnread());

            User peer = userMap.get(peerId);
            if (peer != null) {
                c.setPeerName(peer.getNickname());
                c.setPeerAvatar(peer.getAvatar());
            }
            Product p = productMap.get(c.getProductId());
            if (p != null) {
                c.setProductTitle(p.getTitle());
                if (p.getImages() != null && !p.getImages().isEmpty()) {
                    c.setProductImage(p.getImages().split(",")[0]);
                }
            }
        });
        return list;
    }

    @Override
    public Conversation getConversation(Long userId, Long conversationId) {
        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            throw new RuntimeException("会话不存在");
        }
        if (!isMember(userId, conversationId)) {
            throw new RuntimeException("无权查看此会话");
        }
        // 补全展示信息
        Long peerId = userId.equals(conv.getBuyerId()) ? conv.getSellerId() : conv.getBuyerId();
        conv.setPeerId(peerId);
        User peer = userMapper.selectById(peerId);
        if (peer != null) {
            conv.setPeerName(peer.getNickname());
            conv.setPeerAvatar(peer.getAvatar());
        }
        if (conv.getProductId() != null) {
            Product p = productMapper.selectById(conv.getProductId());
            if (p != null) {
                conv.setProductTitle(p.getTitle());
                if (p.getImages() != null && !p.getImages().isEmpty()) {
                    conv.setProductImage(p.getImages().split(",")[0]);
                }
            }
        }
        return conv;
    }

    @Override
    public List<PrivateMessage> history(Long userId, Long conversationId, Long beforeId, int size) {
        if (!isMember(userId, conversationId)) {
            throw new RuntimeException("无权查看此会话");
        }
        // 只过滤「我自己删掉的消息」：对方删没删与我无关，我照样能看到
        LambdaQueryWrapper<PrivateMessage> wrapper = new LambdaQueryWrapper<PrivateMessage>()
                .eq(PrivateMessage::getConversationId, conversationId)
                .and(w -> w
                        // 我发的消息：看 sender_deleted
                        .nested(n -> n.eq(PrivateMessage::getSenderId, userId)
                                      .eq(PrivateMessage::getSenderDeleted, 0))
                        // 我收到的消息：看 receiver_deleted
                        .or(n -> n.eq(PrivateMessage::getReceiverId, userId)
                                  .eq(PrivateMessage::getReceiverDeleted, 0))
                )
                .orderByDesc(PrivateMessage::getId)
                .last("LIMIT " + Math.min(size, 100));
        if (beforeId != null) {
            wrapper.lt(PrivateMessage::getId, beforeId);
        }
        List<PrivateMessage> messages = messageMapper.selectList(wrapper);

        // 填充发送者信息
        if (!messages.isEmpty()) {
            List<Long> senderIds = messages.stream().map(PrivateMessage::getSenderId).distinct().toList();
            Map<Long, User> userMap = userMapper.selectBatchIds(senderIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));
            messages.forEach(m -> {
                User u = userMap.get(m.getSenderId());
                if (u != null) {
                    m.setSenderName(u.getNickname());
                    m.setSenderAvatar(u.getAvatar());
                }
            });
        }

        // 查出来是倒序的，反转成正序（前端按时间从上到下展示）
        Collections.reverse(messages);
        return messages;
    }

    @Override
    @Transactional
    public PrivateMessage sendMessage(Long senderId, Long conversationId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("消息内容不能为空");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new RuntimeException("消息内容过长");
        }

        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            throw new RuntimeException("会话不存在");
        }
        if (!isMember(senderId, conversationId)) {
            throw new RuntimeException("无权在此会话中发言");
        }

        boolean isBuyer = senderId.equals(conv.getBuyerId());
        Long receiverId = isBuyer ? conv.getSellerId() : conv.getBuyerId();

        PrivateMessage msg = new PrivateMessage();
        msg.setConversationId(conversationId);
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setContent(content.trim());
        msg.setIsRead(0);
        msg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(msg);

        // 更新会话摘要 + 对方未读数 +1
        String preview = content.trim();
        if (preview.length() > 50) {
            preview = preview.substring(0, 50) + "...";
        }
        conv.setLastMessage(preview);
        conv.setLastTime(LocalDateTime.now());
        conv.setUpdateTime(LocalDateTime.now());

        // 对方未读数 +1
        if (isBuyer) {
            conv.setSellerUnread(nvl(conv.getSellerUnread()) + 1);
        } else {
            conv.setBuyerUnread(nvl(conv.getBuyerUnread()) + 1);
        }

        // 双方都恢复显示：发送方本就要看到，接收方之前若移除了会话，
        // 也应该因为这条新消息重新看到它出现在列表里
        conv.setBuyerDeleted(0);
        conv.setSellerDeleted(0);

        conversationMapper.updateById(conv);

        // 补上发送者信息，方便直接推送给前端
        User sender = userMapper.selectById(senderId);
        if (sender != null) {
            msg.setSenderName(sender.getNickname());
            msg.setSenderAvatar(sender.getAvatar());
        }
        return msg;
    }

    @Override
    @Transactional
    public void markRead(Long userId, Long conversationId) {
        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            throw new RuntimeException("会话不存在");
        }
        if (!isMember(userId, conversationId)) {
            throw new RuntimeException("无权操作此会话");
        }

        // 把发给我的未读消息标记为已读
        PrivateMessage update = new PrivateMessage();
        update.setIsRead(1);
        messageMapper.update(update, new LambdaQueryWrapper<PrivateMessage>()
                .eq(PrivateMessage::getConversationId, conversationId)
                .eq(PrivateMessage::getReceiverId, userId)
                .eq(PrivateMessage::getIsRead, 0));

        // 清零我的未读数
        if (userId.equals(conv.getBuyerId())) {
            conv.setBuyerUnread(0);
        } else {
            conv.setSellerUnread(0);
        }
        conversationMapper.updateById(conv);
    }

    @Override
    @Transactional
    public void deleteMessage(Long userId, Long messageId) {
        PrivateMessage msg = messageMapper.selectById(messageId);
        if (msg == null) {
            throw new RuntimeException("消息不存在");
        }
        boolean isSender = userId.equals(msg.getSenderId());
        boolean isReceiver = userId.equals(msg.getReceiverId());
        if (!isSender && !isReceiver) {
            throw new RuntimeException("无权删除此消息");
        }
        markDeleted(msg, isSender);
        messageMapper.updateById(msg);
    }

    @Override
    @Transactional
    public void clearConversation(Long userId, Long conversationId) {
        if (!isMember(userId, conversationId)) {
            throw new RuntimeException("无权操作此会话");
        }

        // 我发送的消息
        PrivateMessage senderPatch = new PrivateMessage();
        senderPatch.setSenderDeleted(1);
        messageMapper.update(senderPatch, new LambdaQueryWrapper<PrivateMessage>()
                .eq(PrivateMessage::getConversationId, conversationId)
                .eq(PrivateMessage::getSenderId, userId));

        // 我接收的消息
        PrivateMessage receiverPatch = new PrivateMessage();
        receiverPatch.setReceiverDeleted(1);
        messageMapper.update(receiverPatch, new LambdaQueryWrapper<PrivateMessage>()
                .eq(PrivateMessage::getConversationId, conversationId)
                .eq(PrivateMessage::getReceiverId, userId));

        // 会话摘要也一并清掉，列表里不再显示最后一条消息
        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv != null) {
            conv.setLastMessage(null);
            conversationMapper.updateById(conv);
        }
    }

    @Override
    @Transactional
    public void removeConversation(Long userId, Long conversationId) {
        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            throw new RuntimeException("会话不存在");
        }
        if (!isMember(userId, conversationId)) {
            throw new RuntimeException("无权操作此会话");
        }

        boolean isBuyer = userId.equals(conv.getBuyerId());

        // 1. 会话标记为已移除
        if (isBuyer) {
            conv.setBuyerDeleted(1);
            conv.setBuyerUnread(0);
        } else {
            conv.setSellerDeleted(1);
            conv.setSellerUnread(0);
        }
        conversationMapper.updateById(conv);

        // 2. 历史消息一并对我隐藏，避免重新打开时还能看到旧记录
        PrivateMessage senderPatch = new PrivateMessage();
        senderPatch.setSenderDeleted(1);
        messageMapper.update(senderPatch, new LambdaQueryWrapper<PrivateMessage>()
                .eq(PrivateMessage::getConversationId, conversationId)
                .eq(PrivateMessage::getSenderId, userId));

        PrivateMessage receiverPatch = new PrivateMessage();
        receiverPatch.setReceiverDeleted(1);
        messageMapper.update(receiverPatch, new LambdaQueryWrapper<PrivateMessage>()
                .eq(PrivateMessage::getConversationId, conversationId)
                .eq(PrivateMessage::getReceiverId, userId));
    }

    /** 按用户在会话中的角色，设置对应的一侧标记 */
    private void markDeleted(PrivateMessage msg, boolean isSender) {
        if (isSender) {
            msg.setSenderDeleted(1);
        } else {
            msg.setReceiverDeleted(1);
        }
    }

    @Override
    @Transactional
    public void sendOpeningMessageIfEmpty(Long senderId, Long conversationId, String content) {
        // 会话里已有消息就不再发送开场白，避免重复
        long count = messageMapper.selectCount(new LambdaQueryWrapper<PrivateMessage>()
                .eq(PrivateMessage::getConversationId, conversationId));
        if (count > 0) {
            return;
        }
        // 复用 sendMessage，保证未读数和会话摘要都被正确更新
        sendMessage(senderId, conversationId, content);
    }

    @Override
    public int totalUnread(Long userId) {
        List<Conversation> list = conversationMapper.selectList(
                new LambdaQueryWrapper<Conversation>()
                        .and(w -> w.eq(Conversation::getBuyerId, userId).or().eq(Conversation::getSellerId, userId)));
        int total = 0;
        for (Conversation c : list) {
            total += userId.equals(c.getBuyerId()) ? nvl(c.getBuyerUnread()) : nvl(c.getSellerUnread());
        }
        return total;
    }

    @Override
    public boolean isMember(Long userId, Long conversationId) {
        Conversation conv = conversationMapper.selectById(conversationId);
        return conv != null && (userId.equals(conv.getBuyerId()) || userId.equals(conv.getSellerId()));
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }
}
