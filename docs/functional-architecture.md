# 智能饮食健康管理平台 — 功能架构图

```mermaid
graph TB
    subgraph 用户端
        A[用户]
    end

    subgraph 前端 - Vue 3 + Element Plus
        B1[认证模块<br/>登录/注册]
        B2[用户中心<br/>个人资料/家庭成员]
        B3[饮食管理<br/>饮食记录/食物库]
        B4[健康追踪<br/>运动/喝水/体重]
        B5[健康目标<br/>目标设置/达成]
        B6[症状记录<br/>身体症状]
        B7[收藏管理<br/>食物收藏]
        B8[饮食建议<br/>每日建议/报告]
    end

    subgraph 后端 - Spring Boot 2.7.18
        C1[AuthController<br/>JWT认证/限流]
        C2[UserController<br/>用户管理]
        C3[FoodController<br/>食物库230+]
        C4[DietRecordController<br/>饮食记录]
        C5[FamilyMemberController<br/>家庭成员]
        C6[HealthGoalController<br/>健康目标]
        C7[ExerciseRecordController<br/>运动记录]
        C8[WaterRecordController<br/>喝水记录]
        C9[WeightRecordController<br/>体重记录]
        C10[BodySymptomController<br/>症状记录]
        C11[FavoriteController<br/>食物收藏]
        C12[DietAdviceController<br/>饮食建议]
        C13[AdminController<br/>管理员后台]
    end

    subgraph 数据层 - MySQL 8.0
        D1[(user)]
        D2[(food<br/>230+)]
        D3[(diet_record<br/>diet_record_detail)]
        D4[(family_member)]
        D5[(health_goal)]
        D6[(exercise_record)]
        D7[(water_record)]
        D8[(weight_record)]
        D9[(body_symptom)]
        D10[(food_favorite)]
    end

    A --> B1 & B2 & B3 & B4 & B5 & B6 & B7 & B8
    B1 --> C1
    B2 --> C2 & C5
    B3 --> C3 & C4 & C11
    B4 --> C7 & C8 & C9
    B5 --> C6
    B6 --> C10
    B7 --> C11
    B8 --> C12

    C1 --> D1
    C2 --> D1
    C3 --> D2
    C4 --> D3
    C5 --> D4
    C6 --> D5
    C7 --> D6
    C8 --> D7
    C9 --> D8
    C10 --> D9
    C11 --> D10
    C12 -.-> D2 & D3
    C13 --> D1 & D2 & D3
```

## 功能模块说明

| 模块 | 功能 | 后端接口 |
|------|------|----------|
| 认证 | 登录/注册/JWT/限流 | `/auth/*` |
| 用户 | 个人资料/家庭成员 | `/users/*`, `/family/*` |
| 食物 | 食物库浏览/收藏 | `/foods/*`, `/favorites/*` |
| 饮食 | 记录每餐/营养计算 | `/diet-records/*` |
| 目标 | 健康目标设置/进度 | `/health-goals/*` |
| 运动 | 运动记录 | `/exercise-records/*` |
| 喝水 | 喝水记录 | `/water-records/*` |
| 体重 | 体重记录/趋势 | `/weight-records/*` |
| 症状 | 身体症状记录 | `/body-symptoms/*` |
| 建议 | 智能饮食建议 | `/diet-advice/*` |
| 管理 | 后台管理 | `/admin/**` |
