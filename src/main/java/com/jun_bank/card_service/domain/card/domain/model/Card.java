package com.jun_bank.card_service.domain.card.domain.model;

import com.jun_bank.card_service.domain.card.domain.exception.CardException;
import com.jun_bank.card_service.domain.card.domain.model.vo.CardId;
import com.jun_bank.card_service.domain.card.domain.model.vo.CardNumber;
import com.jun_bank.card_service.domain.card.domain.model.vo.Money;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * 카드 도메인 모델 (Aggregate Root)
 * <p>
 * 카드의 핵심 비즈니스 로직과 한도 관리를 담당합니다.
 *
 * <h3>책임:</h3>
 * <ul>
 *   <li>카드 발급 및 상태 관리</li>
 *   <li>일일/월간 한도 관리</li>
 *   <li>결제 가능 여부 검증</li>
 * </ul>
 *
 * <h3>한도 관리:</h3>
 * <p>
 * dailyUsed와 monthlyUsed는 결제 승인 시 누적됩니다.
 * 날짜/월이 변경되면 자동으로 초기화됩니다.
 * </p>
 *
 * @see CardType
 * @see CardStatus
 */
@Getter
public class Card {

    // ========================================
    // 핵심 필드
    // ========================================

    private CardId cardId;
    private CardNumber cardNumber;
    private String userId;           // USR-xxx
    private String accountId;        // ACC-xxx (연결된 계좌)
    private CardType cardType;
    private CardStatus status;
    private YearMonth expiryDate;    // MM/YY
    private String cvc;              // 암호화 저장

    // ========================================
    // 한도 필드
    // ========================================

    private Money dailyLimit;        // 일일 한도
    private Money monthlyLimit;      // 월간 한도
    private Money dailyUsed;         // 일일 사용액
    private Money monthlyUsed;       // 월간 사용액
    private LocalDate lastUsedDate;  // 마지막 사용일 (일일 한도 초기화용)
    private YearMonth lastUsedMonth; // 마지막 사용월 (월간 한도 초기화용)

    // ========================================
    // 동시성 제어
    // ========================================

    private Long version;

    // ========================================
    // 감사 필드
    // ========================================

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime deletedAt;
    private String deletedBy;
    private Boolean isDeleted;

    private Card() {}

    // ========================================
    // 생성 메서드
    // ========================================

    public static CardCreateBuilder createBuilder() {
        return new CardCreateBuilder();
    }

    public static CardRestoreBuilder restoreBuilder() {
        return new CardRestoreBuilder();
    }

    // ========================================
    // 상태 확인 메서드
    // ========================================

    public boolean isNew() {
        return this.cardId == null;
    }

    public boolean isActive() {
        return this.status.isActive();
    }

    public boolean isExpired() {
        return YearMonth.now().isAfter(this.expiryDate);
    }

    public boolean canPay() {
        return this.status.canPay() && !isExpired();
    }

    public boolean isDebitCard() {
        return this.cardType.isDebit();
    }

    public boolean isCreditCard() {
        return this.cardType.isCredit();
    }

    // ========================================
    // 한도 관련 메서드
    // ========================================

    /**
     * 결제 가능 여부 검증
     * <p>
     * 카드 상태, 유효기간, 일일/월간 한도를 모두 검증합니다.
     * </p>
     *
     * @param amount 결제 금액
     * @throws CardException 결제 불가 사유 발생 시
     */
    public void validatePayment(Money amount) {
        // 상태 검증
        validateCanPay();

        // 한도 초기화 (날짜/월 변경 시)
        resetLimitsIfNeeded();

        // 일일 한도 검증
        if (!dailyUsed.isWithinLimit(amount, dailyLimit)) {
            throw CardException.dailyLimitExceeded(
                    dailyUsed.amount(), dailyLimit.amount(), amount.amount());
        }

        // 월간 한도 검증
        if (!monthlyUsed.isWithinLimit(amount, monthlyLimit)) {
            throw CardException.monthlyLimitExceeded(
                    monthlyUsed.amount(), monthlyLimit.amount(), amount.amount());
        }
    }

    /**
     * 결제 승인 처리
     * <p>
     * 일일/월간 사용액을 누적합니다.
     * </p>
     *
     * @param amount 결제 금액
     */
    public void recordPayment(Money amount) {
        resetLimitsIfNeeded();

        this.dailyUsed = this.dailyUsed.add(amount);
        this.monthlyUsed = this.monthlyUsed.add(amount);
        this.lastUsedDate = LocalDate.now();
        this.lastUsedMonth = YearMonth.now();
    }

