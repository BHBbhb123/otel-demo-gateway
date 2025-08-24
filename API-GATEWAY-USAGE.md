# API Gateway 使用指南

OpenTelemetry Demo 项目中的 API Gateway 提供了一个统一的入口点来访问所有微服务。

## 🚀 快速开始

### 1. 启动完整系统
```bash
# 克隆项目
git clone https://github.com/open-telemetry/opentelemetry-demo.git
cd opentelemetry-demo

# 启动所有服务（包括API Gateway）
make run

# 或者使用 docker compose
docker compose up -d
```

### 2. 访问API Gateway
API Gateway 将在端口 8090 上运行：
```bash
# 健康检查
curl http://localhost:8090/health

# 获取产品列表
curl http://localhost:8090/api/products?currencyCode=USD

# 获取广告
curl http://localhost:8090/api/ads?contextKeys=home
```

## 📡 API 端点

### 公开端点（无需认证）
| 端点 | 方法 | 描述 |
|------|------|------|
| `/health` | GET | 服务健康检查 |
| `/ready` | GET | 服务就绪状态 |
| `/api/products` | GET | 获取产品列表 |
| `/api/products/{id}` | GET | 获取特定产品 |
| `/api/currency` | GET | 获取支持的货币列表 |
| `/api/ads` | GET | 获取广告 |
| `/api/recommendations` | GET | 获取推荐产品 |
| `/api/quote` | GET | 获取运费报价 |

### 需要认证的端点
| 端点 | 方法 | 描述 |
|------|------|------|
| `/api/cart` | GET | 获取购物车 |
| `/api/cart` | POST | 添加商品到购物车 |
| `/api/cart` | DELETE | 清空购物车 |
| `/api/checkout` | POST | 结账 |
| `/api/payment` | POST | 处理支付 |
| `/api/shipping` | GET | 计算运费 |
| `/api/email` | POST | 发送邮件 |

## 🔐 认证方式

API Gateway 支持多种认证方式：

### 1. Session ID（推荐用于Web应用）
```bash
# 在请求头中传递
curl -H "X-Session-ID: your-session-id" \
     http://localhost:8090/api/cart?currencyCode=USD

# 或作为查询参数
curl "http://localhost:8090/api/cart?sessionId=your-session-id&currencyCode=USD"
```

### 2. Bearer Token
```bash
curl -H "Authorization: Bearer your-token" \
     http://localhost:8090/api/cart?currencyCode=USD
```

## 📊 监控和观察

### 1. 健康检查端点
```bash
# 基础健康检查
curl http://localhost:8090/health

# Spring Boot Actuator 端点
curl http://localhost:8090/actuator/health
curl http://localhost:8090/actuator/metrics
curl http://localhost:8090/actuator/prometheus
```

### 2. 请求追踪
每个通过 API Gateway 的请求都会：
- 生成唯一的请求ID（`X-Request-ID` 头）
- 创建 OpenTelemetry 追踪链路
- 记录详细的访问日志
- 导出 Prometheus 指标

### 3. 查看追踪数据
- **Jaeger UI**: http://localhost:16686
- **Grafana**: http://localhost:3000  
- **Prometheus**: http://localhost:9090

## 🔧 限流控制

API Gateway 实现了分布式限流：

### 默认限制
- **频率**: 每分钟 100 个请求
- **识别**: 基于会话ID或客户端IP
- **响应**: 超出限制返回 429 状态码

### 限流头信息
每个响应都包含限流信息：
```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
```

## 🔄 负载均衡和故障处理

### 重试机制
- **重试次数**: 最多 3 次
- **重试条件**: `BAD_GATEWAY`, `SERVICE_UNAVAILABLE`
- **支持方法**: `GET`, `POST`

### 超时设置
- **连接超时**: 10 秒
- **响应超时**: 30 秒

## 🔧 本地开发

### 1. 只启动 API Gateway
```bash
cd src/api-gateway

# 使用 Maven 运行
mvn spring-boot:run

# 或使用 Docker
docker compose -f docker-compose.test.yml up
```

