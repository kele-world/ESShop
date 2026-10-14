-- ==================== 校园二手交易平台数据库脚本 ====================
-- 创建数据库
CREATE DATABASE IF NOT EXISTS esshop DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE esshop;

-- ==================== 用户表 ====================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码(加密)',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色: USER-普通用户, ADMIN-管理员',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 插入默认管理员 (密码: admin123, MD5加密)
INSERT INTO `user` (`username`, `password`, `nickname`, `role`, `status`) VALUES
('admin', '0192023a7bbd73250516f069df18b500', '系统管理员', 'ADMIN', 1);

-- ==================== 商品分类表 ====================
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- 插入默认分类
INSERT INTO `category` (`name`, `sort`) VALUES
('电子产品', 1),
('书籍教材', 2),
('生活用品', 3),
('服饰鞋帽', 4),
('运动器材', 5),
('数码配件', 6),
('文具办公', 7),
('其他', 99);

-- ==================== 商品表 ====================
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    `title` VARCHAR(100) NOT NULL COMMENT '商品标题',
    `description` TEXT COMMENT '商品描述',
    `price` DECIMAL(10,2) NOT NULL COMMENT '售价',
    `original_price` DECIMAL(10,2) DEFAULT NULL COMMENT '原价',
    `images` VARCHAR(1000) DEFAULT NULL COMMENT '商品图片，多个用逗号分隔',
    `category_id` BIGINT NOT NULL COMMENT '分类ID',
    `user_id` BIGINT NOT NULL COMMENT '发布者ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ON_SALE' COMMENT '状态: ON_SALE-在售, SOLD-已售出, OFF_SHELF-已下架',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_title` (`title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- ==================== 订单表 ====================