    /**
     * 결제 취소 처리
     * <p>
     * 일일/월간 사용액에서 차감합니다.
     * </p>
     *
     * @param amount 취소 금액
     */
    public void recordCancellation(Money amount) {
        // 한도에서 차감 (0 미만이 되지 않도록)
        if (this.dailyUsed.isGreaterThanOrEqual(amount)) {
            this.dailyUsed = this.dailyUsed.subtract(amount);
        } else {
            this.dailyUsed = Money.ZERO;
        }

        if (this.monthlyUsed.isGreaterThanOrEqual(amount)) {
            this.monthlyUsed = this.monthlyUsed.subtract(amount);
        } else {
            this.monthlyUsed = Money.ZERO;
        }
    }

    /**
     * 날짜/월 변경 시 한도 초기화
     */
    private void resetLimitsIfNeeded() {
        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.now();

        // 일일 한도 초기화
        if (lastUsedDate == null || !lastUsedDate.equals(today)) {
            this.dailyUsed = Money.ZERO;
            this.lastUsedDate = today;
        }

        // 월간 한도 초기화
        if (lastUsedMonth == null || !lastUsedMonth.equals(thisMonth)) {
            this.monthlyUsed = Money.ZERO;
            this.lastUsedMonth = thisMonth;
        }
    }

    // ========================================
    // 상태 변경 메서드
    // ========================================

    /**
     * 카드 활성화
     */
    public void activate() {
        if (this.status.isActive()) {
            throw CardException.cardAlreadyActive(
                    this.cardId != null ? this.cardId.value() : "NEW");
        }
        if (!this.status.canReactivate()) {
            throw CardException.invalidStatusTransition(this.status.name(), "ACTIVE");
        }
        validateNotExpired();
        this.status = CardStatus.ACTIVE;
    }

    /**
     * 카드 비활성화
     */
    public void deactivate() {
        validateStatusTransition(CardStatus.INACTIVE);
        this.status = CardStatus.INACTIVE;
    }

    /**
     * 분실/도난 신고
     */
    public void block() {
        validateStatusTransition(CardStatus.BLOCKED);
        this.status = CardStatus.BLOCKED;
    }

    /**
     * 분실/도난 신고 해제
     */
    public void unblock() {
        if (!this.status.isBlocked()) {
            throw CardException.invalidStatusTransition(this.status.name(), "ACTIVE");
        }
        this.status = CardStatus.ACTIVE;
    }

    /**
     * 카드 해지
     */
    public void terminate() {
        if (!this.status.canTerminate()) {
            throw CardException.invalidStatusTransition(this.status.name(), "TERMINATED");
        }
        this.status = CardStatus.TERMINATED;
    }

    /**
     * 만료 처리
     */
    public void expire() {
        this.status = CardStatus.EXPIRED;
    }

    /**
     * 한도 변경
     */
    public void changeLimits(Money newDailyLimit, Money newMonthlyLimit) {
        if (newDailyLimit != null && newDailyLimit.isPositive()) {
            this.dailyLimit = newDailyLimit;
        }
        if (newMonthlyLimit != null && newMonthlyLimit.isPositive()) {
            this.monthlyLimit = newMonthlyLimit;
        }
    }

    // ========================================
    // Private 검증 메서드
    // ========================================

    private void validateCanPay() {
        if (!canPay()) {
            if (isExpired()) {
                throw CardException.cardExpired(
                        this.cardId != null ? this.cardId.value() : "NEW");
            }
            if (this.status.isBlocked()) {
                throw CardException.cardBlocked(
                        this.cardId != null ? this.cardId.value() : "NEW");
            }
            if (this.status.isTerminated()) {
                throw CardException.cardTerminated(
                        this.cardId != null ? this.cardId.value() : "NEW");
            }
            throw CardException.cardNotActive(
                    this.cardId != null ? this.cardId.value() : "NEW",
                    this.status.name());
        }
    }

    private void validateNotExpired() {
        if (isExpired()) {
            throw CardException.cardExpired(
                    this.cardId != null ? this.cardId.value() : "NEW");
        }
    }

    private void validateStatusTransition(CardStatus target) {
        if (!this.status.canTransitionTo(target)) {
            throw CardException.invalidStatusTransition(this.status.name(), target.name());
        }
    }

    // ========================================
    // Builder 클래스
    // ========================================

    public static class CardCreateBuilder {
        private String userId;
        private String accountId;
        private CardType cardType;
        private Money dailyLimit;
        private Money monthlyLimit;

        public CardCreateBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public CardCreateBuilder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public CardCreateBuilder cardType(CardType cardType) {
            this.cardType = cardType;
            return this;
        }

        public CardCreateBuilder dailyLimit(Money dailyLimit) {
            this.dailyLimit = dailyLimit;
            return this;
        }

        public CardCreateBuilder monthlyLimit(Money monthlyLimit) {
            this.monthlyLimit = monthlyLimit;
            return this;
        }

