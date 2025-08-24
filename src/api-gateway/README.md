# API Gateway

OpenTelemetry Demo 系统的 API Gateway 服务，基于 Spring Cloud Gateway 构建。

## 功能特性

### 核心功能
- **统一入口点**: 为所有微服务提供统一的API访问入口
- **路由管理**: 智能路由请求到相应的后端服务
- **负载均衡**: 支持多种负载均衡策略
- **健康检查**: 实时监控后端服务状态

### 安全特性
- **身份验证**: 基于会话的身份验证机制
- **授权控制**: 细粒度的API访问控制
- **CORS支持**: 跨域资源共享配置

### 性能与可靠性
- **限流控制**: 基于Redis的分布式限流
- **超时处理**: 可配置的连接和响应超时
- **重试机制**: 自动重试失败的请求
- **熔断保护**: 防止级联故障

### 可观测性
- **OpenTelemetry集成**: 完整的分布式链路追踪
- **结构化日志**: 详细的请求/响应日志记录
- **Prometheus指标**: 丰富的性能指标导出
- **健康监控**: 实时服务健康状态检查

## API 路由规则

| 路径前缀 | 目标服务 | 需要认证 | 描述 |
|---------|---------|---------|------|
| `/api/ads/**` | Ad Service | 否 | 广告服务 |
| `/api/cart/**` | Cart Service | 是 | 购物车服务 |
| `/api/checkout/**` | Checkout Service | 是 | 结账服务 |
| `/api/currency/**` | Currency Service | 否 | 货币服务 |
| `/api/email/**` | Email Service | 是 | 邮件服务 |
| `/api/payment/**` | Payment Service | 是 | 支付服务 |
| `/api/products/**` | Product Catalog | 否 | 产品目录服务 |
| `/api/quote/**` | Quote Service | 否 | 报价服务 |
| `/api/recommendations/**` | Recommendation | 否 | 推荐服务 |
| `/api/shipping/**` | Shipping Service | 是 | 运输服务 |

## 环境变量配置

### 基本配置
- `GATEWAY_PORT`: Gateway服务端口 (默认: 8080)
- `REDIS_HOST`: Redis服务器地址 (默认: valkey-cart)
- `REDIS_PORT`: Redis服务器端口 (默认: 6379)

### 服务地址配置
- `AD_ADDR`: 广告服务地址
- `CART_ADDR`: 购物车服务地址
- `CHECKOUT_ADDR`: 结账服务地址
- `CURRENCY_ADDR`: 货币服务地址
- `EMAIL_ADDR`: 邮件服务地址
- `PAYMENT_ADDR`: 支付服务地址
- `PRODUCT_CATALOG_ADDR`: 产品目录服务地址
- `QUOTE_ADDR`: 报价服务地址
- `RECOMMENDATION_ADDR`: 推荐服务地址
- `SHIPPING_ADDR`: 运输服务地址

### OpenTelemetry配置
- `OTEL_EXPORTER_OTLP_ENDPOINT`: OTLP导出器端点
- `OTEL_SERVICE_NAME`: 服务名称
- `OTEL_SERVICE_VERSION`: 服务版本

## 使用方法

### 本地开发
```bash
# 编译项目
mvn clean package

# 运行服务
java -jar target/api-gateway-1.0.0.jar
```

### Docker构建
```bash
# 构建镜像
docker build -t api-gateway .

# 运行容器
docker run -p 8080:8080 api-gateway
```

### 健康检查
```bash
# 基本健康检查
curl http://localhost:8080/health

# 就绪状态检查
curl http://localhost:8080/ready

# Actuator端点
curl http://localhost:8080/actuator/health
```

## 限流配置

默认限流策略：
- **限制**: 每分钟100个请求
- **识别**: 基于会话ID或客户端IP
- **存储**: 使用Redis进行分布式限流
- **响应**: 超出限制返回429状态码

## 日志格式

所有请求都会生成结构化日志：
```
2024-01-15 10:30:45 [reactor-http-nio-1] INFO  [trace123,span456] LoggingFilter - 
Request ID: uuid-123 | Method: GET | Path: /api/products | Client IP: 192.168.1.100
```

## 监控指标

通过 `/actuator/prometheus` 端点导出的指标包括：
- HTTP请求计数和延迟
- 限流命中统计
- 错误率统计
- 后端服务健康状态

## 故障排除

### 常见问题

1. **Redis连接失败**
   - 检查Redis服务是否运行
   - 验证网络连接和端口配置

2. **后端服务不可用**
   - 检查服务发现配置
   - 验证服务健康状态

3. **认证失败**
   - 确认会话ID格式正确
   - 检查认证逻辑配置

### 调试模式
设置环境变量启用详细日志：
```bash
export LOGGING_LEVEL_COM_OPENTELEMETRY_DEMO_GATEWAY=DEBUG
```
