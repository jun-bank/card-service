package com.jun_bank.card_service.domain.card.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

/**
 * 카드 유형 Enum
 * <p>
 * 각 카드 유형별 기본 정책을 정의합니다.
 * 한도, 계좌 연동 필요 여부, 신용 기능 여부 등을 포함합니다.
 * </p>
 *
 * <h3>카드 유형:</h3>
 * <ul>
 *   <li>DEBIT: 체크카드 - 계좌 연동 필수, 즉시 출금</li>
 *   <li>CREDIT: 신용카드 - 후불 결제, 신용 한도</li>
 *   <li>PREPAID: 선불카드 - 충전 후 사용</li>
 * </ul>
 *
 * @see Card
 */
@Getter
@RequiredArgsConstructor
public enum CardType {

    /**
     * 체크카드
     * <p>
     * 계좌와 연동되어 결제 시 즉시 출금됩니다.
     * 잔액 범위 내에서만 결제 가능합니다.
     * </p>
     */
    DEBIT(
            "체크카드",
            "DEB",
            new BigDecimal("5000000"),   // 일일 한도 500만원
            new BigDecimal("50000000"),  // 월간 한도 5000만원
            new BigDecimal("3000000"),   // 건당 한도 300만원
            true,   // 계좌 연동 필요
            false   // 신용 기능 없음
    ),

    /**
     * 신용카드
     * <p>
     * 후불 결제 방식으로, 신용 한도 내에서 결제합니다.
     * 결제대금은 익월에 청구됩니다.
     * </p>
     */
    CREDIT(
            "신용카드",
            "CRD",
            new BigDecimal("10000000"),  // 일일 한도 1000만원
            new BigDecimal("100000000"), // 월간 한도 1억원
            new BigDecimal("5000000"),   // 건당 한도 500만원
            false,  // 계좌 연동 선택
            true    // 신용 기능 있음
    ),

    /**
     * 선불카드
     * <p>
     * 미리 충전한 금액 범위 내에서 결제합니다.
     * 충전 금액이 소진되면 재충전이 필요합니다.
     * </p>
     */
    PREPAID(
            "선불카드",
            "PRE",
            new BigDecimal("1000000"),   // 일일 한도 100만원
            new BigDecimal("5000000"),   // 월간 한도 500만원
            new BigDecimal("500000"),    // 건당 한도 50만원
            false,  // 계좌 연동 없음
            false   // 신용 기능 없음
    );

    /** 표시명 */
    private final String displayName;

    /** 코드 (DB 저장용) */
    private final String code;

    /** 기본 일일 한도 */
    private final BigDecimal defaultDailyLimit;

    /** 기본 월간 한도 */
    private final BigDecimal defaultMonthlyLimit;

    /** 기본 건당 한도 */
    private final BigDecimal defaultSingleLimit;

    /** 계좌 연동 필수 여부 */
    private final boolean requiresAccount;

    /** 신용 기능 여부 */
    private final boolean hasCredit;

    /**
     * 코드로 CardType 찾기
     *
     * @param code 카드 유형 코드
     * @return CardType
     * @throws IllegalArgumentException 알 수 없는 코드인 경우
     */
    public static CardType fromCode(String code) {
        for (CardType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown CardType code: " + code);
    }

    /**
     * 체크카드 여부 확인
     *
     * @return 체크카드이면 true
     */
    public boolean isDebit() {
        return this == DEBIT;
    }

    /**
     * 신용카드 여부 확인
     *
     * @return 신용카드이면 true
     */
    public boolean isCredit() {
        return this == CREDIT;
    }

    /**
     * 선불카드 여부 확인
     *
     * @return 선불카드이면 true
     */
    public boolean isPrepaid() {
        return this == PREPAID;
    }

    /**
     * 즉시 출금 필요 여부 확인
     * <p>
     * 체크카드와 선불카드는 결제 시 즉시 잔액에서 차감됩니다.
     * </p>
     *
     * @return 즉시 출금이 필요하면 true
     */
    public boolean requiresImmediateDebit() {
        return this == DEBIT || this == PREPAID;
    }
}