-- ============================================================
-- 修复：早期发布的帖子缺少分类（category_id 为 NULL）
-- ============================================================
-- 背景：category_id 字段是后期加的，之前发布的帖子该字段为空，
--       导致前端不显示分类标签。本脚本为这些帖子补齐分类。
--
-- 执行步骤：
--   1. 先跑【查看】，确认哪些帖子缺分类、应该归到哪一类
--   2. 再按需跑【修复】里的 UPDATE
-- ============================================================

USE esshop;

-- ==================== 1. 查看缺分类的帖子 ====================
SELECT
    p.id,
    p.title,
    p.user_id,
    p.status,
    p.create_time
FROM `community_post` p
WHERE p.category_id IS NULL
  AND p.deleted = 0
ORDER BY p.id;

-- ==================== 2. 查看可选的分类ID ====================
SELECT id, name, sort FROM `community_category` WHERE deleted = 0 ORDER BY sort;

-- ==================== 3. 修复（改成你要的 id） ====================
-- 下面两种方式选一种执行。

-- 方式 A：把某一条帖子归到指定分类（把 11 和 2 换成实际值）
-- 分类ID参考：1=闲置好物  2=校园生活  3=二手心得  4=求助问答  5=其他
-- UPDATE `community_post` SET `category_id` = 2 WHERE `id` = 11;

-- 方式 B：把所有缺分类的帖子统一归到「其他」
-- UPDATE `community_post`
-- SET `category_id` = (SELECT id FROM `community_category` WHERE name = '其他' LIMIT 1)
-- WHERE `category_id` IS NULL;

-- 执行完再查一次确认
-- SELECT id, title, category_id FROM `community_post` WHERE deleted = 0 ORDER BY id;
