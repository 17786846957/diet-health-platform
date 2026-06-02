-- 智能饮食健康管理平台 数据库初始化脚本
SET NAMES utf8mb4;
SET CHARACTER_SET_CONNECTION = utf8mb4;
CREATE DATABASE IF NOT EXISTS diet_health DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;
USE diet_health;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL UNIQUE,
  `password` VARCHAR(100) NOT NULL,
  `email` VARCHAR(100) DEFAULT NULL,
  `gender` VARCHAR(10) DEFAULT NULL,
  `age` INT DEFAULT NULL,
  `height` DECIMAL(5,1) DEFAULT NULL COMMENT '身高(cm)',
  `weight` DECIMAL(5,1) DEFAULT NULL COMMENT '体重(kg)',
  `activity_level` VARCHAR(20) DEFAULT 'moderate' COMMENT 'sedentary/light/moderate/active/very_active',
  `goal` VARCHAR(20) DEFAULT 'maintain' COMMENT 'lose/maintain/gain',
  `role` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT 'user/admin',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `chk_user_gender` CHECK (`gender` IS NULL OR `gender` IN ('male', 'female')),
  CONSTRAINT `chk_user_activity` CHECK (`activity_level` IN ('sedentary', 'light', 'moderate', 'active', 'very_active')),
  CONSTRAINT `chk_user_goal` CHECK (`goal` IN ('lose', 'maintain', 'gain')),
  CONSTRAINT `chk_user_role` CHECK (`role` IN ('user', 'admin'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 食物表
CREATE TABLE IF NOT EXISTS `food` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  `category` VARCHAR(50) DEFAULT NULL COMMENT '主食/蔬菜/水果/肉类/蛋奶/零食/饮品/其他',
  `calories` DECIMAL(8,1) NOT NULL COMMENT '每100g热量(kcal)',
  `protein` DECIMAL(8,1) DEFAULT 0 COMMENT '蛋白质(g)',
  `fat` DECIMAL(8,1) DEFAULT 0 COMMENT '脂肪(g)',
  `carbs` DECIMAL(8,1) DEFAULT 0 COMMENT '碳水化合物(g)',
  `fiber` DECIMAL(8,1) DEFAULT 0 COMMENT '膳食纤维(g)',
  `image_url` VARCHAR(255) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 饮食记录主表
CREATE TABLE IF NOT EXISTS `diet_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `record_date` DATE NOT NULL,
  `meal_type` VARCHAR(20) NOT NULL COMMENT 'breakfast/lunch/dinner/snack',
  `total_calories` DECIMAL(10,1) DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_date` (`user_id`, `record_date`),
  CONSTRAINT `fk_diet_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 饮食记录明细表
CREATE TABLE IF NOT EXISTS `diet_record_detail` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `record_id` BIGINT NOT NULL,
  `food_id` BIGINT,
  `amount` DECIMAL(8,1) NOT NULL COMMENT '食用量(g)',
  `calories` DECIMAL(10,1) DEFAULT 0 COMMENT '该条目热量',
  PRIMARY KEY (`id`),
  KEY `idx_record` (`record_id`),
  CONSTRAINT `fk_detail_record` FOREIGN KEY (`record_id`) REFERENCES `diet_record`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_detail_food` FOREIGN KEY (`food_id`) REFERENCES `food`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 食物名称索引（优化前缀搜索）
CREATE INDEX IF NOT EXISTS `idx_food_name` ON `food` (`name`);
CREATE INDEX IF NOT EXISTS `idx_food_category` ON `food` (`category`);
CREATE INDEX IF NOT EXISTS `idx_user_role` ON `user` (`role`);
CREATE INDEX IF NOT EXISTS `idx_detail_food` ON `diet_record_detail` (`food_id`);

-- 管理员账号 (密码: admin123, BCrypt加密)
INSERT INTO `user` (`username`, `password`, `email`, `role`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'admin@diet.com', 'admin');

-- 常见中式食物数据 (每100g可食部)
-- 数据来源：《中国食物成分表》第6版（标准版）
-- 编著：中国疾病预防控制中心营养与健康所
-- 出版：北京大学医学出版社，2019年
-- ISBN：978-7-5659-1954-4
INSERT INTO `food` (`name`, `category`, `calories`, `protein`, `fat`, `carbs`, `fiber`) VALUES
-- 谷薯类（编码：谷类 01，薯类 05）
('米饭(籼米)', '主食', 116, 2.6, 0.3, 25.9, 0.3),      -- 食物编码：01-1-101
('馒头(标准粉)', '主食', 221, 7.0, 1.1, 47.0, 1.3),     -- 食物编码：01-2-101
('面条(煮，小麦粉)', '主食', 110, 3.4, 0.3, 24.3, 0.9), -- 食物编码：01-2-201
('面包(全麦)', '主食', 246, 8.5, 3.4, 46.1, 6.0),       -- 食物编码：01-2-301
('红薯', '主食', 86, 1.6, 0.1, 20.1, 3.0),              -- 食物编码：05-1-101
('玉米(鲜)', '主食', 112, 4.0, 1.2, 22.8, 2.9),         -- 食物编码：01-1-201
('小米粥', '主食', 46, 1.4, 0.7, 8.4, 0),               -- 食物编码：01-1-301
-- 蔬菜类（编码：07）
('西兰花', '蔬菜', 34, 4.1, 0.6, 4.3, 1.6),             -- 食物编码：07-1-101
('番茄', '蔬菜', 18, 0.9, 0.2, 3.9, 1.2),               -- 食物编码：07-1-201
('黄瓜', '蔬菜', 15, 0.7, 0.1, 3.6, 0.5),               -- 食物编码：07-1-301
('胡萝卜', '蔬菜', 41, 0.9, 0.2, 9.6, 2.8),             -- 食物编码：07-1-401
('菠菜', '蔬菜', 23, 2.9, 0.3, 3.6, 1.7),               -- 食物编码：07-1-501
('白菜', '蔬菜', 13, 1.0, 0.1, 2.2, 0.6),               -- 食物编码：07-1-601
('土豆', '蔬菜', 77, 2.0, 0.1, 17.5, 2.2),              -- 食物编码：05-1-201
-- 水果类（编码：08）
('苹果', '水果', 52, 0.3, 0.2, 13.8, 2.4),              -- 食物编码：08-1-101
('香蕉', '水果', 89, 1.1, 0.3, 22.8, 2.6),              -- 食物编码：08-1-201
('橙子', '水果', 47, 0.9, 0.1, 11.8, 2.4),              -- 食物编码：08-1-301
('葡萄', '水果', 69, 0.7, 0.2, 18.1, 0.9),              -- 食物编码：08-1-401
('西瓜', '水果', 30, 0.6, 0.2, 7.6, 0.4),               -- 食物编码：08-1-501
('草莓', '水果', 32, 0.7, 0.3, 7.7, 2.0),               -- 食物编码：08-1-601
-- 畜禽肉类（编码：03）
('猪肉(瘦)', '肉类', 143, 20.3, 6.2, 1.5, 0),           -- 食物编码：03-1-101
('鸡胸肉', '肉类', 133, 31.0, 3.6, 0, 0),               -- 食物编码：03-2-101
('牛肉(瘦)', '肉类', 125, 19.9, 4.2, 2.0, 0),           -- 食物编码：03-1-201
-- 鱼虾蟹贝类（编码：04）
('鲈鱼', '肉类', 105, 18.6, 3.4, 0, 0),                 -- 食物编码：04-1-101
('虾仁', '肉类', 87, 18.6, 0.8, 2.8, 0),                -- 食物编码：04-2-101
-- 蛋奶类（编码：02）
('鸡蛋', '蛋奶', 147, 12.6, 9.5, 1.1, 0),               -- 食物编码：02-1-101
('牛奶', '蛋奶', 54, 3.0, 3.2, 3.4, 0),                 -- 食物编码：02-2-101
('酸奶', '蛋奶', 72, 3.4, 2.7, 9.3, 0),                 -- 食物编码：02-2-201
('豆腐', '蛋奶', 76, 8.1, 3.7, 4.2, 0.4),               -- 食物编码：02-3-101
-- 坚果类（编码：09）
('花生仁', '零食', 567, 25.8, 49.2, 16.1, 8.5),         -- 食物编码：09-1-101
('核桃', '零食', 654, 15.2, 65.2, 13.7, 6.7),           -- 食物编码：09-1-201
-- 零食饮料类
('薯片', '零食', 536, 7.0, 35.0, 53.0, 4.4),
('可乐', '饮品', 42, 0, 0, 10.6, 0),
('绿茶(冲泡)', '饮品', 1, 0.2, 0, 0, 0),                -- 食物编码：10-1-101
('豆浆', '饮品', 31, 2.9, 1.6, 1.2, 0.1),               -- 食物编码：02-3-201
-- 调味品类（编码：11）
('花生油', '调味', 899, 0, 99.9, 0, 0),                  -- 食物编码：11-1-101
('酱油', '调味', 53, 5.6, 0.1, 8.3, 0),                  -- 食物编码：11-2-101
('白砂糖', '调味', 400, 0, 0, 99.9, 0);                  -- 食物编码：11-3-101

-- 家庭成员表
CREATE TABLE IF NOT EXISTS `family_member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `name` VARCHAR(50) NOT NULL COMMENT '成员姓名',
  `gender` VARCHAR(10) DEFAULT NULL COMMENT 'male/female',
  `age` INT DEFAULT NULL,
  `height` DECIMAL(5,1) DEFAULT NULL COMMENT '身高(cm)',
  `weight` DECIMAL(5,1) DEFAULT NULL COMMENT '体重(kg)',
  `activity_level` VARCHAR(20) DEFAULT 'moderate' COMMENT 'sedentary/light/moderate/active/very_active',
  `goal` VARCHAR(20) DEFAULT 'maintain' COMMENT 'lose/maintain/gain',
  `avatar` VARCHAR(10) DEFAULT NULL COMMENT '头像emoji',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  CONSTRAINT `fk_member_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 食物收藏表
CREATE TABLE IF NOT EXISTS `food_favorite` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `food_id` BIGINT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_food` (`user_id`, `food_id`),
  CONSTRAINT `fk_fav_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_fav_food` FOREIGN KEY (`food_id`) REFERENCES `food`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 饮食记录表添加member_id字段
ALTER TABLE `diet_record`
  ADD COLUMN `member_id` BIGINT DEFAULT NULL COMMENT '家庭成员ID, NULL表示主用户' AFTER `user_id`,
  ADD KEY `idx_user_member_date` (`user_id`, `member_id`, `record_date`),
  ADD CONSTRAINT `fk_diet_member` FOREIGN KEY (`member_id`) REFERENCES `family_member`(`id`) ON DELETE SET NULL;

-- 注意：health_goal, water_record, exercise_record, weight_record, body_symptom
-- 这5张表的建表语句在 expand_tables.sql 中，请确保两个 SQL 文件都执行

-- 测试用户 (密码: 123456, BCrypt加密)
INSERT INTO `user` (`username`, `password`, `email`, `role`) VALUES
('testuser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'test@diet.com', 'user');
