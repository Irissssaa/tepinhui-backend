# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

**项目名称**：特品汇后端（tepinhui-backend）
**技术栈**：Spring Boot 3 + MyBatis-Plus + Redis + Swagger + Spring Security + JWT
**目标**：特产电商平台后端，支持溯源码查询、特产地图可视化、购物车与订单流转

## 分支管理

| 分支 | 负责人 | 功能模块 |
|---|---|---|
| `dev-zyx` | 张圯炫 | 地图接口 + 溯源模块 + 商家审核 + 特产文化 |
| `dev-njmd` | 乃吉木丁 | 用户认证 + 商品CRUD + 购物车 + 订单流转 |

### 开发流程（严格遵守）

```bash
git checkout main
git pull origin main
git checkout dev-zyx  # 或 dev-njmd
git merge main
...开发...
git add . 
git commit -m "xxx"
git push origin dev-zyx  # 或 dev-njmd
# 开发完成后在 GitHub 提 PR：dev-zyx → main 或 dev-njmd → main
```

## Git 提交规范

| 类型 | 示例 |
|---|---|
| `feat(trace)` | feat(trace): 添加溯源查询接口及Redis缓存 |
| `feat(user)` | feat(user): 添加用户注册登录接口 |
| `feat(order)` | feat(order): 添加订单创建与状态流转 |
| `feat(product)` | feat(product): 添加商品CRUD接口 |
| `feat(cart)` | feat(cart): 添加购物车逻辑接口 |
| `feat(specialty)` | feat(specialty): 添加特产分布地图接口 |
| `feat(merchant)` | feat(merchant): 添加商家审核管理接口 |
| `perf(cache)` | perf(cache): 溯源查询接口集成24小时Redis缓存 |
| `docs(swagger)` | docs(swagger): 更新溯源模块接口文档 |
| `docs` | docs: 文档更新 |

## API 路径规范

### 路径前缀配置

项目**使用** `server.servlet.context-path` 作为全局路径前缀，通过环境变量 `APP_API_PREFIX` 配置（默认 `/tph`）。

**重要规范：**
- Controller 中**不要**手动添加 `/tph` 前缀，`context-path` 会自动添加
- 示例：`@RequestMapping("/api/v1/trace")` → 实际路径：`/tph/api/v1/trace`

**正确写法：**
```java
@RequestMapping("/api/v1/trace")  // ✅ 实际路径：/tph/api/v1/trace
```

**错误写法：**
```java
@RequestMapping("/tph/api/v1/trace")  // ❌ 会导致路径重复：/tph/tph/api/v1/trace
```

### 实际路径格式

| 组件 | 实际路径 |
|---|---|
| 健康检查 | `/tph/health` |
| 认证接口 | `/tph/auth` |
| 业务接口 | `/tph/api/v1/*` |
| Swagger UI | `http://localhost:8060/swagger-ui.html` |
| API 文档 | `http://localhost:8060/tph/api-docs` |

### Swagger UI 路径配置

```properties
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
```

**Swagger 显示完整路径的关键：** 在 SwaggerConfig 中配置 `servers`：
```java
.servers(List.of(
    new Server().url(apiPrefix).description("服务地址")
))
```
这样 Swagger UI 中每个接口都会显示完整的前缀路径（如 `/tph/api/v1/trace`），且修改环境变量后自动适配。

### 模块路径规范

| 模块 | 路径 | 负责人 |
|---|---|---|
| 用户认证 | `/auth` | 乃吉木丁 |
| 用户信息 | `/user` | 乃吉木丁 |
| 商品管理 | `/products` | 乃吉木丁 |
| 购物车 | `/cart` | 乃吉木丁 |
| 订单管理 | `/orders` | 乃吉木丁 |
| 商家入驻 | `/merchant` | 乃吉木丁 |
| 溯源模块 | `/trace` | 张圯炫 |
| 特产模块 | `/specialties` | 张圯炫 |
| 地图模块 | `/map` | 张圯炫 |
| 统计模块 | `/stats` | 张圯炫 |
| 管理后台 | `/admin` | 张圯炫 |

## 数据库表规范（13张核心表）

### 表清单

| 表名 | 说明 | 负责人 |
|---|---|---|
| `user` | 用户表 | 乃吉木丁 |
| `merchant` | 商家表 | 张圯炫 |
| `origin` | 产地表 | 张圯炫 |
| `specialty` | 特产表 | 张圯炫 |
| `product` | 商品表 | 乃吉木丁 |
| `orders` | 订单表 | 乃吉木丁 |
| `order_item` | 订单明细表 | 乃吉木丁 |
| `cart_item` | 购物车表 | 乃吉木丁 |
| `address` | 收货地址表 | 乃吉木丁 |
| `review` | 商品评价表 | 乃吉木丁 |
| `category` | 商品分类表 | 张圯炫 |
| `trace_record` | 溯源记录表 | 张圯炫 |
| `culture_content` | 特产文化内容表 | 张圯炫 |

### 关键字段规范

- **ID**：统一使用 `BIGINT` 类型作为主键
- **时间字段**：统一为 `created_at`、`updated_at`，类型 `DATETIME`
- **状态字段**：使用 `ENUM` 类型，如 `status`、`audit_status`
- **价格字段**：使用 `DECIMAL(10,2)`
- **坐标字段**：使用 `DECIMAL(10,6)` 存储经纬度

## 缓存策略（Redis）

### 缓存Key规范

