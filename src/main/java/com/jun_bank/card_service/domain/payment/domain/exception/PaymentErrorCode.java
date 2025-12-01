package com.jun_bank.card_service.domain.payment.domain.exception;

import com.jun_bank.common_lib.exception.ErrorCode;

/**
 * 결제 도메인 에러 코드
 * <p>
 * 결제 처리 관련 비즈니스 로직에서 발생할 수 있는 모든 에러를 정의합니다.
 *
 * <h3>에러 코드 체계:</h3>
 * <ul>
 *   <li>PAY_001~009: 유효성 검증 오류 (400)</li>
 *   <li>PAY_010~019: 조회 오류 (404)</li>
 *   <li>PAY_020~029: 결제 처리 오류 (400)</li>
 *   <li>PAY_030~039: 상태 오류 (422)</li>
 *   <li>PAY_050~059: 외부 시스템 오류 (503)</li>
 * </ul>
 *
 * @see PaymentException
 * @see ErrorCode
 */
public enum PaymentErrorCode implements ErrorCode {

    // ========================================
    // 유효성 검증 오류 (400 Bad Request)
    // ========================================

    /**
     * 유효하지 않은 결제 ID 형식
     */
    INVALID_PAYMENT_ID_FORMAT("PAY_001", "유효하지 않은 결제 ID 형식입니다", 400),

    /**
     * 유효하지 않은 금액
     */
    INVALID_AMOUNT("PAY_002", "유효하지 않은 금액입니다", 400),

    /**
     * 최소 결제 금액 미달
     */
    MINIMUM_PAYMENT_AMOUNT("PAY_003", "최소 결제 금액은 100원입니다", 400),

    // ========================================
    // 조회 오류 (404 Not Found)
    // ========================================

    /**
     * 결제를 찾을 수 없음
     */
    PAYMENT_NOT_FOUND("PAY_010", "결제 내역을 찾을 수 없습니다", 404),

    // ========================================
    // 결제 처리 오류 (400 Bad Request)
    // ========================================

    /**
     * 이미 승인된 결제
     */
    PAYMENT_ALREADY_APPROVED("PAY_020", "이미 승인된 결제입니다", 400),

    /**
     * 이미 취소된 결제
     */
    PAYMENT_ALREADY_CANCELLED("PAY_021", "이미 취소된 결제입니다", 400),

    /**
     * 취소 불가 상태
     */
    PAYMENT_CANNOT_CANCEL("PAY_022", "취소할 수 없는 결제 상태입니다", 400),

    /**
     * 잔액 부족 (체크카드)
     */
    INSUFFICIENT_BALANCE("PAY_023", "계좌 잔액이 부족합니다", 400),

    /**
     * 결제 거절
     */
    PAYMENT_DECLINED("PAY_024", "결제가 거절되었습니다", 400),

    // ========================================
    // 상태 오류 (422 Unprocessable Entity)
    // ========================================

    /**
     * 허용되지 않은 상태 전이
     */
    INVALID_STATUS_TRANSITION("PAY_030", "허용되지 않은 상태 변경입니다", 422),

    // ========================================
    // 외부 시스템 오류 (503 Service Unavailable)
    // ========================================

    /**
     * 외부 결제 API 오류
     */
    EXTERNAL_API_ERROR("PAY_050", "결제 시스템 오류가 발생했습니다", 503),

    /**
     * 회로 차단기 열림
     */
    CIRCUIT_BREAKER_OPEN("PAY_051", "일시적으로 서비스를 이용할 수 없습니다. 잠시 후 다시 시도해주세요", 503),

    /**
     * 요청 제한 초과
     */
    RATE_LIMIT_EXCEEDED("PAY_052", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요", 429);

    private final String code;
    private final String message;
    private final int status;

    PaymentErrorCode(String code, String message, int status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getStatus() {
        return status;
    }
}