DROP TABLE IF EXISTS `order_info`;
CREATE TABLE `order_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单编号',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `buyer_id` BIGINT NOT NULL COMMENT '买家ID',
    `seller_id` BIGINT NOT NULL COMMENT '卖家ID',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '交易金额',
    `status` VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT' COMMENT '订单状态: PENDING_PAYMENT-待付款, PAID-已付款, COMPLETED-已完成, CANCELLED-已取消',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_buyer_id` (`buyer_id`),
    KEY `idx_seller_id` (`seller_id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ==================== 社区帖子表 ====================
DROP TABLE IF EXISTS `community_post`;
CREATE TABLE `community_post` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '帖子ID',
    `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
    `user_id` BIGINT NOT NULL COMMENT '发布者ID',
    `title` VARCHAR(100) NOT NULL COMMENT '标题',
    `content` TEXT COMMENT '内容',
    `images` VARCHAR(1000) DEFAULT NULL COMMENT '图片，多个用逗号分隔',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
    `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    `comment_count` INT NOT NULL DEFAULT 0 COMMENT '评论数',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区帖子表';

-- ==================== 社区分类表 ====================
DROP TABLE IF EXISTS `community_category`;
CREATE TABLE `community_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `deleted` TINYINT NOT NULL DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区分类表';

INSERT INTO `community_category` (`name`, `sort`) VALUES
('闲置好物', 1), ('校园生活', 2), ('二手心得', 3), ('求助问答', 4), ('其他', 99);

-- ==================== 帖子点赞表 ====================
DROP TABLE IF EXISTS `post_like`;
CREATE TABLE `post_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `post_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_user` (`post_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子点赞表';

-- ==================== 用户收藏表 ====================
DROP TABLE IF EXISTS `user_favorite`;
CREATE TABLE `user_favorite` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `target_id` BIGINT NOT NULL COMMENT '收藏目标ID',
    `type` VARCHAR(10) NOT NULL COMMENT 'POST-帖子, PRODUCT-商品',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_target_type` (`user_id`, `target_id`, `type`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏表';

-- ==================== 帖子评论表 ====================
DROP TABLE IF EXISTS `post_comment`;
CREATE TABLE `post_comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `post_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `content` VARCHAR(500) NOT NULL COMMENT '评论内容',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_post_id` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子评论表';

-- ==================== 智能客服知识库表 ====================
DROP TABLE IF EXISTS `chat_knowledge`;
CREATE TABLE `chat_knowledge` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '知识ID',
    `category` VARCHAR(50) DEFAULT NULL COMMENT '分类',
    `question` VARCHAR(200) NOT NULL COMMENT '标准问题',
    `keywords` VARCHAR(500) NOT NULL COMMENT '关键词，多个用逗号分隔',
    `answer` TEXT NOT NULL COMMENT '答案',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用: 0-停用, 1-启用',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能客服知识库表';

-- 预置知识库
INSERT INTO `chat_knowledge` (`category`, `question`, `keywords`, `answer`, `sort`) VALUES
('交易', '怎么购买商品？', '怎么买,如何购买,下单,怎么下单,购买,我要买', '购买流程很简单：\n1. 在首页或商品市场找到心仪的商品\n2. 点击进入商品详情页\n3. 点击"立即购买"按钮\n4. 在"我的订单"中完成付款\n\n付款后请及时联系卖家约定交易地点。', 1),
('交易', '怎么发布商品？', '怎么发布,发布商品,我要卖,出售,怎么卖,发布闲置', '发布商品步骤：\n1. 登录后点击右上角"发布"按钮\n2. 填写商品标题、分类、售价和描述\n3. 上传商品图片（最多6张，建议拍实物图）\n4. 点击"发布"即可\n\n温馨提示：如实描述商品成色，能更快卖出哦。', 2),
('订单', '订单状态是什么意思？', '订单状态,待付款,已付款,已完成,已取消,状态', '订单共有4种状态：\n• 待付款：下单成功，等待买家付款\n• 已付款：买家已付款，等待线下交易\n• 已完成：交易完成，双方确认\n• 已取消：订单已取消，商品重新上架\n\n如需取消订单，在"我的订单"中点击"取消"即可。', 3),
('订单', '怎么取消订单？', '取消订单,不想买了,退款,怎么退,退货', '取消订单：\n进入"我的订单" → 找到对应订单 → 点击"取消"按钮。\n\n注意：\n• 待付款和已付款的订单都可以取消\n• 已完成的订单无法取消\n• 取消后商品会自动重新上架\n\n如需退款请直接与卖家协商。', 4),
('账号', '怎么注册账号？', '注册,怎么注册,新用户,创建账号', '注册步骤：\n1. 点击页面右上角"注册"按钮\n2. 填写用户名（3-20个字符）和密码（6-20个字符）\n3. 选填昵称和手机号\n4. 点击"注册"完成\n\n注册后即可登录发布和购买商品。', 5),
('账号', '忘记密码怎么办？', '忘记密码,密码忘了,找回密码,重置密码', '很抱歉，目前暂不支持自助找回密码。\n\n请联系平台管理员协助重置，或使用其他账号登录。\n\n建议：注册时填写手机号，便于后续找回账号。', 6),
('账号', '怎么修改个人信息？', '修改资料,改昵称,换头像,个人信息,改手机号', '修改个人信息：\n1. 点击右上角头像 → 选择"个人中心"\n2. 可修改头像、昵称、手机号\n3. 点击"保存修改"\n\n头像支持直接上传图片。', 7),
('交易', '怎么联系卖家？', '联系卖家,怎么联系,卖家联系方式,找卖家', '目前平台不提供在线聊天功能。\n\n建议通过以下方式联系卖家：\n• 在商品详情页查看卖家昵称\n• 下单后通过订单信息查看联系方式\n• 通过社区发帖或评论留言\n\n交易时请注意人身和财产安全，建议在校内公共场所交易。', 8),
('交易', '交易安全要注意什么？', '安全,被骗,诈骗,风险,注意事项', '校园交易安全提醒：\n\n✅ 建议在校内公共场所（如图书馆、食堂门口）交易\n✅ 当面验货后再付款\n✅ 贵重物品建议有同学陪同\n\n❌ 不要提前转账给陌生人\n❌ 不要脱离平台私下交易\n❌ 不要轻信"低价急售"等话术\n\n遇到可疑情况请及时举报。', 9),
('社区', '怎么在社区发帖？', '发帖,发布文章,社区发帖,怎么发帖,写文章', '社区发帖步骤：\n1. 进入"社区"页面\n2. 点击右上角"发布图文"按钮\n3. 选择分类、填写标题和内容\n4. 可上传最多6张图片\n5. 点击"发布"\n\n注意：帖子需要管理员审核通过后才会展示。', 10),
('社区', '帖子为什么要审核？', '审核,为什么审核,帖子没显示,审核要多久', '为保证社区内容质量，所有帖子需经管理员审核。\n\n• 审核通过后会在社区页面展示\n• 审核时间通常为1个工作日内\n• 如长时间未通过，可能是不符合社区规范\n\n请确保内容真实、友善、与校园生活相关。', 11),
('商品', '怎么收藏商品？', '收藏,怎么收藏,我的收藏,收藏在哪', '收藏商品：\n在商品详情页点击"收藏"按钮即可。\n\n查看收藏：\n点击右上角头像 → "我的收藏"，可分别查看收藏的帖子和商品。', 12),
('商品', '商品下架了还能看到吗？', '下架,商品下架,下架还能看到,重新上架', '商品下架后：\n• 其他用户在商品市场看不到了\n• 在"我的发布"中仍可看到，状态显示为"已下架"\n• 已下架的商品可以重新发布\n\n如果你想买的东西被下架了，可能是卖家已售出或暂不出售。', 13),
('商品', '怎么修改商品价格？', '改价,修改价格,价格错了,改价格', '修改价格：\n1. 进入"我的发布"页面\n2. 找到对应商品\n3. 点击"改价"按钮，输入新价格即可\n\n也可以点击"编辑"修改商品的其他信息。', 14);

-- ==================== 客服对话记录表 ====================
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID(未登录为空)',
    `role` VARCHAR(20) NOT NULL COMMENT 'USER-用户, ASSISTANT-客服',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `matched_id` BIGINT DEFAULT NULL COMMENT '命中的知识库ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服对话记录表';