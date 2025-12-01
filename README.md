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
│            실패율 > 임계값 (예: 40%)                         │
│                         │                                   │
│                         ▼                                   │
│                    ┌─────────┐                              │
│        ┌──────────│  OPEN   │ ◄── 차단 상태                │
│        │          │ (열림)   │     모든 요청 즉시 실패      │
│        │          └────┬────┘                              │
│        │               │                                   │
│        │      대기 시간 경과 (예: 15초)                     │
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

#### Retry (재시도)

**왜 필요한가?**
> 일시적인 네트워크 오류 등을 자동으로 복구

```
요청
  │
  ▼
1차 시도 ──────────────────────────────────────> 성공 ✓
  │
  │ 실패
  ▼
⏳ 대기 (500ms)
  │
  ▼
2차 시도 ──────────────────────────────────────> 성공 ✓
  │
  │ 실패
  ▼
❌ 최종 실패 (maxAttempts=2 도달)
```

#### Bulkhead (격벽)

**왜 필요한가?**
> 동시 호출 수 제한으로 리소스 고갈 방지

```
┌──────────────────────────────────────────────────┐
│               Bulkhead (최대 50개)                │
│  ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐ ... ┌───┐       │
│  │ 1 │ │ 2 │ │ 3 │ │...│ │49 │     │50 │       │
│  └───┘ └───┘ └───┘ └───┘ └───┘     └───┘       │
└──────────────────────────────────────────────────┘
                       │
요청 51번 ─────────────┤
                       │
              maxWaitDuration = 100ms
                       │
                 시간 초과 → ❌ BulkheadFullException
```

#### RateLimiter (속도 제한)

**왜 필요한가?**
> 초당 요청 수 제한으로 서버 보호 및 외부 API 정책 준수

```
토큰 버킷 (Token Bucket) 방식

┌─────────────────────────────┐
│ 버킷: [●●●●●●●●●●]          │ ← 초당 50개 토큰 생성
│       (50개 토큰)           │
└─────────────────────────────┘
               │
요청 1~50 ─────┼──> 토큰 소비 → 처리 ✓
요청 51 ───────┼──> 토큰 없음! → ❌ RequestNotPermitted
               │
          1초 후 토큰 리필
               │
요청 52 ───────┼──> 토큰 소비 → 처리 ✓
```

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

### 도메인 구조 (2개 Bounded Context)
```
domain/
├── card/                              # 카드 도메인 (8개)
│   └── domain/
│       ├── exception/
│       │   ├── CardErrorCode.java     # 카드 에러 코드 (CRD_xxx)
│       │   └── CardException.java     # 카드 예외
│       └── model/
│           ├── Card.java              # Aggregate Root (한도 관리)
│           ├── CardStatus.java        # ACTIVE/INACTIVE/BLOCKED/EXPIRED/TERMINATED
│           ├── CardType.java          # DEBIT/CREDIT
│           └── vo/
│               ├── CardId.java        # CRD-xxxxxxxx
│               ├── CardNumber.java    # 16자리 (Luhn 알고리즘)
│               └── Money.java         # 금액 VO
│
└── payment/                           # 결제 도메인 (6개)
    └── domain/
        ├── exception/
        │   ├── PaymentErrorCode.java  # 결제 에러 코드 (PAY_xxx)
        │   └── PaymentException.java  # 결제 예외
        └── model/
            ├── Payment.java           # Aggregate Root
            ├── PaymentStatus.java     # PENDING/APPROVED/DECLINED/CANCELLED/REFUNDED
            └── vo/
                ├── PaymentId.java     # PAY-xxxxxxxx
                └── Money.java         # 금액 VO
```

### 도메인 분리 이유
| 도메인 | 책임 | 특성 |
|--------|------|------|
| **card** | 카드 발급/관리, 한도 정책 | 상태 변경 가능, Luhn 알고리즘 |
| **payment** | 결제 생명주기 관리 | 승인/취소/환불, 독립적인 Aggregate |

