-- ============================================================
-- 客服知识库初始数据
-- ============================================================
-- 这 14 条问答是智能客服服务不可用时的降级兜底，
-- 正常运行时走大模型，Python 服务挂了才会用到这里。
--
-- 可重复执行：已存在同 question 的记录会跳过，不会重复插入。
--
-- 执行：mysql -u root -p esshop < migration_chat_data.sql
-- ============================================================

USE esshop;

INSERT INTO `chat_knowledge` (`category`, `question`, `keywords`, `answer`, `sort`)
SELECT * FROM (SELECT '交易' AS c, '怎么购买商品？' AS q,
    '怎么买,如何购买,下单,怎么下单,购买,我要买' AS k,
    '购买流程很简单：\n1. 在首页或商品市场找到心仪的商品\n2. 点击进入商品详情页\n3. 点击"立即购买"按钮\n4. 在"我的订单"中完成付款\n\n付款后请及时联系卖家约定交易地点。' AS a,
    1 AS s) t
WHERE NOT EXISTS (SELECT 1 FROM `chat_knowledge` WHERE `question` = '怎么购买商品？');

INSERT INTO `chat_knowledge` (`category`, `question`, `keywords`, `answer`, `sort`)
SELECT * FROM (SELECT '交易', '怎么发布商品？',
    '怎么发布,发布商品,我要卖,出售,怎么卖,发布闲置',
    '发布商品步骤：\n1. 登录后点击右上角"发布"按钮\n2. 填写商品标题、分类、售价和描述\n3. 上传商品图片（最多6张，建议拍实物图）\n4. 点击"发布"即可\n\n温馨提示：如实描述商品成色，能更快卖出哦。', 2) t
WHERE NOT EXISTS (SELECT 1 FROM `chat_knowledge` WHERE `question` = '怎么发布商品？');

INSERT INTO `chat_knowledge` (`category`, `question`, `keywords`, `answer`, `sort`)
SELECT * FROM (SELECT '订单', '订单状态是什么意思？',
    '订单状态,待付款,已付款,已完成,已取消,状态',
    '订单共有4种状态：\n• 待付款：下单成功，等待买家付款\n• 已付款：买家已付款，等待线下交易\n• 已完成：交易完成，双方确认\n• 已取消：订单已取消，商品重新上架\n\n如需取消订单，在"我的订单"中点击"取消"即可。', 3) t
WHERE NOT EXISTS (SELECT 1 FROM `chat_knowledge` WHERE `question` = '订单状态是什么意思？');

INSERT INTO `chat_knowledge` (`category`, `question`, `keywords`, `answer`, `sort`)
SELECT * FROM (SELECT '订单', '怎么取消订单？',
    '取消订单,不想买了,退款,怎么退,退货',
    '取消订单：\n进入"我的订单" → 找到对应订单 → 点击"取消"按钮。\n\n注意：\n• 待付款和已付款的订单都可以取消\n• 已完成的订单无法取消\n• 取消后商品会自动重新上架\n\n如需退款请直接与卖家协商。', 4) t
WHERE NOT EXISTS (SELECT 1 FROM `chat_knowledge` WHERE `question` = '怎么取消订单？');

INSERT INTO `chat_knowledge` (`category`, `question`, `keywords`, `answer`, `sort`)
SELECT * FROM (SELECT '账号', '怎么注册账号？',
    '注册,怎么注册,新用户,创建账号',
    '注册步骤：\n1. 点击页面右上角"注册"按钮\n2. 填写用户名（3-20个字符）和密码（6-20个字符）\n3. 选填昵称和手机号\n4. 点击"注册"完成\n\n注册后即可登录发布和购买商品。', 5) t
WHERE NOT EXISTS (SELECT 1 FROM `chat_knowledge` WHERE `question` = '怎么注册账号？');

INSERT INTO `chat_knowledge` (`category`, `question`, `keywords`, `answer`, `sort`)
SELECT * FROM (SELECT '账号', '忘记密码怎么办？',
    '忘记密码,密码忘了,找回密码,重置密码',
    '很抱歉，目前暂不支持自助找回密码。\n\n请联系平台管理员协助重置，或使用其他账号登录。\n\n建议：注册时填写手机号，便于后续找回账号。', 6) t
WHERE NOT EXISTS (SELECT 1 FROM `chat_knowledge` WHERE `question` = '忘记密码怎么办？');

INSERT INTO `chat_knowledge` (`category`, `question`, `keywords`, `answer`, `sort`)
SELECT * FROM (SELECT '账号', '怎么修改个人信息？',
    '修改资料,改昵称,换头像,个人信息,改手机号',
    '修改个人信息：\n1. 点击右上角头像 → 选择"个人中心"\n2. 可修改头像、昵称、手机号\n3. 点击"保存修改"\n\n头像支持直接上传图片。', 7) t
