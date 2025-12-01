package com.jun_bank.card_service.domain.card.domain.exception;

import com.jun_bank.common_lib.exception.BusinessException;

import java.math.BigDecimal;

/**
 * 카드 도메인 예외
 * <p>
 * 카드 발급 관련 비즈니스 로직에서 발생하는 예외를 처리합니다.
 *
 * @see CardErrorCode
 * @see BusinessException
 */
public class CardException extends BusinessException {

    public CardException(CardErrorCode errorCode) {
        super(errorCode);
    }

    public CardException(CardErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }

    // ========================================
    // 유효성 검증 관련 팩토리 메서드
    // ========================================

    public static CardException invalidCardIdFormat(String id) {
        return new CardException(CardErrorCode.INVALID_CARD_ID_FORMAT, "id=" + id);
    }

    public static CardException invalidCardNumberFormat(String cardNumber) {
        return new CardException(CardErrorCode.INVALID_CARD_NUMBER_FORMAT,
                "cardNumber=" + (cardNumber != null ? maskCardNumber(cardNumber) : "null"));
    }

    public static CardException invalidCvc() {
        return new CardException(CardErrorCode.INVALID_CVC);
    }

    public static CardException invalidLimit(BigDecimal limit) {
        return new CardException(CardErrorCode.INVALID_LIMIT,
                "limit=" + (limit != null ? limit.toPlainString() : "null"));
    }

    public static CardException invalidExpiryDate(String expiryDate) {
        return new CardException(CardErrorCode.INVALID_EXPIRY_DATE, "expiryDate=" + expiryDate);
    }

    // ========================================
    // 조회 관련 팩토리 메서드
    // ========================================

    public static CardException cardNotFound(String cardId) {
        return new CardException(CardErrorCode.CARD_NOT_FOUND, "cardId=" + cardId);
    }

    // ========================================
    // 한도 관련 팩토리 메서드
    // ========================================

    public static CardException dailyLimitExceeded(BigDecimal used, BigDecimal limit, BigDecimal requested) {
        return new CardException(CardErrorCode.DAILY_LIMIT_EXCEEDED,
                String.format("사용=%s, 한도=%s, 요청=%s",
                        used.toPlainString(), limit.toPlainString(), requested.toPlainString()));
    }

    public static CardException monthlyLimitExceeded(BigDecimal used, BigDecimal limit, BigDecimal requested) {
        return new CardException(CardErrorCode.MONTHLY_LIMIT_EXCEEDED,
                String.format("사용=%s, 한도=%s, 요청=%s",
                        used.toPlainString(), limit.toPlainString(), requested.toPlainString()));
    }

    public static CardException singlePaymentLimitExceeded(BigDecimal amount, BigDecimal maxAmount) {
        return new CardException(CardErrorCode.SINGLE_PAYMENT_LIMIT_EXCEEDED,
                String.format("요청=%s, 한도=%s", amount.toPlainString(), maxAmount.toPlainString()));
    }

    // ========================================
    // 상태 관련 팩토리 메서드
    // ========================================

    public static CardException cardNotActive(String cardId, String status) {
        return new CardException(CardErrorCode.CARD_NOT_ACTIVE,
                String.format("cardId=%s, status=%s", cardId, status));
    }

    public static CardException cardBlocked(String cardId) {
        return new CardException(CardErrorCode.CARD_BLOCKED, "cardId=" + cardId);
    }

    public static CardException cardExpired(String cardId) {
        return new CardException(CardErrorCode.CARD_EXPIRED, "cardId=" + cardId);
    }

    public static CardException cardTerminated(String cardId) {
        return new CardException(CardErrorCode.CARD_TERMINATED, "cardId=" + cardId);
    }

    public static CardException cardAlreadyActive(String cardId) {
        return new CardException(CardErrorCode.CARD_ALREADY_ACTIVE, "cardId=" + cardId);
    }

    public static CardException invalidStatusTransition(String from, String to) {
        return new CardException(CardErrorCode.INVALID_STATUS_TRANSITION,
                String.format("from=%s, to=%s", from, to));
    }

    public static CardException notCardOwner(String cardId) {
        return new CardException(CardErrorCode.NOT_CARD_OWNER, "cardId=" + cardId);
    }

    // ========================================
    // 외부 시스템 관련 팩토리 메서드
    // ========================================

    public static CardException externalApiError(String reason) {
        return new CardException(CardErrorCode.EXTERNAL_API_ERROR, "reason=" + reason);
    }

    public static CardException circuitBreakerOpen(String serviceName) {
        return new CardException(CardErrorCode.CIRCUIT_BREAKER_OPEN, "service=" + serviceName);
    }

    public static CardException rateLimitExceeded() {
        return new CardException(CardErrorCode.RATE_LIMIT_EXCEEDED);
    }

    public static CardException accountServiceError(String reason) {
        return new CardException(CardErrorCode.ACCOUNT_SERVICE_ERROR, "reason=" + reason);
    }

    // ========================================
    // 유틸 메서드
    // ========================================

    private static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 8) {
            return "****";
        }
        return cardNumber.substring(0, 4) + "-****-****-" + cardNumber.substring(cardNumber.length() - 4);
    }
}