### Card 도메인 모델
```
┌─────────────────────────────────────────────────────────────┐
│                           Card                               │
├─────────────────────────────────────────────────────────────┤
│ 【핵심 필드】                                                 │
│ cardId: CardId (PK, CRD-xxxxxxxx)                           │
│ cardNumber: CardNumber (16자리, Luhn 검증)                  │
│ userId: String (USR-xxx)                                    │
│ accountId: String (ACC-xxx, 연결 계좌)                      │
│ cardType: CardType (DEBIT/CREDIT)                          │
│ status: CardStatus                                          │
│ expiryDate: YearMonth (발급일 + 5년)                        │
│ cvc: String (3자리, 암호화)                                 │
├─────────────────────────────────────────────────────────────┤
│ 【한도 필드】                                                 │
│ dailyLimit, monthlyLimit (설정 한도)                        │
│ dailyUsed, monthlyUsed (사용액)                             │
│ lastUsedDate, lastUsedMonth (초기화 기준)                   │
│ version: Long (@Version, 동시성 제어)                       │
├─────────────────────────────────────────────────────────────┤
│ 【감사 필드 - BaseEntity】                                    │
│ createdAt, updatedAt, createdBy, updatedBy                  │
│ deletedAt, deletedBy, isDeleted (Soft Delete)               │
├─────────────────────────────────────────────────────────────┤
│ 【한도 관리 메서드】                                          │
│ + validatePayment(amount)  // 상태, 유효기간, 한도 검증      │
│ + recordPayment(amount)    // 사용액 누적                    │
│ + recordCancellation(amount) // 사용액 차감                  │
├─────────────────────────────────────────────────────────────┤
│ 【상태 변경 메서드】                                          │
│ + activate(), deactivate()                                  │
│ + block(), unblock() (분실/도난 신고)                       │
│ + terminate(), expire()                                     │
│ + changeLimits(daily, monthly)                              │
├─────────────────────────────────────────────────────────────┤
│ 【상태 확인 메서드】                                          │
│ + isNew(), isActive(), isExpired(), canPay()                │
│ + isDebitCard(), isCreditCard()                             │
└─────────────────────────────────────────────────────────────┘
```

### CardType Enum (정책 메서드)
```java
public enum CardType {
    DEBIT("체크카드", immediateDebit=true, dailyDefault=500만, monthlyDefault=5000만),
    CREDIT("신용카드", immediateDebit=false, dailyDefault=1000만, monthlyDefault=1억);
    
    public boolean isImmediateDebit();  // 즉시 출금 여부
    public BigDecimal getDefaultDailyLimit();
    public BigDecimal getDefaultMonthlyLimit();
    public boolean isDebit();
    public boolean isCredit();
}
```

### CardStatus Enum (상태 전이)
```
ACTIVE ────┬── 비활성화 ──▶ INACTIVE ──┬── 활성화 ──▶ ACTIVE
           ├── 분실신고 ──▶ BLOCKED ───┼── 해제 ────▶ ACTIVE
           ├── 만료 ──────▶ EXPIRED ───┘
           └── 해지 ──────────────────────────────▶ TERMINATED (최종)
```

**정책 메서드:**
```java
public enum CardStatus {
    ACTIVE, INACTIVE, BLOCKED, EXPIRED, TERMINATED;
    
    public boolean canPay();           // 결제 가능 여부
    public boolean canReactivate();    // 재활성화 가능 여부
    public boolean canTerminate();     // 해지 가능 여부
    public boolean canTransitionTo(CardStatus target);
    public Set<CardStatus> getAllowedTransitions();
}
```

