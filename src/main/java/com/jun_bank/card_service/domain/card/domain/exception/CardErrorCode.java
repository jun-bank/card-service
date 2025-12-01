package com.jun_bank.card_service.domain.card.domain.exception;

import com.jun_bank.common_lib.exception.ErrorCode;

/**
 * 카드 도메인 에러 코드
 * <p>
 * 카드 발급 관련 비즈니스 로직에서 발생할 수 있는 모든 에러를 정의합니다.
 *
 * <h3>에러 코드 체계:</h3>
 * <ul>
 *   <li>CRD_001~009: 유효성 검증 오류 (400)</li>
 *   <li>CRD_010~019: 조회 오류 (404)</li>
 *   <li>CRD_020~029: 한도 오류 (400)</li>
 *   <li>CRD_030~039: 상태 오류 (422)</li>
 *   <li>CRD_050~059: 외부 시스템 오류 (503)</li>
 * </ul>
 *
 * @see CardException
 * @see ErrorCode
 */
public enum CardErrorCode implements ErrorCode {

    // ========================================
    // 유효성 검증 오류 (400 Bad Request)
    // ========================================

    /**
     * 유효하지 않은 카드 ID 형식
     */
    INVALID_CARD_ID_FORMAT("CRD_001", "유효하지 않은 카드 ID 형식입니다", 400),

    /**
     * 유효하지 않은 카드번호 형식
     */
    INVALID_CARD_NUMBER_FORMAT("CRD_003", "유효하지 않은 카드번호 형식입니다", 400),

    /**
     * 유효하지 않은 CVC
     */
    INVALID_CVC("CRD_004", "유효하지 않은 CVC입니다", 400),

    /**
     * 유효하지 않은 한도 설정
     */
    INVALID_LIMIT("CRD_006", "유효하지 않은 한도 설정입니다", 400),

    /**
     * 유효하지 않은 유효기간
     */
    INVALID_EXPIRY_DATE("CRD_007", "유효하지 않은 유효기간입니다", 400),

    // ========================================
    // 조회 오류 (404 Not Found)
    // ========================================

    /**
     * 카드를 찾을 수 없음
     */
    CARD_NOT_FOUND("CRD_010", "카드를 찾을 수 없습니다", 404),

    // ========================================
    // 한도 오류 (400 Bad Request)
    // ========================================

    /**
     * 일일 한도 초과
     */
    DAILY_LIMIT_EXCEEDED("CRD_020", "일일 사용 한도를 초과했습니다", 400),

    /**
     * 월간 한도 초과
     */
    MONTHLY_LIMIT_EXCEEDED("CRD_021", "월간 사용 한도를 초과했습니다", 400),

    /**
     * 1회 결제 한도 초과
     */
    SINGLE_PAYMENT_LIMIT_EXCEEDED("CRD_022", "1회 결제 한도를 초과했습니다", 400),

    // ========================================
    // 상태 오류 (422 Unprocessable Entity)
    // ========================================

    /**
     * 비활성 카드
     */
    CARD_NOT_ACTIVE("CRD_030", "비활성 상태의 카드입니다", 422),

    /**
     * 분실/도난 신고된 카드
     */
    CARD_BLOCKED("CRD_031", "분실/도난 신고된 카드입니다", 422),

    /**
     * 만료된 카드
     */
    CARD_EXPIRED("CRD_032", "만료된 카드입니다", 422),

    /**
     * 해지된 카드
     */
    CARD_TERMINATED("CRD_033", "해지된 카드입니다", 422),

    /**
     * 이미 활성화된 카드
     */
    CARD_ALREADY_ACTIVE("CRD_034", "이미 활성화된 카드입니다", 422),

    /**
     * 허용되지 않은 상태 전이
     */
    INVALID_STATUS_TRANSITION("CRD_035", "허용되지 않은 상태 변경입니다", 422),

    /**
     * 본인 카드가 아님
     */
    NOT_CARD_OWNER("CRD_036", "본인 소유 카드가 아닙니다", 400),

    // ========================================
    // 외부 시스템 오류 (503 Service Unavailable)
    // ========================================

    /**
     * 외부 카드사 API 오류
     */
    EXTERNAL_API_ERROR("CRD_050", "카드사 시스템 오류가 발생했습니다", 503),

    /**
     * 회로 차단기 열림
     */
    CIRCUIT_BREAKER_OPEN("CRD_051", "일시적으로 서비스를 이용할 수 없습니다. 잠시 후 다시 시도해주세요", 503),

    /**
     * 요청 제한 초과
     */
    RATE_LIMIT_EXCEEDED("CRD_052", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요", 429),

    /**
     * 계좌 서비스 오류
     */
    ACCOUNT_SERVICE_ERROR("CRD_053", "계좌 서비스 오류가 발생했습니다", 503);

    private final String code;
    private final String message;
    private final int status;

    CardErrorCode(String code, String message, int status) {
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