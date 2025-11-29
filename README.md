# Card Service

> 카드 관리/결제 서비스 - 카드 발급, 결제 승인, 한도 관리

## 📋 개요

| 항목 | 내용 |
|------|------|
| 포트 | 8084 |
| 데이터베이스 | card_db (PostgreSQL) |
| 주요 역할 | 카드 발급/관리, 결제 처리 |

## 🎯 학습 포인트

### 1. Resilience4j 패턴들 ⭐ (핵심 학습 주제)

#### Circuit Breaker (회로 차단기)

**왜 필요한가?**
> 외부 서비스 장애 시 연쇄 장애(Cascading Failure) 방지

```
┌─────────────────────────────────────────────────────────────┐
│                Circuit Breaker 상태 전이                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                    ┌─────────┐                              │
│                    │ CLOSED  │ ◄── 정상 상태                │
│                    │ (닫힘)   │     모든 요청 통과          │
│                    └────┬────┘                              │
│                         │                                   │
│            실패율 > 임계값 (예: 50%)                         │
│                         │                                   │
│                         ▼                                   │
│                    ┌─────────┐                              │
│        ┌──────────│  OPEN   │ ◄── 차단 상태                │
│        │          │ (열림)   │     모든 요청 즉시 실패      │
│        │          └────┬────┘                              │
│        │               │                                   │
│        │      대기 시간 경과 (예: 10초)                     │
│        │               │                                   │
│        │               ▼                                   │
│        │          ┌──────────┐                             │
│        │          │HALF_OPEN │ ◄── 테스트 상태             │
│        │          │(반개방)   │     일부 요청만 허용        │
│        │          └────┬─────┘                             │
│        │               │                                   │
│        │    ┌──────────┴──────────┐                        │
│        │    │                     │                        │
│        │  성공                   실패                       │
│        │    │                     │                        │
│        │    ▼                     │                        │
│        │  CLOSED                  │                        │
│        │  (복구)                  │                        │
│        │                         │                        │
│        └─────────────────────────┘                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**코드 예시**
```java
@CircuitBreaker(name = "paymentCircuitBreaker", fallbackMethod = "paymentFallback")
public PaymentResult processPayment(PaymentRequest request) {
    return accountServiceClient.debit(request);
}

// Circuit이 OPEN일 때 호출되는 Fallback 메서드
public PaymentResult paymentFallback(PaymentRequest request, Exception e) {
    log.warn("Circuit breaker activated: {}", e.getMessage());
    return PaymentResult.pendingForRetry(request.getPaymentId());
}
```

---

#### Retry (재시도)

**왜 필요한가?**
> 일시적인 네트워크 오류 등을 자동으로 복구

```
┌─────────────────────────────────────────────────────────────┐
│                    Retry 동작 방식                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   요청                                                      │
│     │                                                       │
│     ▼                                                       │
│   1차 시도 ──────────────────────────────────────> 성공 ✓   │
│     │                                                       │
│     │ 실패                                                  │
│     ▼                                                       │
│   ⏳ 대기 (1초)                                             │
│     │                                                       │
│     ▼                                                       │
│   2차 시도 ──────────────────────────────────────> 성공 ✓   │
│     │                                                       │
│     │ 실패                                                  │
│     ▼                                                       │
│   ⏳ 대기 (2초) ← 지수 백오프 (Exponential Backoff)         │
│     │                                                       │
│     ▼                                                       │
│   3차 시도 ──────────────────────────────────────> 성공 ✓   │
│     │                                                       │
│     │ 실패                                                  │
│     ▼                                                       │
│   ❌ 최종 실패 (maxAttempts 도달)                           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**코드 예시**
```java
@Retry(name = "externalCardApiRetry", fallbackMethod = "externalApiFallback")
public CardAuthResult authorizeExternal(CardAuthRequest request) {
    return externalCardApi.authorize(request);
}
```

---

#### Bulkhead (격벽)