        public Card build() {
            Card card = new Card();
            card.userId = this.userId;
            card.accountId = this.accountId;
            card.cardType = this.cardType != null ? this.cardType : CardType.DEBIT;
            card.cardNumber = CardNumber.generate();
            card.status = CardStatus.ACTIVE;
            card.expiryDate = YearMonth.now().plusYears(5);  // 5년 후 만료
            card.cvc = generateCvc();

            // 한도 설정 (기본값 또는 지정값)
            card.dailyLimit = this.dailyLimit != null ? this.dailyLimit :
                    Money.of(card.cardType.getDefaultDailyLimit());
            card.monthlyLimit = this.monthlyLimit != null ? this.monthlyLimit :
                    Money.of(card.cardType.getDefaultMonthlyLimit());
            card.dailyUsed = Money.ZERO;
            card.monthlyUsed = Money.ZERO;
            card.lastUsedDate = LocalDate.now();
            card.lastUsedMonth = YearMonth.now();

            card.isDeleted = false;

            return card;
        }

        private String generateCvc() {
            return String.format("%03d", new java.security.SecureRandom().nextInt(1000));
        }
    }

    public static class CardRestoreBuilder {
        private CardId cardId;
        private CardNumber cardNumber;
        private String userId;
        private String accountId;
        private CardType cardType;
        private CardStatus status;
        private YearMonth expiryDate;
        private String cvc;
        private Money dailyLimit;
        private Money monthlyLimit;
        private Money dailyUsed;
        private Money monthlyUsed;
        private LocalDate lastUsedDate;
        private YearMonth lastUsedMonth;
        private Long version;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String createdBy;
        private String updatedBy;
        private LocalDateTime deletedAt;
        private String deletedBy;
        private Boolean isDeleted;

        public CardRestoreBuilder cardId(CardId cardId) { this.cardId = cardId; return this; }
        public CardRestoreBuilder cardNumber(CardNumber cardNumber) { this.cardNumber = cardNumber; return this; }
        public CardRestoreBuilder userId(String userId) { this.userId = userId; return this; }
        public CardRestoreBuilder accountId(String accountId) { this.accountId = accountId; return this; }
        public CardRestoreBuilder cardType(CardType cardType) { this.cardType = cardType; return this; }
        public CardRestoreBuilder status(CardStatus status) { this.status = status; return this; }
        public CardRestoreBuilder expiryDate(YearMonth expiryDate) { this.expiryDate = expiryDate; return this; }
        public CardRestoreBuilder cvc(String cvc) { this.cvc = cvc; return this; }
        public CardRestoreBuilder dailyLimit(Money dailyLimit) { this.dailyLimit = dailyLimit; return this; }
        public CardRestoreBuilder monthlyLimit(Money monthlyLimit) { this.monthlyLimit = monthlyLimit; return this; }
        public CardRestoreBuilder dailyUsed(Money dailyUsed) { this.dailyUsed = dailyUsed; return this; }
        public CardRestoreBuilder monthlyUsed(Money monthlyUsed) { this.monthlyUsed = monthlyUsed; return this; }
        public CardRestoreBuilder lastUsedDate(LocalDate lastUsedDate) { this.lastUsedDate = lastUsedDate; return this; }
        public CardRestoreBuilder lastUsedMonth(YearMonth lastUsedMonth) { this.lastUsedMonth = lastUsedMonth; return this; }
        public CardRestoreBuilder version(Long version) { this.version = version; return this; }
        public CardRestoreBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public CardRestoreBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public CardRestoreBuilder createdBy(String createdBy) { this.createdBy = createdBy; return this; }
        public CardRestoreBuilder updatedBy(String updatedBy) { this.updatedBy = updatedBy; return this; }
        public CardRestoreBuilder deletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; return this; }
        public CardRestoreBuilder deletedBy(String deletedBy) { this.deletedBy = deletedBy; return this; }
        public CardRestoreBuilder isDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; return this; }

        public Card build() {
            Card card = new Card();
            card.cardId = this.cardId;
            card.cardNumber = this.cardNumber;
            card.userId = this.userId;
            card.accountId = this.accountId;
            card.cardType = this.cardType;
            card.status = this.status;
            card.expiryDate = this.expiryDate;
            card.cvc = this.cvc;
            card.dailyLimit = this.dailyLimit;
            card.monthlyLimit = this.monthlyLimit;
            card.dailyUsed = this.dailyUsed;
            card.monthlyUsed = this.monthlyUsed;
            card.lastUsedDate = this.lastUsedDate;
            card.lastUsedMonth = this.lastUsedMonth;
            card.version = this.version;
            card.createdAt = this.createdAt;
            card.updatedAt = this.updatedAt;
            card.createdBy = this.createdBy;
            card.updatedBy = this.updatedBy;
            card.deletedAt = this.deletedAt;
            card.deletedBy = this.deletedBy;
            card.isDeleted = this.isDeleted;
            return card;
        }
    }
}