### 2. 配置本地环境
复制 `application-example.yml` 到 `application-local.yml` 并修改配置：
```yaml
server:
  port: 8090  # 避免与frontend-proxy的8080端口冲突

services:
  ad:
    url: http://localhost:9555
  cart:
    url: http://localhost:7070
  # ... 其他服务
```

### 3. 启动依赖服务
```bash
# 启动 Redis（用于限流）
docker run -d -p 6379:6379 valkey/valkey:8.1-alpine

# 启动 OpenTelemetry Collector（可选）
docker run -d -p 4317:4317 -p 4318:4318 \
  otel/opentelemetry-collector-contrib:0.129.1
```

## 🐛 故障排除

### 常见问题

1. **Redis 连接失败**
   ```bash
   # 检查 Redis 是否运行
   docker ps | grep valkey
   
   # 检查网络连接
   telnet localhost 6379
   ```

2. **后端服务不可用**
   ```bash
   # 检查服务状态
   curl http://localhost:9555/health  # Ad Service
   curl http://localhost:7070/health  # Cart Service
   ```

3. **认证失败**
   ```bash
   # 确保包含有效的会话ID
   curl -H "X-Session-ID: test-session" \
        http://localhost:8090/api/cart?currencyCode=USD
   ```

### 调试模式
设置环境变量启用详细日志：
```bash
export LOGGING_LEVEL_COM_OPENTELEMETRY_DEMO_GATEWAY=DEBUG
export LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_CLOUD_GATEWAY=DEBUG
```

## 📝 示例请求

### 获取产品列表
```bash
curl -X GET "http://localhost:8090/api/products?currencyCode=USD" \
     -H "Accept: application/json"
```

### 添加商品到购物车
```bash
curl -X POST "http://localhost:8090/api/cart?currencyCode=USD" \
     -H "X-Session-ID: user-123" \
     -H "Content-Type: application/json" \
     -d '{
       "item": {
         "productId": "OLJCESPC7Z",
         "quantity": 1
       },
       "userId": "user-123"
     }'
```

### 获取购物车内容
```bash
curl -X GET "http://localhost:8090/api/cart?sessionId=user-123&currencyCode=USD" \
     -H "Accept: application/json"
```

### 结账流程
```bash
curl -X POST "http://localhost:8090/api/checkout?currencyCode=USD" \
     -H "X-Session-ID: user-123" \
     -H "Content-Type: application/json" \
     -d '{
       "userId": "user-123",
       "userCurrency": "USD",
       "address": {
         "streetAddress": "1600 Amphitheatre Parkway",
         "city": "Mountain View",
         "state": "CA",
         "country": "United States",
         "zipCode": "94043"
       },
       "email": "user@example.com",
       "creditCard": {
         "creditCardNumber": "4432-8015-6152-0454",
         "creditCardExpirationMonth": 1,
         "creditCardExpirationYear": 2024,
         "creditCardCvv": 672
       }
     }'
```

## 🏗️ 架构图

```
Client Request
      ↓
┌─────────────────┐
│   API Gateway   │ ←── Rate Limiting (Redis)
│   (Port 8090)   │
└─────────────────┘
      ↓
┌─────────────────┐
│ Service Router  │
└─────────────────┘
      ↓
┌──────────────────────────────────────────────┐
│                Backend Services              │
├──────────┬──────────┬──────────┬─────────────┤
│ Product  │   Cart   │ Checkout │    ...      │
│ Catalog  │ Service  │ Service  │             │
│(Port 3550)│(Port 7070)│(Port 5050)│           │
└──────────┴──────────┴──────────┴─────────────┘
      ↓
┌─────────────────┐
│ OpenTelemetry   │
│   Collector     │
└─────────────────┘
```

通过这个 API Gateway，您可以：
- 🔒 安全地访问所有微服务
- 📈 监控所有API调用
- ⚡ 享受自动限流保护  
- 🔍 获得完整的分布式追踪
- 🛡️ 受益于统一的错误处理