**왜 필요한가?**
> 동시 호출 수 제한으로 리소스 고갈 방지

```
┌─────────────────────────────────────────────────────────────┐
│                    Bulkhead 동작 방식                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌──────────────────────────────────────────────────┐      │
│   │               Bulkhead (최대 10개)                │      │
│   │  ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐                  │      │
│   │  │ 1 │ │ 2 │ │ 3 │ │...│ │10 │ ← 동시 처리 중   │      │
│   │  └───┘ └───┘ └───┘ └───┘ └───┘                  │      │
│   └──────────────────────────────────────────────────┘      │
│                          │                                  │
│                          │                                  │
│   요청 11번 ─────────────┤                                  │
│                          │                                  │
│          ┌───────────────┴───────────────┐                  │
│          │                               │                  │
│    maxWaitDuration > 0            maxWaitDuration = 0       │
│          │                               │                  │
│          ▼                               ▼                  │
│       대기열                        즉시 거부               │
│      (대기 중)                  BulkheadFullException       │
│          │                                                  │
│    대기 시간 초과                                           │
│          │                                                  │
│          ▼                                                  │
│        거부                                                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**코드 예시**
```java
@Bulkhead(name = "paymentBulkhead", fallbackMethod = "bulkheadFallback")
public PaymentResult processPayment(PaymentRequest request) {
    // 최대 50개 동시 처리
    return doPayment(request);
}
```

---

#### Rate Limiter (속도 제한)

**왜 필요한가?**
> 초당 요청 수 제한으로 서버 보호 및 외부 API 정책 준수

```
┌─────────────────────────────────────────────────────────────┐
│                  Rate Limiter 동작 방식                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   토큰 버킷 (Token Bucket) 방식                             │
│                                                             │
│   ┌─────────────────────────────┐                           │
│   │ 버킷: [●●●●●●●●●●]          │ ← 초당 100개 토큰 생성    │
│   │       (100개 토큰)          │                           │
│   └─────────────────────────────┘                           │
│                  │                                          │
│                  │                                          │
│   요청 1 ────────┼──> 토큰 1개 소비 → 처리 ✓               │
│   요청 2 ────────┼──> 토큰 1개 소비 → 처리 ✓               │
│   ...            │                                          │
│   요청 100 ──────┼──> 토큰 1개 소비 → 처리 ✓               │
│   요청 101 ──────┼──> 토큰 없음! → ❌ 거부                  │
│                  │                                          │
│             1초 후 토큰 리필                                 │
│                  │                                          │
│   요청 102 ──────┼──> 토큰 1개 소비 → 처리 ✓               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**코드 예시**
```java
@RateLimiter(name = "externalCardApiRateLimiter", fallbackMethod = "rateLimitFallback")
public CardAuthResult authorizeExternal(CardAuthRequest request) {
    // 초당 50건 제한 (외부 카드사 API 정책)
    return externalCardApi.authorize(request);
}
```

---

### 2. 패턴 조합

```java
// 권장 순서: RateLimiter → CircuitBreaker → Bulkhead → Retry
@RateLimiter(name = "payment")
@CircuitBreaker(name = "payment")
@Bulkhead(name = "payment")
@Retry(name = "payment")
public PaymentResult processPayment(PaymentRequest request) {
    return accountServiceClient.debit(request);
}
```

---

## 🗄️ 도메인 모델

### Card Entity

```
┌─────────────────────────────────────────────┐
│                   Card                       │
├─────────────────────────────────────────────┤
│ id: Long (PK, Auto)                         │
│ cardNumber: String (16자리, 암호화)          │
│ cardNumberMasked: String (9410-****-****-1234)│
│ userId: Long (FK → User)                    │
│ accountId: Long (FK → Account)              │
│ cardType: CardType                          │
│ status: CardStatus                          │
│ expiryDate: YearMonth (MM/YY)               │
│ cvc: String (암호화)                        │
│ dailyLimit: BigDecimal                      │
│ monthlyLimit: BigDecimal                    │
│ dailyUsed: BigDecimal                       │
│ monthlyUsed: BigDecimal                     │
│ createdAt: LocalDateTime                    │
│ version: Long (@Version)                    │
└─────────────────────────────────────────────┘
```

