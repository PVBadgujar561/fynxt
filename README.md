# FYNXT Stock Trading Desk Backend API

A high-performance, production-ready Spring Boot backend engine designed for a stock trading desk. The system exposes RESTful API endpoints allowing traders to manage orders, manage portfolio holdings, and analyze sector exposure risk profiles. 

This implementation is architected to operate under heavy concurrent load, guaranteeing strict business rule enforcement and absolute data consistency across all multi-threaded environments.

---

## 🛠️ Tech Stack & Prerequisites

* **Java 17+** (Eclipse Temurin / Amazon Corretto OpenJDK distribution)
* **Spring Boot 3.x / 4.x** (Web, Data JPA)
* **PostgreSQL 15+** (Relational storage runtime database)
* **Docker & Docker Compose** (Containerization orchestration infrastructure)
* **Maven** (Dependency compilation and build pipeline)

---

## 🏗️ Architectural Core Decisions & Design Trade-offs

### 1. Concurrency & Gate-Locking Strategy (The 3-Pending Cap Rule)
* **The Challenge:** The business rule dictates that a trader cannot exceed 3 simultaneous orders in a `PENDING` state. Standard database row-level locking (`SELECT ... FOR UPDATE`) fails if no rows exist yet for a brand new trader. In a rapid concurrent burst, multiple parallel connection threads would read a count of `0` at the exact same millisecond, slip past the check, and double-create entries, violating the system limits.
* **The Solution:** We implemented an application-level, thread-safe synchronization gateway using a `ConcurrentHashMap` registering fair `ReentrantLock` instances bound to individual trader identity references utilizing `traderId.intern()`. 
* **The Benefit:** Parallel threads executing actions against the same trader account are forced to serialize in an ordered line right at the entrance of the transaction context. This completely prevents race conditions during zero-state entries without introducing database thread starvation or table deadlock penalties.

### 2. Multi-Tier Data Integrity Strategy
* **The Solution:** Asset protection is handled via a defensive double-layer strategy. Business logic validations run inside the Java service transaction boundary, while strict `CHECK (quantity >= 0)` constraints are defined directly on the PostgreSQL `portfolios` relational table layer.
* **The Benefit:** The application layer intercepts invalid actions instantly to return clean, fast, descriptive REST exception responses. Meanwhile, the lower database constraints act as a bulletproof safeguard against downstream code updates or external direct database mutations.

### 3. Pure Java Decoupled Analytics Engine
* **The Solution:** Per assignment specifications, the **Sector Overlap Analysis** calculation logic is isolated inside a pure Java component (`SectorOverlapCalculator`) containing absolute zero framework dependencies or external relational calls.
* **The Benefit:** This strictly adheres to clean architecture principles. The mathematical loop processes entirely within isolated memory allocations, allowing the analytic framework to be completely testable using basic unit tests without initializing heavy integration frameworks or database stubs.

---

## 📦 Core REST API Endpoints Exposed

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| **POST** | `/api/orders` | Places a new stock trade (`BUY`/`SELL`). Initialized to `PENDING`. |
| **POST** | `/api/orders/{id}/fill` | Fills an existing `PENDING` order, updating portfolio inventory records. |
| **POST** | `/api/orders/{id}/cancel` | Cancels a `PENDING` trade out of the order book. |
| **GET** | `/api/portfolios/{traderId}` | Returns the trader's total active stock balances summarized by sector. |
| **GET** | `/api/portfolios/{traderId}/overlap` | Computes risk thresholds against `TECH_HEAVY`, `FINANCE_HEAVY`, and `BALANCED` baskets. |
| **POST** | `/api/portfolios/{traderId}/items` | Manually injects stock allocations directly into a trader's holdings. |

---

## 🚀 How to Build and Run the Application

### 1. Pre-compilation Build
Compile the application binary package and execute the automated verification test framework (which runs natively against your local test configurations):
```bash
mvn clean package
```

### 2. Launch Using Docker Compose
Spin up the entirely containerized network (PostgreSQL container instance + Spring Boot application service container) seamlessly with a single command:
```bash
docker compose up --build
```

### 3. Tearing Down the Ecosystem
To stop the application context and purge temporary container state configurations, execute:
```bash
docker compose down -v
```
