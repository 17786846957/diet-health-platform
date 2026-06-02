-- 扩充表结构：健康目标、饮水记录、运动记录、身体症状记录
USE diet_health;

-- 健康目标表
CREATE TABLE IF NOT EXISTS `health_goal` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `member_id` BIGINT DEFAULT NULL COMMENT '家庭成员ID, NULL表示主用户',
  `goal_type` VARCHAR(30) NOT NULL COMMENT 'lose_weight/gain_weight/muscle/health',
  `target_weight` DECIMAL(5,1) DEFAULT NULL COMMENT '目标体重(kg)',
  `target_calories` DECIMAL(8,1) DEFAULT NULL COMMENT '每日目标热量(kcal)',
  `target_protein` DECIMAL(8,1) DEFAULT NULL COMMENT '每日目标蛋白质(g)',
  `target_fat` DECIMAL(8,1) DEFAULT NULL COMMENT '每日目标脂肪(g)',
  `target_carbs` DECIMAL(8,1) DEFAULT NULL COMMENT '每日目标碳水(g)',
  `target_water` DECIMAL(6,1) DEFAULT 2000 COMMENT '每日目标饮水量(ml)',
  `start_date` DATE NOT NULL COMMENT '开始日期',
  `end_date` DATE DEFAULT NULL COMMENT '结束日期',
  `status` VARCHAR(20) DEFAULT 'active' COMMENT 'active/completed/cancelled',
  `progress` DECIMAL(5,2) DEFAULT 0 COMMENT '完成进度(%)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_member` (`user_id`, `member_id`),
  KEY `idx_goal_status` (`user_id`, `status`),
  CONSTRAINT `fk_goal_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_goal_member` FOREIGN KEY (`member_id`) REFERENCES `family_member`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 饮水记录表
CREATE TABLE IF NOT EXISTS `water_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `member_id` BIGINT DEFAULT NULL COMMENT '家庭成员ID, NULL表示主用户',
  `record_date` DATE NOT NULL COMMENT '记录日期',
  `amount` DECIMAL(6,1) NOT NULL COMMENT '饮水量(ml)',
  `drink_type` VARCHAR(20) DEFAULT 'water' COMMENT 'water/tea/coffee/juice/milk/other',
  `record_time` TIME DEFAULT NULL COMMENT '记录时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_date` (`user_id`, `record_date`),
  CONSTRAINT `fk_water_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_water_member` FOREIGN KEY (`member_id`) REFERENCES `family_member`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 运动记录表
CREATE TABLE IF NOT EXISTS `exercise_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `member_id` BIGINT DEFAULT NULL COMMENT '家庭成员ID, NULL表示主用户',
  `record_date` DATE NOT NULL COMMENT '记录日期',
  `exercise_type` VARCHAR(50) NOT NULL COMMENT '运动类型',
  `duration` INT NOT NULL COMMENT '运动时长(分钟)',
  `calories_burned` DECIMAL(8,1) DEFAULT 0 COMMENT '消耗热量(kcal)',
  `intensity` VARCHAR(20) DEFAULT 'moderate' COMMENT 'low/moderate/high',
  `notes` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_date` (`user_id`, `record_date`),
  CONSTRAINT `fk_exercise_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_exercise_member` FOREIGN KEY (`member_id`) REFERENCES `family_member`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 身体症状记录表
CREATE TABLE IF NOT EXISTS `body_symptom` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `member_id` BIGINT DEFAULT NULL COMMENT '家庭成员ID, NULL表示主用户',
  `record_date` DATE NOT NULL COMMENT '记录日期',
  `symptom_type` VARCHAR(50) NOT NULL COMMENT '症状类型：headache/fatigue/nausea/bloating/insomnia/other',
  `severity` INT DEFAULT 3 COMMENT '严重程度1-10',
  CONSTRAINT `chk_severity` CHECK (`severity` BETWEEN 1 AND 10),
  `description` VARCHAR(500) DEFAULT NULL COMMENT '症状描述',
  `possible_cause` VARCHAR(200) DEFAULT NULL COMMENT '可能原因',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_date` (`user_id`, `record_date`),
  CONSTRAINT `fk_symptom_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_symptom_member` FOREIGN KEY (`member_id`) REFERENCES `family_member`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 体重记录表
CREATE TABLE IF NOT EXISTS `weight_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `member_id` BIGINT DEFAULT NULL COMMENT '家庭成员ID, NULL表示主用户',
  `record_date` DATE NOT NULL COMMENT '记录日期',
  `weight` DECIMAL(5,1) NOT NULL COMMENT '体重(kg)',
  `body_fat` DECIMAL(4,1) DEFAULT NULL COMMENT '体脂率(%)',
  `notes` VARCHAR(200) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`, `member_id`, `record_date`),
  CONSTRAINT `fk_weight_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_weight_member` FOREIGN KEY (`member_id`) REFERENCES `family_member`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