### Payment Entity

```
┌─────────────────────────────────────────────┐
│                  Payment                     │
├─────────────────────────────────────────────┤
│ id: Long (PK, Auto)                         │
│ paymentId: String (UUID, Unique)            │
│ cardId: Long (FK → Card)                    │
│ merchantName: String (가맹점명)              │
│ merchantId: String (가맹점 ID)               │
│ amount: BigDecimal                          │
│ status: PaymentStatus                       │
│ approvalNumber: String (승인 번호)           │
│ failReason: String                          │
│ requestedAt: LocalDateTime                  │
│ approvedAt: LocalDateTime                   │
│ cancelledAt: LocalDateTime                  │
└─────────────────────────────────────────────┘
```

### CardType Enum
```java
public enum CardType {
    DEBIT,    // 체크카드 (즉시 출금)
    CREDIT    // 신용카드 (후불)
}
```

### CardStatus Enum
```java
public enum CardStatus {
    ACTIVE,      // 정상
    INACTIVE,    // 비활성화
    BLOCKED,     // 분실/도난 신고
    EXPIRED,     // 만료
    TERMINATED   // 해지
}
```

### PaymentStatus Enum
```java
public enum PaymentStatus {
    PENDING,    // 처리 중
    APPROVED,   // 승인
    DECLINED,   // 거절
    CANCELLED,  // 취소
    REFUNDED    // 환불
}
```

---

## 📡 API 명세

### 1. 카드 발급 신청
```http
POST /api/v1/cards
X-User-Id: 1
X-User-Role: USER
Content-Type: application/json

{
  "accountId": 1,
  "cardType": "DEBIT",
  "dailyLimit": 1000000,
  "monthlyLimit": 10000000
}
```

**Response (201 Created)**
```json
{
  "cardId": 1,
  "cardNumber": "9410-****-****-1234",
  "cardType": "DEBIT",
  "expiryDate": "01/29",
  "status": "ACTIVE",
  "dailyLimit": 1000000,
  "monthlyLimit": 10000000,
  "createdAt": "2024-01-15T10:30:00"
}
```

**이벤트 발행**: `card.issued`

---

### 2. 내 카드 목록 조회
```http
GET /api/v1/cards
X-User-Id: 1
X-User-Role: USER
```

**Response (200 OK)**
```json
{
  "cards": [
    {
      "cardId": 1,
      "cardNumber": "9410-****-****-1234",
      "cardType": "DEBIT",
      "status": "ACTIVE",
      "expiryDate": "01/29",
      "dailyLimit": 1000000,
      "dailyUsed": 150000,
      "dailyRemaining": 850000
    }
  ]
}
```

---

### 3. 결제 승인 요청
```http
POST /api/v1/cards/{cardId}/payments
X-User-Id: 1
X-User-Role: USER
X-Idempotency-Key: payment-uuid-12345
Content-Type: application/json

{
  "amount": 50000,
  "merchantName": "스타벅스 강남점",
  "merchantId": "MERCHANT-001"
}
```

**Response (200 OK)**
```json
{
  "paymentId": "pay-uuid-abcd",
  "status": "APPROVED",
  "cardNumber": "9410-****-****-1234",
  "amount": 50000,
  "merchantName": "스타벅스 강남점",
  "approvalNumber": "12345678",
  "approvedAt": "2024-01-15T14:30:00"
}
```

**결제 거절 시 (400 Bad Request)**
```json
{
  "paymentId": "pay-uuid-efgh",
  "status": "DECLINED",
  "error": "DAILY_LIMIT_EXCEEDED",
  "message": "일일 한도를 초과했습니다.",
  "dailyLimit": 1000000,
  "dailyUsed": 980000,
  "requestedAmount": 50000
}
```