### Payment 도메인 모델
```
┌─────────────────────────────────────────────────────────────┐
│                         Payment                              │
├─────────────────────────────────────────────────────────────┤
│ 【핵심 필드】                                                 │
│ paymentId: PaymentId (PAY-xxxxxxxx)                         │
│ cardId: String (CRD-xxx)                                    │
│ merchantName: String (가맹점명)                              │
│ merchantId: String (가맹점 ID)                               │
│ amount: Money                                               │
│ status: PaymentStatus                                       │
│ approvalNumber: String (승인 번호)                          │
│ failReason, cancelReason: String                            │
│ idempotencyKey: String (멱등성 키)                          │
│ requestedAt, approvedAt, cancelledAt                        │
├─────────────────────────────────────────────────────────────┤
│ 【감사 필드】                                                 │
│ createdAt, updatedAt, createdBy, updatedBy                  │
│ deletedAt, deletedBy, isDeleted (Soft Delete)               │
├─────────────────────────────────────────────────────────────┤
│ 【비즈니스 메서드】                                           │
│ + approve(approvalNumber)  // → APPROVED                    │
│ + decline(reason)          // → DECLINED                    │
│ + cancel(reason)           // → CANCELLED (당일 취소)       │
│ + refund(reason)           // → REFUNDED (익일 이후 환불)   │
├─────────────────────────────────────────────────────────────┤
│ 【상태 확인 메서드】                                          │
│ + isNew(), isPending(), isApproved(), isDeclined()          │
│ + canCancel(), isFinal()                                    │
└─────────────────────────────────────────────────────────────┘
```

### PaymentStatus Enum (상태 전이)
```
              승인 성공
┌─────────┐ ─────────▶ ┌──────────┐ ─── 취소 ──▶ ┌───────────┐
│ PENDING │            │ APPROVED │              │ CANCELLED │
└─────────┘            └──────────┘              └───────────┘
     │                       │
     │ 승인 거절             │ 환불
     ▼                       ▼
┌──────────┐            ┌──────────┐
│ DECLINED │            │ REFUNDED │
└──────────┘            └──────────┘
```

**정책 메서드:**
```java
public enum PaymentStatus {
    PENDING, APPROVED, DECLINED, CANCELLED, REFUNDED;
    
    public boolean isFinal();
    public boolean canCancel();
    public boolean canTransitionTo(PaymentStatus target);
}
```

### CardNumber VO (Luhn 알고리즘)
```java
public record CardNumber(String value) {
    public static final String BIN = "9410";  // jun-bank 카드사 번호
    
    public static CardNumber generate();      // 새 카드번호 생성 (BIN + 11자리 + 체크디지트)
    public String masked();                   // 9410-****-****-1234
    public String formatted();                // 9410-1234-5678-9012
    public String getPrefix();                // 앞 6자리
    public String getLastFour();              // 뒤 4자리
}
```

### Exception 체계

#### CardErrorCode (CRD_xxx)
```java
public enum CardErrorCode implements ErrorCode {
    // 유효성 (400)
    INVALID_CARD_ID_FORMAT, INVALID_CARD_NUMBER_FORMAT, 
    INVALID_CVC, INVALID_LIMIT, INVALID_EXPIRY_DATE,
    
    // 조회 (404)
    CARD_NOT_FOUND,
    
    // 한도 (400)
    DAILY_LIMIT_EXCEEDED, MONTHLY_LIMIT_EXCEEDED, SINGLE_PAYMENT_LIMIT_EXCEEDED,
    
    // 상태 (422)
    CARD_NOT_ACTIVE, CARD_BLOCKED, CARD_EXPIRED, CARD_TERMINATED,
    CARD_ALREADY_ACTIVE, INVALID_STATUS_TRANSITION, NOT_CARD_OWNER,
    
    // 외부 시스템 (503/429)
    EXTERNAL_API_ERROR, CIRCUIT_BREAKER_OPEN, RATE_LIMIT_EXCEEDED, ACCOUNT_SERVICE_ERROR;
}
```

