CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(50) NOT NULL UNIQUE,
  `password` VARCHAR(100) NOT NULL,
  `email` VARCHAR(100),
  `gender` VARCHAR(10),
  `age` INT,
  `height` DOUBLE,
  `weight` DOUBLE,
  `activity_level` VARCHAR(20),
  `goal` VARCHAR(20),
  `role` VARCHAR(20) DEFAULT 'user',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `food` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL,
  `category` VARCHAR(50) NOT NULL,
  `calories` DOUBLE NOT NULL,
  `protein` DOUBLE,
  `fat` DOUBLE,
  `carbs` DOUBLE,
  `fiber` DOUBLE,
  `image_url` VARCHAR(255),
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `family_member` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `name` VARCHAR(50) NOT NULL,
  `gender` VARCHAR(10),
  `age` INT,
  `height` DOUBLE,
  `weight` DOUBLE,
  `activity_level` VARCHAR(20) DEFAULT 'moderate',
  `goal` VARCHAR(20) DEFAULT 'maintain',
  `avatar` VARCHAR(10),
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `diet_record` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `member_id` BIGINT,
  `record_date` DATE NOT NULL,
  `meal_type` VARCHAR(20) NOT NULL,
  `total_calories` DOUBLE,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `diet_record_detail` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `record_id` BIGINT NOT NULL,
  `food_id` BIGINT NOT NULL,
  `amount` DOUBLE,
  `calories` DOUBLE
);

CREATE TABLE IF NOT EXISTS `food_favorite` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `food_id` BIGINT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (user_id, food_id)
);

CREATE TABLE IF NOT EXISTS `water_record` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `member_id` BIGINT,
  `record_date` DATE NOT NULL,
  `amount` DECIMAL(10,2) NOT NULL,
  `drink_type` VARCHAR(50),
  `record_time` TIME,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `exercise_record` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `member_id` BIGINT,
  `record_date` DATE NOT NULL,
  `exercise_type` VARCHAR(50) NOT NULL,
  `duration` INT NOT NULL,
  `calories_burned` DECIMAL(10,2),
  `intensity` VARCHAR(20),
  `notes` VARCHAR(255),
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `weight_record` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `member_id` BIGINT,
  `record_date` DATE NOT NULL,
  `weight` DECIMAL(10,2) NOT NULL,
  `body_fat` DECIMAL(10,2),
  `notes` VARCHAR(255),
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `health_goal` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `member_id` BIGINT,
  `goal_type` VARCHAR(50) NOT NULL,
  `target_weight` DECIMAL(10,2),
  `target_calories` DECIMAL(10,2),
  `target_protein` DECIMAL(10,2),
  `target_fat` DECIMAL(10,2),
  `target_carbs` DECIMAL(10,2),
  `target_water` DECIMAL(10,2),
  `start_date` DATE,
  `end_date` DATE,
  `status` VARCHAR(20) DEFAULT 'active',
  `progress` DECIMAL(5,2) DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `body_symptom` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `member_id` BIGINT,
  `record_date` DATE NOT NULL,
  `symptom_type` VARCHAR(50) NOT NULL,
  `severity` INT,
  `description` VARCHAR(500),
  `possible_cause` VARCHAR(255),
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
);
