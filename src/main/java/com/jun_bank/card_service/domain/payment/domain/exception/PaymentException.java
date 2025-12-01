package com.jun_bank.card_service.domain.payment.domain.exception;

import com.jun_bank.common_lib.exception.BusinessException;

import java.math.BigDecimal;

/**
 * 결제 도메인 예외
 * <p>
 * 결제 처리 관련 비즈니스 로직에서 발생하는 예외를 처리합니다.
 *
 * @see PaymentErrorCode
 * @see BusinessException
 */
public class PaymentException extends BusinessException {

    public PaymentException(PaymentErrorCode errorCode) {
        super(errorCode);
    }

    public PaymentException(PaymentErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }

    // ========================================
    // 유효성 검증 관련 팩토리 메서드
    // ========================================

    public static PaymentException invalidPaymentIdFormat(String id) {
        return new PaymentException(PaymentErrorCode.INVALID_PAYMENT_ID_FORMAT, "id=" + id);
    }

    public static PaymentException invalidAmount(BigDecimal amount) {
        return new PaymentException(PaymentErrorCode.INVALID_AMOUNT,
                "amount=" + (amount != null ? amount.toPlainString() : "null"));
    }

    public static PaymentException minimumPaymentAmount(BigDecimal amount) {
        return new PaymentException(PaymentErrorCode.MINIMUM_PAYMENT_AMOUNT,
                "amount=" + (amount != null ? amount.toPlainString() : "null"));
    }

    // ========================================
    // 조회 관련 팩토리 메서드
    // ========================================

    public static PaymentException paymentNotFound(String paymentId) {
        return new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND, "paymentId=" + paymentId);
    }

    // ========================================
    // 결제 처리 관련 팩토리 메서드
    // ========================================

    public static PaymentException paymentAlreadyApproved(String paymentId) {
        return new PaymentException(PaymentErrorCode.PAYMENT_ALREADY_APPROVED, "paymentId=" + paymentId);
    }

    public static PaymentException paymentAlreadyCancelled(String paymentId) {
        return new PaymentException(PaymentErrorCode.PAYMENT_ALREADY_CANCELLED, "paymentId=" + paymentId);
    }

    public static PaymentException paymentCannotCancel(String paymentId, String status) {
        return new PaymentException(PaymentErrorCode.PAYMENT_CANNOT_CANCEL,
                String.format("paymentId=%s, status=%s", paymentId, status));
    }

    public static PaymentException insufficientBalance(BigDecimal balance, BigDecimal amount) {
        return new PaymentException(PaymentErrorCode.INSUFFICIENT_BALANCE,
                String.format("잔액=%s, 요청=%s", balance.toPlainString(), amount.toPlainString()));
    }

    public static PaymentException paymentDeclined(String reason) {
        return new PaymentException(PaymentErrorCode.PAYMENT_DECLINED, "reason=" + reason);
    }

    // ========================================
    // 상태 관련 팩토리 메서드
    // ========================================

    public static PaymentException invalidStatusTransition(String from, String to) {
        return new PaymentException(PaymentErrorCode.INVALID_STATUS_TRANSITION,
                String.format("from=%s, to=%s", from, to));
    }

    // ========================================
    // 외부 시스템 관련 팩토리 메서드
    // ========================================

    public static PaymentException externalApiError(String reason) {
        return new PaymentException(PaymentErrorCode.EXTERNAL_API_ERROR, "reason=" + reason);
    }

    public static PaymentException circuitBreakerOpen(String serviceName) {
        return new PaymentException(PaymentErrorCode.CIRCUIT_BREAKER_OPEN, "service=" + serviceName);
    }

    public static PaymentException rateLimitExceeded() {
        return new PaymentException(PaymentErrorCode.RATE_LIMIT_EXCEEDED);
    }
}