# 🛡️ Distributed Rate Limiter — API Protection System

### 🌐 [Live Demo → https://rate-limiter-6p3t.onrender.com](https://rate-limiter-6p3t.onrender.com)

> **Why I Built This:** Every large-scale API — from Twitter to Stripe to OpenAI — needs a rate limiter. Without one, a single abusive user or bot can bring down your entire service. I built this project to deeply understand how rate limiting actually works under the hood: the algorithms, the concurrency challenges, and how to scale it across distributed servers using Redis. It's the kind of invisible infrastructure that powers the internet, and I wanted to build it from scratch instead of just using a library.

---

## 💡 The Intuition

Rate limiting answers a deceptively simple question: **"Should this request be allowed right now?"**

But the answer depends on:
- **Who** is asking (API key → Free user? Premium user? Admin?)
- **How often** they've asked recently (sliding windows, token refills)
- **How many servers** are handling traffic (need shared state via Redis)
- **Can they burst?** (short spikes should be ok if long-term average is fine)

I implemented **3 different algorithms** to explore the tradeoffs:

| Algorithm | Idea | Tradeoff |
|-----------|------|----------|
| **Fixed Window** | Count requests in fixed time blocks (e.g., per minute) | Simple but allows 2× bursts at window boundaries |
| **Sliding Window** | Track exact timestamps of each request in a sliding window | Precise but uses more memory |
| **Token Bucket** | Tokens refill at steady rate; each request costs 1 token | Best for controlled bursts while maintaining long-term limits |

The Token Bucket is the most production-appropriate (used by AWS, Stripe, etc.) and is the default in this system.

### Why Concurrency Matters

APIs handle thousands of requests simultaneously. If two threads read the counter as `29/30` at the same time, both might allow their request — now you're at `31/30`, a race condition. I solved this using:
- **`ConcurrentHashMap`** — lock-free thread-safe map
- **`AtomicLong`** & CAS loops — atomic counter updates with no locks  
- **Redis Lua scripts** — atomic increment-and-check executed server-side

### Why Redis?

In a real production setup, your API runs on **multiple server instances** behind a load balancer. Each instance needs to share the same rate-limit counters. Redis provides:
- Sub-millisecond reads/writes
- Atomic Lua scripts (no race conditions)
- TTL-based key expiry (automatic cleanup)
- But I also built a **graceful fallback** — if Redis goes down, the system seamlessly switches to in-memory rate limiting so your API stays up.

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
| **Interactive Dashboard** | Real-time dashboard with API tester, burst simulator & charts |
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

The app starts on **http://localhost:8080** and redirects to the dashboard.

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

## 📊 Interactive Monitoring Dashboard

The dashboard at **http://localhost:8080/dashboard** lets you:

- **📡 Live API Tester** — Select an API key & endpoint, send requests, see responses in real-time
- **💥 Burst Simulator** — Fire N requests with configurable delay, watch the progress bar and allowed/blocked counts
- **⚙️ Algorithm Switcher** — Switch between Fixed Window, Sliding Window, and Token Bucket with one click
- **📈 Live Charts** — Doughnut chart (allowed vs blocked) + line chart (requests per minute)
- **🏆 Top Users Table** — Ranked by request volume with health status indicators
- **🔴🟢 Redis Status** — Real-time indicator showing Redis Online/Offline

Auto-refreshes every 3 seconds. All controls interact with the live backend.

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
├── algorithm/          # Rate limiting algorithms (3 implementations)
├── admin/              # Admin API + rule config service (hot-reload)
├── api/                # Sample protected endpoints
├── auth/               # API key auth filter & IP limiter
├── config/             # WebMvc + Redis + Home controller
├── interceptor/        # Rate limit middleware (core logic)
├── model/              # Domain models (DTOs, enums)
├── monitoring/         # Metrics service + dashboard controller
└── redis/              # Redis-backed distributed store + Lua scripts
```

---

## 🧪 Testing

```bash
mvn test
```

Unit tests cover all three algorithms with tests for: within-limit, exceeding-limit, independent keys, remaining count, and burst capacity.

---

## 🚢 Deployment

### Build a standalone JAR
```bash
mvn clean package -DskipTests
java -jar target/distributed-rate-limiter-1.0.0.jar
```

The JAR is fully self-contained (embedded Tomcat + all dependencies). Just needs Java 17+ on the server.

### Pre-seeded API Keys

| Key | Plan | Rate Limit |
|-----|------|------------|
| `free-key-1` | FREE | 30 req/min |
| `free-key-2` | FREE | 30 req/min |
| `premium-key-1` | PREMIUM | 200 req/min (2× burst) |
| `admin-key-1` | ADMIN | 1000 req/min (3× burst) |

---

## 🛠️ Tech Stack

- **Backend:** Java 17, Spring Boot 3.2, Spring MVC
- **Storage:** Redis (Lettuce client) + In-Memory (ConcurrentHashMap)
- **Concurrency:** ConcurrentHashMap, AtomicLong, LongAdder, CAS operations
- **Frontend:** Thymeleaf, Chart.js, Vanilla JS
- **Build:** Maven