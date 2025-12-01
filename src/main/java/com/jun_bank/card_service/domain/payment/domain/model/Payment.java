package com.jun_bank.card_service.domain.payment.domain.model;

import com.jun_bank.card_service.domain.payment.domain.exception.PaymentException;
import com.jun_bank.card_service.domain.payment.domain.model.vo.Money;
import com.jun_bank.card_service.domain.payment.domain.model.vo.PaymentId;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 결제 도메인 모델
 * <p>
 * 카드 결제의 비즈니스 로직을 담당합니다.
 *
 * <h3>결제 흐름:</h3>
 * <ol>
 *   <li>결제 요청 → PENDING 상태로 생성</li>
 *   <li>승인 처리 → APPROVED 또는 DECLINED</li>
 *   <li>취소/환불 → CANCELLED 또는 REFUNDED</li>
 * </ol>
 *
 * <h3>취소 vs 환불:</h3>
 * <ul>
 *   <li>취소: 승인 당일, 매입 전 취소</li>
 *   <li>환불: 승인 익일 이후, 매입 후 환불</li>
 * </ul>
 *
 * @see PaymentStatus
 */
@Getter
public class Payment {

    // ========================================
    // 핵심 필드
    // ========================================

    private PaymentId paymentId;
    private String cardId;                  // CRD-xxx
    private String merchantName;            // 가맹점명
    private String merchantId;              // 가맹점 ID
    private Money amount;                   // 결제 금액
    private PaymentStatus status;
    private String approvalNumber;          // 승인 번호
    private String failReason;              // 실패/거절 사유
    private String cancelReason;            // 취소/환불 사유
    private String idempotencyKey;          // 멱등성 키

    // ========================================
    // 시간 필드
    // ========================================

    private LocalDateTime requestedAt;      // 결제 요청 시간
    private LocalDateTime approvedAt;       // 승인 시간
    private LocalDateTime cancelledAt;      // 취소/환불 시간

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

    private Payment() {}

    // ========================================
    // 생성 메서드
    // ========================================

    public static PaymentCreateBuilder createBuilder() {
        return new PaymentCreateBuilder();
    }

    public static PaymentRestoreBuilder restoreBuilder() {
        return new PaymentRestoreBuilder();
    }

    // ========================================
    // 상태 확인 메서드
    // ========================================

    public boolean isNew() {
        return this.paymentId == null;
    }

    public boolean isPending() {
        return this.status.isPending();
    }

    public boolean isApproved() {
        return this.status.isApproved();
    }

    public boolean isDeclined() {
        return this.status.isDeclined();
    }

    public boolean canCancel() {
        return this.status.canCancel();
    }

    public boolean isFinal() {
        return this.status.isFinal();
    }

    // ========================================
    // 비즈니스 메서드
    // ========================================

    /**
     * 결제 승인 처리
     *
     * @param approvalNumber 승인 번호
     */
    public void approve(String approvalNumber) {
        validateCanProcess();
        validateStatusTransition(PaymentStatus.APPROVED);

        this.status = PaymentStatus.APPROVED;
        this.approvalNumber = approvalNumber;
        this.approvedAt = LocalDateTime.now();
    }

    /**
     * 결제 거절 처리
     *
     * @param reason 거절 사유
     */
    public void decline(String reason) {
        validateCanProcess();
        validateStatusTransition(PaymentStatus.DECLINED);

        this.status = PaymentStatus.DECLINED;
        this.failReason = reason;
    }

    /**
     * 결제 취소 처리
     * <p>
     * 승인 당일에만 가능합니다 (매입 전).
     * </p>
     *
     * @param reason 취소 사유
     */
    public void cancel(String reason) {
        if (!canCancel()) {
            throw PaymentException.paymentCannotCancel(
                    this.paymentId != null ? this.paymentId.value() : "NEW",
                    this.status.name());
        }
        validateStatusTransition(PaymentStatus.CANCELLED);

        this.status = PaymentStatus.CANCELLED;
        this.cancelReason = reason;
        this.cancelledAt = LocalDateTime.now();
    }

    /**
     * 결제 환불 처리
     * <p>
     * 승인 익일 이후에 가능합니다 (매입 후).
     * </p>
     *
     * @param reason 환불 사유
     */
    public void refund(String reason) {
        if (!canCancel()) {
            throw PaymentException.paymentCannotCancel(
                    this.paymentId != null ? this.paymentId.value() : "NEW",
                    this.status.name());
        }
        validateStatusTransition(PaymentStatus.REFUNDED);

        this.status = PaymentStatus.REFUNDED;
        this.cancelReason = reason;
        this.cancelledAt = LocalDateTime.now();
    }

    // ========================================
    // Private 검증 메서드
    // ========================================

