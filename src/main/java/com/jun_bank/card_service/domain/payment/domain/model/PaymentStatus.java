package com.jun_bank.card_service.domain.payment.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * 결제 상태
 * <p>
 * 카드 결제의 처리 상태를 정의합니다.
 *
 * <h3>상태 전이 규칙:</h3>
 * <pre>
 *              승인 성공
 * ┌─────────┐ ─────────▶ ┌──────────┐ ─── 취소 ──▶ ┌───────────┐
 * │ PENDING │            │ APPROVED │              │ CANCELLED │
 * └─────────┘            └──────────┘              └───────────┘
 *     │                       │
 *     │ 승인 거절             │ 환불
 *     └──────────▶ ┌──────────┘──────────▶ ┌──────────┐
 *                  │ DECLINED │            │ REFUNDED │
 *                  └──────────┘            └──────────┘
 * </pre>
 *
 * @see Payment
 */
public enum PaymentStatus {

    /**
     * 처리 중
     * <p>
     * 결제 요청이 접수되어 승인 대기 중인 상태.
     * 외부 카드사 승인 또는 계좌 차감 진행 중.
     * </p>
     */
    PENDING("처리중", false, false),

    /**
     * 승인
     * <p>
     * 결제가 정상적으로 승인된 상태.
     * 취소 또는 환불이 가능합니다.
     * </p>
     */
    APPROVED("승인", true, true),

    /**
     * 거절
     * <p>
     * 결제가 거절된 최종 상태.
     * 한도 초과, 잔액 부족, 카드 상태 이상 등의 사유.
     * </p>
     */
    DECLINED("거절", true, false),

    /**
     * 취소
     * <p>
     * 결제가 취소된 최종 상태.
     * 승인 당일에만 취소 가능 (매입 전).
     * </p>
     */
    CANCELLED("취소", true, false),

    /**
     * 환불
     * <p>
     * 결제가 환불된 최종 상태.
     * 승인 익일 이후 환불 처리 (매입 후).
     * </p>
     */
    REFUNDED("환불", true, false);

    private final String description;
    private final boolean isFinal;       // 최종 상태 여부
    private final boolean canCancel;     // 취소/환불 가능 여부

    PaymentStatus(String description, boolean isFinal, boolean canCancel) {
        this.description = description;
        this.isFinal = isFinal;
        this.canCancel = canCancel;
    }

    /**
     * 상태 설명 반환
     *
     * @return 한글 설명
     */
    public String getDescription() {
        return description;
    }

    /**
     * 최종 상태 여부 확인
     *
     * @return 최종 상태이면 true
     */
    public boolean isFinal() {
        return isFinal;
    }

    /**
     * 취소/환불 가능 여부 확인
     *
     * @return 취소/환불 가능하면 true
     */
    public boolean canCancel() {
        return canCancel;
    }

    /**
     * 처리 중 여부 확인
     *
     * @return PENDING이면 true
     */
    public boolean isPending() {
        return this == PENDING;
    }

    /**
     * 승인 상태 여부 확인
     *
     * @return APPROVED이면 true
     */
    public boolean isApproved() {
        return this == APPROVED;
    }

    /**
     * 거절 상태 여부 확인
     *
     * @return DECLINED이면 true
     */
    public boolean isDeclined() {
        return this == DECLINED;
    }

    /**
     * 취소/환불 완료 여부 확인
     *
     * @return CANCELLED 또는 REFUNDED이면 true
     */
    public boolean isCancelledOrRefunded() {
        return this == CANCELLED || this == REFUNDED;
    }

    /**
     * 성공적으로 완료된 상태 여부 확인
     * <p>
     * 정상 승인 후 취소/환불되지 않은 상태.
     * </p>
     *
     * @return APPROVED이면 true
     */
    public boolean isSuccessful() {
        return this == APPROVED;
    }

    /**
     * 특정 상태로 전환 가능 여부 확인
     *
     * @param target 전환하려는 상태
     * @return 전환 가능하면 true
     */
    public boolean canTransitionTo(PaymentStatus target) {
        if (this == target) {
            return false;
        }
        return getAllowedTransitions().contains(target);
    }

    /**
     * 현재 상태에서 전환 가능한 상태 목록 반환
     *
     * @return 전환 가능한 상태 Set
     */
    public Set<PaymentStatus> getAllowedTransitions() {
        return switch (this) {
            case PENDING -> EnumSet.of(APPROVED, DECLINED);
            case APPROVED -> EnumSet.of(CANCELLED, REFUNDED);
            case DECLINED, CANCELLED, REFUNDED -> EnumSet.noneOf(PaymentStatus.class);  // 최종 상태
        };
    }
}