**이벤트 발행**: `card.payment.completed`

---

### 4. 결제 취소
```http
POST /api/v1/cards/payments/{paymentId}/cancel
X-User-Id: 1
X-User-Role: USER
Content-Type: application/json

{
  "reason": "고객 변심"
}
```

**Response (200 OK)**
```json
{
  "paymentId": "pay-uuid-abcd",
  "status": "CANCELLED",
  "cancelledAt": "2024-01-15T15:00:00",
  "refundAmount": 50000
}
```

**이벤트 발행**: `card.payment.cancelled`

---

### 5. 카드 한도 변경
```http
PUT /api/v1/cards/{cardId}/limits
X-User-Id: 1
X-User-Role: USER
Content-Type: application/json

{
  "dailyLimit": 2000000,
  "monthlyLimit": 20000000
}
```

**Response (200 OK)**
```json
{
  "cardId": 1,
  "dailyLimit": 2000000,
  "monthlyLimit": 20000000,
  "updatedAt": "2024-01-15T16:00:00"
}
```

---

### 6. 카드 분실 신고
```http
POST /api/v1/cards/{cardId}/block
X-User-Id: 1
X-User-Role: USER
Content-Type: application/json

{
  "reason": "분실"
}
```

**Response (200 OK)**
```json
{
  "cardId": 1,
  "status": "BLOCKED",
  "blockedAt": "2024-01-15T17:00:00",
  "message": "카드가 정지되었습니다."
}
```

---

### 7. 결제 내역 조회
```http
GET /api/v1/cards/{cardId}/payments?page=0&size=20
X-User-Id: 1
X-User-Role: USER
```

**Response (200 OK)**
```json
{
  "content": [
    {
      "paymentId": "pay-uuid-abcd",
      "amount": 50000,
      "merchantName": "스타벅스 강남점",
      "status": "APPROVED",
      "approvedAt": "2024-01-15T14:30:00"
    },
    {
      "paymentId": "pay-uuid-ijkl",
      "amount": 30000,
      "merchantName": "편의점",
      "status": "APPROVED",
      "approvedAt": "2024-01-15T12:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 45
}
```

---

## 📂 패키지 구조

```
com.junbank.card
├── CardServiceApplication.java
├── domain
│   ├── entity
│   │   ├── Card.java
│   │   └── Payment.java
│   ├── enums
│   │   ├── CardType.java
│   │   ├── CardStatus.java
│   │   └── PaymentStatus.java
│   └── repository
│       ├── CardRepository.java
│       └── PaymentRepository.java
├── application
│   ├── service
│   │   ├── CardService.java
│   │   └── PaymentService.java
│   ├── dto
│   │   ├── request
│   │   │   ├── CardIssueRequest.java
│   │   │   ├── PaymentRequest.java
│   │   │   └── LimitChangeRequest.java
│   │   └── response
│   │       ├── CardResponse.java
│   │       └── PaymentResponse.java
│   └── exception
│       ├── CardNotFoundException.java
│       ├── LimitExceededException.java
│       └── PaymentDeclinedException.java
├── infrastructure
│   ├── kafka
│   │   ├── CardEventProducer.java
│   │   └── AccountEventConsumer.java
│   ├── feign
│   │   └── AccountServiceClient.java
│   ├── resilience
│   │   ├── CircuitBreakerConfig.java
│   │   ├── RetryConfig.java
│   │   ├── BulkheadConfig.java
│   │   └── RateLimiterConfig.java
│   ├── external
│   │   └── ExternalCardApiClient.java
│   └── config
│       ├── JpaConfig.java
│       └── KafkaConfig.java
└── presentation
    ├── controller
    │   └── CardController.java
    └── advice
        └── CardExceptionHandler.java
```

---

## 🔗 서비스 간 통신