WHERE NOT EXISTS (SELECT 1 FROM `chat_knowledge` WHERE `question` = '怎么修改个人信息？');

INSERT INTO `chat_knowledge` (`category`, `question`, `keywords`, `answer`, `sort`)
SELECT * FROM (SELECT '交易', '怎么联系卖家？',
    '联系卖家,怎么联系,卖家联系方式,找卖家',
    '目前平台不提供在线聊天功能。\n\n建议通过以下方式联系卖家：\n• 在商品详情页查看卖家昵称\n• 下单后通过订单信息查看联系方式\n• 通过社区发帖或评论留言\n\n交易时请注意人身和财产安全，建议在校内公共场所交易。', 8) t
WHERE NOT EXISTS (SELECT 1 FROM `chat_knowledge` WHERE `question` = '怎么联系卖家？');

INSERT INTO `chat_knowledge` (`category`, `question`, `keywords`, `answer`, `sort`)
SELECT * FROM (SELECT '交易', '交易安全要注意什么？',
    '安全,被骗,诈骗,风险,注意事项',
    '校园交易安全提醒：\n\n✅ 建议在校内公共场所（如图书馆、食堂门口）交易\n✅ 当面验货后再付款\n✅ 贵重物品建议有同学陪同\n\n❌ 不要提前转账给陌生人\n❌ 不要脱离平台私下交易\n❌ 不要轻信"低价急售"等话术\n\n遇到可疑情况请及时举报。', 9) t
WHERE NOT EXISTS (SELECT 1 FROM `chat_knowledge` WHERE `question` = '交易安全要注意什么？');

INSERT INTO `chat_knowledge` (`category`, `question`, `keywords`, `answer`, `sort`)
SELECT * FROM (SELECT '社区', '怎么在社区发帖？',
    '发帖,发布文章,社区发帖,怎么发帖,写文章',
    '社区发帖步骤：\n1. 进入"社区"页面\n2. 点击右上角"发布图文"按钮\n3. 选择分类、填写标题和内容\n4. 可上传最多6张图片\n5. 点击"发布"\n\n注意：帖子需要管理员审核通过后才会展示。', 10) t
WHERE NOT EXISTS (SELECT 1 FROM `chat_knowledge` WHERE `question` = '怎么在社区发帖？');

INSERT INTO `chat_knowledge` (`category`, `question`, `keywords`, `answer`, `sort`)
SELECT * FROM (SELECT '社区', '帖子为什么要审核？',
    '审核,为什么审核,帖子没显示,审核要多久',
    '为保证社区内容质量，所有帖子需经管理员审核。\n\n• 审核通过后会在社区页面展示\n• 审核时间通常为1个工作日内\n• 如长时间未通过，可能是不符合社区规范\n\n请确保内容真实、友善、与校园生活相关。', 11) t
WHERE NOT EXISTS (SELECT 1 FROM `chat_knowledge` WHERE `question` = '帖子为什么要审核？');

INSERT INTO `chat_knowledge` (`category`, `question`, `keywords`, `answer`, `sort`)
SELECT * FROM (SELECT '商品', '怎么收藏商品？',
    '收藏,怎么收藏,我的收藏,收藏在哪',
    '收藏商品：\n在商品详情页点击"收藏"按钮即可。\n\n查看收藏：\n点击右上角头像 → "我的收藏"，可分别查看收藏的帖子和商品。', 12) t
WHERE NOT EXISTS (SELECT 1 FROM `chat_knowledge` WHERE `question` = '怎么收藏商品？');

INSERT INTO `chat_knowledge` (`category`, `question`, `keywords`, `answer`, `sort`)
SELECT * FROM (SELECT '商品', '商品下架了还能看到吗？',
    '下架,商品下架,下架还能看到,重新上架',
    '商品下架后：\n• 其他用户在商品市场看不到了\n• 在"我的发布"中仍可看到，状态显示为"已下架"\n• 已下架的商品可以重新上架\n\n如果你想买的东西被下架了，可能是卖家已售出或暂不出售。', 13) t
WHERE NOT EXISTS (SELECT 1 FROM `chat_knowledge` WHERE `question` = '商品下架了还能看到吗？');

INSERT INTO `chat_knowledge` (`category`, `question`, `keywords`, `answer`, `sort`)
SELECT * FROM (SELECT '商品', '怎么修改商品价格？',
    '改价,修改价格,价格错了,改价格',
    '修改价格：\n1. 进入"我的发布"页面\n2. 找到对应商品\n3. 点击"改价"按钮，输入新价格即可\n\n也可以点击"编辑"修改商品的其他信息。', 14) t
WHERE NOT EXISTS (SELECT 1 FROM `chat_knowledge` WHERE `question` = '怎么修改商品价格？');

-- 验证结果
SELECT COUNT(*) AS '知识库条数' FROM `chat_knowledge`;
