# 智能饮食健康管理平台

基于 Spring Boot + Vue 3 的全栈饮食健康管理应用，支持食物库、饮食记录、营养统计、家庭成员管理等功能。

## 功能特性

- **食物库**：230+ 种中国常见食物，含详细营养成分
- **饮食记录**：记录每日三餐及加餐，自动计算营养摄入
- **营养统计**：可视化展示每日/每周/每月营养摄入趋势
- **饮水记录**：追踪每日饮水量，支持快捷添加
- **运动记录**：记录运动类型、时长、消耗热量
- **体重管理**：记录体重变化趋势，BMI 计算
- **健康目标**：设定减重/增肌/维持等健康目标
- **身体症状**：记录身体不适症状，分析可能原因
- **智能建议**：基于算法提供个性化饮食建议
- **家庭管理**：支持添加家庭成员，统一管理健康数据
- **管理后台**：用户管理、数据统计、食物库管理

## 技术栈

### 后端
- Java 11 + Spring Boot 2.7.18
- Spring Security + JWT 认证
- MyBatis-Plus + MySQL 8.0
- SpringDoc OpenAPI (Swagger)
- JUnit 5 + Mockito 测试

### 前端
- Vue 3 + Vite 5
- Element Plus 组件库
- Pinia 状态管理
- ECharts 图表
- Playwright E2E 测试

## 快速开始

### 环境要求
- JDK 11+
- Maven 3.6+
- Node.js 16+
- MySQL 8.0+

### 数据库初始化
```bash
# 创建数据库并导入数据
mysql -u root -p < sql/init.sql
mysql -u root -p diet_health < sql/food_data.sql
```

### 后端启动
```bash
cd diet-health-backend
mvn spring-boot:run
```
后端将在 http://localhost:8082 启动

### 前端启动
```bash
cd diet-health-frontend
npm install
npm run dev
```
前端将在 http://localhost:5173 启动

### Docker 部署
```bash
docker-compose up -d
```
访问 http://localhost

## 测试账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin  | admin123 | 管理员 |
| testuser | 123456 | 普通用户 |

## 运行测试

### 后端测试
```bash
cd diet-health-backend
mvn test
```

### 前端 E2E 测试
```bash
cd diet-health-frontend
npx playwright test
```

## 项目结构

```
bishe2/
├── diet-health-backend/     # 后端 Spring Boot 项目
│   ├── src/main/java/       # 源代码
│   │   ├── controller/      # REST 控制器
│   │   ├── service/         # 业务逻辑
│   │   ├── mapper/          # MyBatis-Plus Mapper
│   │   ├── entity/          # 实体类
│   │   ├── dto/             # 数据传输对象
│   │   ├── config/          # 配置类
│   │   └── common/          # 通用工具类
│   └── src/test/            # 测试代码
├── diet-health-frontend/    # 前端 Vue 3 项目
│   ├── src/
│   │   ├── api/             # API 调用
│   │   ├── views/           # 页面组件
│   │   ├── components/      # 通用组件
│   │   ├── stores/          # Pinia 状态
│   │   ├── router/          # 路由配置
│   │   └── utils/           # 工具函数
│   └── e2e/                 # E2E 测试
├── sql/                     # 数据库脚本
│   ├── init.sql             # 建表脚本
│   └── food_data.sql        # 食物数据
├── docker-compose.yml       # Docker 配置
└── README.md                # 项目说明
```

## 环境变量

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `DB_HOST` | localhost | 数据库主机 |
| `DB_USER` | root | 数据库用户名 |
| `DB_PASSWORD` | 123456 | 数据库密码 |
| `JWT_SECRET` | (无) | JWT 密钥（生产环境必填）|
| `SPRING_PROFILES_ACTIVE` | dev | Spring 配置文件 |

## API 文档

启动后端后访问：http://localhost:8082/swagger-ui.html

## 许可证

本项目仅用于毕业设计，不用于商业用途。
