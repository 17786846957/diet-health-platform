---
name: db-migrate
description: "数据库迁移和初始化管理。管理 SQL 脚本执行顺序、重建数据库、数据备份。适用于：重建数据库、初始化数据、执行 SQL 脚本、数据库迁移、数据备份恢复。当用户说'重建数据库'、'初始化数据库'、'执行 SQL'、'数据库迁移'、'备份数据'时触发。"
---

# DB Migrate — 数据库迁移管理

管理项目的 MySQL 数据库初始化和迁移。

## 数据库信息
- 数据库名：`diet_health`
- 用户名：`root`
- SQL 文件位置：`D:/bishe2/sql/`

## SQL 执行顺序（严格）

| 顺序 | 文件 | 用途 |
|------|------|------|
| 1 | `init.sql` | 建表 + 基础数据（38 种食物） |
| 2 | `food_expand.sql` | 扩展食物数据（+193 种） |
| 3 | `expand_tables.sql` | 额外表（water_record, exercise_record 等） |
| 4 | `food_data.sql` | 补充食物数据（如有） |

## 常用操作

### 完全重建数据库
```bash
mysql -u root -p -e "DROP DATABASE IF EXISTS diet_health; CREATE DATABASE diet_health DEFAULT CHARACTER SET utf8mb4;"
mysql -u root -p diet_health < D:/bishe2/sql/init.sql
mysql -u root -p diet_health < D:/bishe2/sql/food_expand.sql
mysql -u root -p diet_health < D:/bishe2/sql/expand_tables.sql
```

### 仅重建表结构（保留数据）
```bash
mysql -u root -p diet_health < D:/bishe2/sql/init.sql
```

### 添加扩展食物数据
```bash
mysql -u root -p diet_health < D:/bishe2/sql/food_expand.sql
```

### 数据备份
```bash
mysqldump -u root -p diet_health > D:/bishe2/sql/backup_$(date +%Y%m%d).sql
```

### 数据恢复
```bash
mysql -u root -p diet_health < D:/bishe2/sql/backup_20260602.sql
```

### 验证数据完整性
```bash
mysql -u root -p diet_health -e "SELECT COUNT(*) AS food_count FROM food; SELECT COUNT(*) AS user_count FROM user;"
```

## 注意事项
- 重建数据库会清除所有用户数据，操作前确认
- `init.sql` 包含 `DROP TABLE IF EXISTS`，可重复执行
- `food_expand.sql` 使用 `INSERT IGNORE`，不会重复插入
- 测试环境使用 H2 内存数据库，不受影响