    private void validateCanProcess() {
        if (!this.status.isPending()) {
            if (this.status.isApproved()) {
                throw PaymentException.paymentAlreadyApproved(
                        this.paymentId != null ? this.paymentId.value() : "NEW");
            }
            throw PaymentException.paymentCannotCancel(
                    this.paymentId != null ? this.paymentId.value() : "NEW",
                    this.status.name());
        }
    }

    private void validateStatusTransition(PaymentStatus target) {
        if (!this.status.canTransitionTo(target)) {
            throw PaymentException.invalidStatusTransition(this.status.name(), target.name());
        }
    }

    // ========================================
    // Builder 클래스
    // ========================================

    public static class PaymentCreateBuilder {
        private String cardId;
        private String merchantName;
        private String merchantId;
        private Money amount;
        private String idempotencyKey;

        public PaymentCreateBuilder cardId(String cardId) {
            this.cardId = cardId;
            return this;
        }

        public PaymentCreateBuilder merchantName(String merchantName) {
            this.merchantName = merchantName;
            return this;
        }

        public PaymentCreateBuilder merchantId(String merchantId) {
            this.merchantId = merchantId;
            return this;
        }

        public PaymentCreateBuilder amount(Money amount) {
            this.amount = amount;
            return this;
        }

        public PaymentCreateBuilder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public Payment build() {
            if (amount == null || !amount.isPositive()) {
                throw PaymentException.invalidAmount(amount != null ? amount.amount() : null);
            }

            Payment payment = new Payment();
            payment.cardId = this.cardId;
            payment.merchantName = this.merchantName;
            payment.merchantId = this.merchantId;
            payment.amount = this.amount;
            payment.idempotencyKey = this.idempotencyKey;
            payment.status = PaymentStatus.PENDING;
            payment.requestedAt = LocalDateTime.now();

            return payment;
        }
    }

    public static class PaymentRestoreBuilder {
        private PaymentId paymentId;
        private String cardId;
        private String merchantName;
        private String merchantId;
        private Money amount;
        private PaymentStatus status;
        private String approvalNumber;
        private String failReason;
        private String cancelReason;
        private String idempotencyKey;
        private LocalDateTime requestedAt;
        private LocalDateTime approvedAt;
        private LocalDateTime cancelledAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String createdBy;
        private String updatedBy;
        private LocalDateTime deletedAt;
        private String deletedBy;
        private Boolean isDeleted;

        public PaymentRestoreBuilder paymentId(PaymentId paymentId) { this.paymentId = paymentId; return this; }
        public PaymentRestoreBuilder cardId(String cardId) { this.cardId = cardId; return this; }
        public PaymentRestoreBuilder merchantName(String merchantName) { this.merchantName = merchantName; return this; }
        public PaymentRestoreBuilder merchantId(String merchantId) { this.merchantId = merchantId; return this; }
        public PaymentRestoreBuilder amount(Money amount) { this.amount = amount; return this; }
        public PaymentRestoreBuilder status(PaymentStatus status) { this.status = status; return this; }
        public PaymentRestoreBuilder approvalNumber(String approvalNumber) { this.approvalNumber = approvalNumber; return this; }
        public PaymentRestoreBuilder failReason(String failReason) { this.failReason = failReason; return this; }
        public PaymentRestoreBuilder cancelReason(String cancelReason) { this.cancelReason = cancelReason; return this; }
        public PaymentRestoreBuilder idempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; return this; }
        public PaymentRestoreBuilder requestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; return this; }
        public PaymentRestoreBuilder approvedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; return this; }
        public PaymentRestoreBuilder cancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; return this; }
        public PaymentRestoreBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public PaymentRestoreBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public PaymentRestoreBuilder createdBy(String createdBy) { this.createdBy = createdBy; return this; }
        public PaymentRestoreBuilder updatedBy(String updatedBy) { this.updatedBy = updatedBy; return this; }
        public PaymentRestoreBuilder deletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; return this; }
        public PaymentRestoreBuilder deletedBy(String deletedBy) { this.deletedBy = deletedBy; return this; }
        public PaymentRestoreBuilder isDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; return this; }

        public Payment build() {
            Payment payment = new Payment();
            payment.paymentId = this.paymentId;
            payment.cardId = this.cardId;
            payment.merchantName = this.merchantName;
            payment.merchantId = this.merchantId;
            payment.amount = this.amount;
            payment.status = this.status;
            payment.approvalNumber = this.approvalNumber;
            payment.failReason = this.failReason;
            payment.cancelReason = this.cancelReason;
            payment.idempotencyKey = this.idempotencyKey;
            payment.requestedAt = this.requestedAt;
            payment.approvedAt = this.approvedAt;
            payment.cancelledAt = this.cancelledAt;
            payment.createdAt = this.createdAt;
            payment.updatedAt = this.updatedAt;
            payment.createdBy = this.createdBy;
            payment.updatedBy = this.updatedBy;
            payment.deletedAt = this.deletedAt;
            payment.deletedBy = this.deletedBy;
            payment.isDeleted = this.isDeleted;
            return payment;
        }
    }
}