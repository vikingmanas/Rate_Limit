# 🛡️ Distributed Rate Limiter — API Protection System

A production-grade, middleware-based rate limiter built with **Spring Boot** that protects APIs from abuse, bots, and server overload. Supports multiple algorithms, API key authentication, Redis distribution, dynamic configuration, and a real-time monitoring dashboard.

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| **3 Algorithms** | Fixed Window, Sliding Window Log, Token Bucket |
| **API Key Auth** | `X-API-Key` header with plan tiers (FREE / PREMIUM / ADMIN) |
| **IP-Based Limiting** | Anonymous requests rate-limited by client IP |
| **Thread-Safe** | `ConcurrentHashMap` + atomic operations throughout |
| **Redis Distributed** | Lua scripts for atomic counters shared across instances |
| **Graceful Fallback** | Auto-falls back to in-memory when Redis is unavailable |
| **Burst Handling** | Configurable burst multiplier in Token Bucket |
| **Dynamic Config** | Admin API to change rules & switch algorithms at runtime |
| **Monitoring Dashboard** | Real-time Chart.js dashboard with traffic metrics |
| **Middleware** | Spring `HandlerInterceptor` — protects any `/api/**` endpoint |

---

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- Redis (optional — falls back to in-memory)

### Run
```bash
mvn spring-boot:run
```

The app starts on **http://localhost:8080**.

### Test the Rate Limiter
```bash
# With a free API key (30 req/min)
curl -H "X-API-Key: free-key-1" http://localhost:8080/api/data

# With a premium API key (200 req/min)
curl -H "X-API-Key: premium-key-1" http://localhost:8080/api/data

# Without API key (IP-based: 20 req/min)
curl http://localhost:8080/api/data
```

When rate-limited you'll receive:
```json
{
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Please retry after 2 seconds.",
  "retryAfterSeconds": 2
}
```

Response headers on every request:
```
X-RateLimit-Limit: 30
X-RateLimit-Remaining: 28
Retry-After: 60          (only on 429)
```

---

## 📊 Monitoring Dashboard

Open **http://localhost:8080/dashboard** for a real-time dashboard showing:
- Total / Allowed / Blocked request counts
- Block rate percentage
- Traffic distribution pie chart
- Requests-per-minute timeline
- Top users table with status indicators

Auto-refreshes every 5 seconds.

---

## 🔧 Admin API

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/admin/rules` | GET | List all plan rules |
| `/admin/rules?planType=FREE` | POST | Update rule (JSON body) |
| `/admin/rules/FREE` | DELETE | Delete a plan rule |
| `/admin/config/algorithm` | GET | Current algorithm |
| `/admin/config/algorithm?algorithm=SLIDING_WINDOW` | PUT | Switch algorithm |
| `/admin/keys` | GET | List all API keys |
| `/admin/keys?owner=test&plan=PREMIUM` | POST | Create new key |
| `/admin/keys/{key}` | DELETE | Revoke a key |

### Example: Switch algorithm at runtime
```bash
curl -X PUT "http://localhost:8080/admin/config/algorithm?algorithm=SLIDING_WINDOW"
```

### Example: Update free plan limit
```bash
curl -X POST "http://localhost:8080/admin/rules?planType=FREE" \
  -H "Content-Type: application/json" \
  -d '{"maxRequests": 50, "windowSeconds": 60, "burstMultiplier": 1.5, "algorithmType": "TOKEN_BUCKET"}'
```

---

## 🏗️ Architecture

```
Request → ApiKeyAuthFilter → RateLimitInterceptor → Controller
                                   │
                    ┌──────────────┼──────────────┐
                    ▼              ▼               ▼
              Fixed Window   Sliding Window   Token Bucket
                    │              │               │
                    └──────┬───────┘               │
                           ▼                       ▼
                    Redis (distributed)    In-Memory (fallback)
                           │
                           ▼
                    MetricsService → Dashboard
```

---

## 📁 Project Structure

```
src/main/java/com/ratelimiter/
├── algorithm/          # Rate limiting algorithms
├── admin/              # Admin API + rule config service
├── api/                # Sample protected endpoints
├── auth/               # API key auth filter & IP limiter
├── config/             # WebMvc + Redis config
├── interceptor/        # Rate limit middleware
├── model/              # Domain models (DTOs, enums)
├── monitoring/         # Metrics service + controller
└── redis/              # Redis-backed distributed store

src/main/resources/
├── scripts/            # Lua scripts for Redis atomicity
├── templates/          # Thymeleaf dashboard
└── static/             # CSS + JS for dashboard
```

---

## 🧪 Testing

```bash
mvn test
```

Unit tests cover all three algorithms:
- `FixedWindowRateLimiterTest`
- `SlidingWindowRateLimiterTest`
- `TokenBucketRateLimiterTest`

---

## ⚙️ Configuration

See `src/main/resources/application.yml` for all defaults (Redis host, plan limits, default algorithm, IP limits).

### Pre-seeded API Keys

| Key | Plan | Owner |
|-----|------|-------|
| `free-key-1` | FREE | demo-free-user |
| `free-key-2` | FREE | demo-free-user-2 |
| `premium-key-1` | PREMIUM | demo-premium-user |
| `admin-key-1` | ADMIN | system-admin |