| 缓存Key | TTL | 用途 | 负责人 |
|---|---|---|---|
| `trace:{traceCode}` | 24小时 | 溯源查询结果 | 张圯炫 |
| `trace:stats` | 1小时 | 溯源统计数据 | 张圯炫 |
| `stats:specialty:map` | 2小时 | 全国特产分布地图 | 张圯炫 |
| `stats:sales:region:{region}` | 1小时 | 地区销售数据 | 张圯炫 |
| `stats:trace:region:{region}` | 1小时 | 地区溯源数据 | 张圯炫 |
| `product:list:{page}` | 5分钟 | 商品列表 | 乃吉木丁 |
| `product:detail:{id}` | 10分钟 | 商品详情 | 乃吉木丁 |

## 溯源码规范

### 命名规则

```
TP-{省份缩写}-{品类缩写}-{年份}-{6位随机数}
```

**示例**：`TP-ZJ-LJ-2024-042138`

| 字段 | 含义 | 说明 |
|---|---|---|
| `TP` | 前缀 | 固定为 TP |
| `{省份缩写}` | 省份 | 如北京→BJ、浙江→ZJ、江苏→JS |
| `{品类缩写}` | 品类 | 如龙井茶→LJ、碧螺春→BLC、铁观音→TG |
| `{年份}` | 生产年份 | 四位年份 |
| `{6位随机数}` | 随机批次号 | 6位数字，保证唯一性 |

## 溯源查询链路（6步）

```
1. 消费者触发查询（扫描二维码/手动输入溯源码）
   ↓
2. 后端优先查 Redis 缓存（key: trace:{traceCode}）
   ↓（未命中）
3. 查询 MySQL：trace_record WHERE trace_code = ? AND audit_status = 'pass'
   ↓
4. 联查 product、merchant 表，组装完整数据
   ↓
5. 写入 Redis 缓存（TTL=24h），返回前端
   ↓
6. 前端按六层结构渲染：产品 → 产地 → 生产 → 加工 → 质检 → 流通
```

### 溯源查询响应结构（6层嵌套）

| 层级 | 字段 |
|---|---|
| `traceCode` | 溯源码 |
| `product` | name, spec, merchantName, coverImg |
| `origin` | province, city, county, address, longitude, latitude |
| `production` | produceDate, producer, rawMaterial |
| `process` | factory, processDate, processDesc |
| `inspection` | org, inspectDate, result, reportUrl |
| `logistics` | warehouseIn, warehouseOut, logisticsInfo |

## 接口权限规范

| 权限级别 | 标识 | 说明 |
|---|---|---|
| 公开 | 公开 | 无需登录 |
| 消费者 | 消费者 | 已登录普通用户 |
| 商家 | 商家 | 已认证商家 |
| 管理员 | 管理员 | 平台管理员 |

## 关键约束（必须遵守）

### 功能约束

1. **Swagger 文档**：所有接口必须添加 `@Operation` 标注，这是前后端协作的唯一标准
2. **缓存一致性**：修改数据时必须清理相关 Redis 缓存
3. **鉴权拦截**：Spring Security 必须准确拦截，未登录用户不能调用下单等需要认证的接口
4. **溯源查询**：必须按 `trace_solution.html` 的六层结构实现
5. **数据库操作**：MyBatis-Plus 注意 SQL 效率，特别是关联查询

### 业务约束

1. **溯源码生成**：必须保证唯一性，遵循命名规范
2. **审核状态**：溯源记录（pending/pass/reject）、商家（pending/approved/rejected）
3. **订单流转**：状态必须是单向流动（pending → paid → shipped → done → cancelled）
4. **价格精度**：所有金额字段使用 `DECIMAL(10,2)`，避免浮点数精度问题
5. **地理位置**：经纬度使用 `DECIMAL(10,6)` 存储，支持地图可视化

## 常用构建命令

```bash
# Maven 构建
mvn clean install

# 运行 Spring Boot 应用
mvn spring-boot:run

# 测试单个接口
mvn test -Dtest=ClassName#methodName

# 代码规范检查
mvn checkstyle:check
```

## 项目结构约定

```
src/
├── main/
│   ├── java/
│   │   └── com/tepinhui/
│   │       ├── controller/     # 控制器层
│   │       ├── service/        # 业务逻辑层
│   │       ├── mapper/         # 数据访问层
│   │       ├── entity/         # 实体类
│   │       ├── dto/            # 数据传输对象
│   │       ├── vo/             # 视图对象（响应格式）
│   │       ├── config/         # 配置类
│   │       └── util/           # 工具类
│   └── resources/
│       ├── mapper/             # MyBatis XML 映射
│       └── application.yml     # 应用配置
└── test/
```

## 开发注意事项

### 张圯炫（dev-zyx）
- 地图接口响应速度必须保证（Redis 缓存，TTL=2h）
- 溯源接口严格按 `trace_solution.html` 实现六层结构
- 所有统计接口需考虑数据聚合和性能优化
- Swagger 文档必须第一时间更新

### 乃吉木丁（dev-njmd）
- 用户-商品-订单表关联多，MyBatis-Plus 注意 SQL 效率
- 鉴权必须拦截准确，未登录用户不能调用下单接口
- 密码必须使用 BCrypt 加密
- JWT Token 过期和刷新策略需设计好

## 技术规范

### 代码风格
- Java 17+
- 遵循 Google Java Style Guide
- 使用 Lombok 简化代码
- 异常处理统一使用全局异常处理器

### 数据库
- MySQL 8.0
- 使用 MyBatis-Plus 进行 ORM 操作
- 所有表必须包含 `created_at`、`updated_at` 字段
- 使用 `ENUM` 类型存储状态值

### 缓存
- Redis 缓存使用 Spring Cache 或直接操作 RedisTemplate
- 所有缓存 Key 必须有明确的 TTL
- 修改数据时必须清理相关缓存

### 文档
- 所有接口必须在 Swagger 中有完整标注
- 文档包括：接口路径、方法、参数说明、响应示例、权限要求