### 발행 이벤트 (Kafka Producer)
| 이벤트 | 토픽 | 수신 서비스 | 설명 |
|--------|------|-------------|------|
| CARD_ISSUED | card.issued | Ledger | 카드 발급 기록 |
| PAYMENT_REQUESTED | card.payment.requested | Account | 결제금 차감 요청 |
| PAYMENT_COMPLETED | card.payment.completed | Ledger | 결제 완료 기록 |
| PAYMENT_CANCELLED | card.payment.cancelled | Ledger, Account | 결제 취소 |

### 수신 이벤트 (Kafka Consumer)
| 이벤트 | 토픽 | 발신 서비스 | 설명 |
|--------|------|-------------|------|
| DEBIT_COMPLETED | account.debit.completed | Account | 차감 완료 응답 |
| DEBIT_FAILED | account.debit.failed | Account | 차감 실패 응답 |

### Feign Client 호출
| 대상 서비스 | 용도 | Resilience4j |
|-------------|------|--------------|
| Account Service | 잔액 차감 | CircuitBreaker, Retry, Bulkhead |
| External Card API | 외부 승인 | CircuitBreaker, Retry, RateLimiter |

---

## ⚙️ Resilience4j 설정

### application.yml (config-repo)
```yaml
resilience4j:
  circuitbreaker:
    instances:
      paymentCircuitBreaker:
        failureRateThreshold: 40
        slowCallRateThreshold: 50
        slowCallDurationThreshold: 2s
        slidingWindowSize: 20
        waitDurationInOpenState: 15s

  retry:
    instances:
      paymentRetry:
        maxAttempts: 2
        waitDuration: 500ms

  bulkhead:
    instances:
      paymentBulkhead:
        maxConcurrentCalls: 50
        maxWaitDuration: 100ms

  ratelimiter:
    instances:
      externalCardApiRateLimiter:
        limitForPeriod: 50
        limitRefreshPeriod: 1s
```

---

## 🧪 테스트 시나리오

### 1. Circuit Breaker 테스트
```java
@Test
void 연속_실패시_회로_차단() {
    // Given: Account Service가 계속 실패하도록 설정
    
    // When: 20번 결제 요청 (10번 실패 → failureRate 50%)
    
    // Then: 
    // 1. 11번째 요청부터 CircuitBreakerOpenException 발생
    // 2. Fallback 메서드 호출됨
    // 3. 15초 후 HALF_OPEN 상태로 전환
}
```

### 2. Rate Limiter 테스트
```java
@Test
void 초당_요청_제한_확인() {
    // Given: 초당 50건 제한 설정
    
    // When: 1초 내에 60건 요청
    
    // Then:
    // 1. 50건 성공
    // 2. 10건 RequestNotPermitted 예외
}
```

### 3. API 테스트
```bash
# 결제 요청
curl -X POST http://localhost:8080/api/v1/cards/1/payments \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -H "X-User-Role: USER" \
  -H "X-Idempotency-Key: test-payment-1" \
  -d '{"amount":50000,"merchantName":"테스트가맹점","merchantId":"M001"}'

# 결제 취소
curl -X POST http://localhost:8080/api/v1/cards/payments/pay-uuid-abcd/cancel \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -H "X-User-Role: USER" \
  -d '{"reason":"테스트 취소"}'
```

---

## 📝 구현 체크리스트

- [ ] Entity, Repository 생성
- [ ] CardService 구현
- [ ] PaymentService 구현
- [ ] **CircuitBreaker 적용**
- [ ] **Retry 적용**
- [ ] **Bulkhead 적용**
- [ ] **RateLimiter 적용**
- [ ] **Fallback 메서드 구현**
- [ ] Controller 구현
- [ ] Kafka Producer 구현
- [ ] Kafka Consumer 구현
- [ ] Feign Client 구현 (Account Service)
- [ ] External API Client 구현 (Mock)
- [ ] Resilience4j 테스트 코드
- [ ] 단위 테스트
- [ ] 통합 테스트
- [ ] API 문서화 (Swagger)