#### PaymentErrorCode (PAY_xxx)
```java
public enum PaymentErrorCode implements ErrorCode {
    // 유효성 (400)
    INVALID_PAYMENT_ID_FORMAT, INVALID_AMOUNT, MINIMUM_PAYMENT_AMOUNT,
    
    // 조회 (404)
    PAYMENT_NOT_FOUND,
    
    // 결제 처리 (400)
    PAYMENT_ALREADY_APPROVED, PAYMENT_ALREADY_CANCELLED, 
    PAYMENT_CANNOT_CANCEL, INSUFFICIENT_BALANCE, PAYMENT_DECLINED,
    
    // 상태 (422)
    INVALID_STATUS_TRANSITION,
    
    // 외부 시스템 (503/429)
    EXTERNAL_API_ERROR, CIRCUIT_BREAKER_OPEN, RATE_LIMIT_EXCEEDED;
}
```

---

## 📡 API 명세

### 1. 카드 발급 신청
```http
POST /api/v1/cards
X-User-Id: USR-a1b2c3d4
X-User-Role: USER
Content-Type: application/json

{
  "accountId": "ACC-12345678",
  "cardType": "DEBIT",
  "dailyLimit": 1000000,
  "monthlyLimit": 10000000
}
```

**Response (201 Created)**
```json
{
  "cardId": "CRD-a1b2c3d4",
  "cardNumber": "9410-****-****-1234",
  "cardType": "DEBIT",
  "status": "ACTIVE",
  "expiryDate": "12/29",
  "dailyLimit": 1000000,
  "monthlyLimit": 10000000,
  "createdAt": "2024-01-15T10:00:00"
}
```

### 2. 결제 요청
```http
POST /api/v1/cards/{cardId}/payments
X-User-Id: USR-a1b2c3d4
X-User-Role: USER
X-Idempotency-Key: payment-uuid-12345

{
  "amount": 50000,
  "merchantName": "테스트가맹점",
  "merchantId": "M001"
}
```

**Response (200 OK)**
```json
{
  "paymentId": "PAY-a1b2c3d4",
  "cardId": "CRD-12345678",
  "amount": 50000,
  "status": "APPROVED",
  "approvalNumber": "AP123456",
  "merchantName": "테스트가맹점",
  "approvedAt": "2024-01-15T12:00:00"
}
```

### 3. 결제 취소
```http
POST /api/v1/cards/payments/{paymentId}/cancel
X-User-Id: USR-a1b2c3d4
X-User-Role: USER

{
  "reason": "고객 요청 취소"
}
```

### 4. 카드 상태 변경
```http
PATCH /api/v1/cards/{cardId}/status
X-User-Id: USR-a1b2c3d4
X-User-Role: USER

{
  "action": "BLOCK",
  "reason": "분실 신고"
}
```

### 5. 결제 내역 조회
```http
GET /api/v1/cards/{cardId}/payments?page=0&size=20
X-User-Id: USR-a1b2c3d4
X-User-Role: USER
```

---

## 📂 패키지 구조

```
com.jun_bank.card_service
├── CardServiceApplication.java
├── global/                              # 전역 설정 레이어
│   ├── config/
│   │   ├── JpaConfig.java
│   │   ├── QueryDslConfig.java
│   │   ├── KafkaProducerConfig.java
│   │   ├── KafkaConsumerConfig.java
│   │   ├── SecurityConfig.java
│   │   ├── FeignConfig.java
│   │   ├── SwaggerConfig.java
│   │   └── AsyncConfig.java
│   ├── infrastructure/
│   │   ├── entity/
│   │   │   └── BaseEntity.java
│   │   └── jpa/
│   │       └── AuditorAwareImpl.java
│   ├── security/
│   │   ├── UserPrincipal.java
│   │   ├── HeaderAuthenticationFilter.java
│   │   └── SecurityContextUtil.java
│   ├── feign/
│   │   ├── FeignErrorDecoder.java
│   │   └── FeignRequestInterceptor.java
│   └── aop/
│       └── LoggingAspect.java
└── domain/
    ├── card/                            # 카드 Bounded Context ★
    │   ├── domain/                      # 순수 도메인 ✅
    │   │   ├── exception/
    │   │   │   ├── CardErrorCode.java   # CRD_xxx 에러 코드
    │   │   │   └── CardException.java
    │   │   └── model/
    │   │       ├── Card.java            # Aggregate Root
    │   │       ├── CardType.java
    │   │       ├── CardStatus.java
    │   │       └── vo/
    │   │           ├── CardId.java
    │   │           ├── CardNumber.java  # Luhn 알고리즘
    │   │           └── Money.java
    │   ├── application/                 # (TODO)
    │   ├── infrastructure/
    │   │   ├── persistence/
    │   │   ├── kafka/
    │   │   ├── feign/
    │   │   └── resilience/              # Resilience4j (TODO)
    │   └── presentation/                # (TODO)
    │
    └── payment/                         # 결제 Bounded Context ★
        ├── domain/                      # 순수 도메인 ✅
        │   ├── exception/
        │   │   ├── PaymentErrorCode.java # PAY_xxx 에러 코드
        │   │   └── PaymentException.java
        │   └── model/
        │       ├── Payment.java         # Aggregate Root
        │       ├── PaymentStatus.java
        │       └── vo/
        │           ├── PaymentId.java
        │           └── Money.java
        ├── application/                 # (TODO)
        ├── infrastructure/              # (TODO)
        └── presentation/                # (TODO)
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
        failureRateThreshold: 40          # 실패율 40% 초과시 OPEN
        slowCallRateThreshold: 50         # 느린 호출 50% 초과시 OPEN
        slowCallDurationThreshold: 2s     # 2초 이상 걸리면 느린 호출
        slidingWindowSize: 20             # 최근 20개 요청 기준
        waitDurationInOpenState: 15s      # OPEN 후 15초 대기

  retry:
    instances:
      paymentRetry:
        maxAttempts: 2                    # 최대 2회 시도
        waitDuration: 500ms               # 재시도 간격

  bulkhead:
    instances:
      paymentBulkhead:
        maxConcurrentCalls: 50            # 동시 50건 제한
        maxWaitDuration: 100ms            # 대기 시간

  ratelimiter:
    instances:
      externalCardApiRateLimiter:
        limitForPeriod: 50                # 초당 50건
        limitRefreshPeriod: 1s
```

---

## 📝 구현 체크리스트

### Domain Layer ✅ (14개 파일, 2개 도메인)

#### card 도메인 (카드) - 8개
- [x] CardErrorCode (CRD_xxx 에러 코드)
- [x] CardException (팩토리 메서드 패턴)
- [x] CardType (정책 메서드)
- [x] CardStatus (정책 메서드, 상태 전이)
- [x] CardId VO
- [x] CardNumber VO (Luhn 알고리즘)
- [x] Money VO
- [x] Card (한도 관리)

#### payment 도메인 (결제) - 6개
- [x] PaymentErrorCode (PAY_xxx 에러 코드)
- [x] PaymentException (팩토리 메서드 패턴)
- [x] PaymentStatus (정책 메서드, 상태 전이)
- [x] PaymentId VO
- [x] Money VO
- [x] Payment

### Application Layer
- [ ] CardUseCase
- [ ] PaymentUseCase
- [ ] CardPort
- [ ] PaymentPort
- [ ] DTO 정의

### Infrastructure Layer
- [ ] CardEntity
- [ ] PaymentEntity
- [ ] JpaRepository
- [ ] CircuitBreakerConfig
- [ ] RetryConfig, BulkheadConfig, RateLimiterConfig
- [ ] CardKafkaProducer
- [ ] CardKafkaConsumer
- [ ] AccountServiceClient (Feign)

### Presentation Layer
- [ ] CardController
- [ ] PaymentController
- [ ] Request/Response DTO
- [ ] Swagger 문서화

### 테스트
- [ ] 도메인 단위 테스트
- [ ] 한도 검증 테스트
- [ ] Circuit Breaker 테스트
- [ ] Rate Limiter 테스트