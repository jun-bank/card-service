package com.jun_bank.card_service.domain.card.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * 카드 상태
 * <p>
 * 카드의 현재 상태를 정의합니다.
 *
 * <h3>상태별 특성:</h3>
 * <table border="1">
 *   <tr><th>상태</th><th>결제 가능</th><th>재활성화 가능</th><th>해지 가능</th></tr>
 *   <tr><td>ACTIVE</td><td>O</td><td>-</td><td>O</td></tr>
 *   <tr><td>INACTIVE</td><td>X</td><td>O</td><td>O</td></tr>
 *   <tr><td>BLOCKED</td><td>X</td><td>O (관리자)</td><td>O</td></tr>
 *   <tr><td>EXPIRED</td><td>X</td><td>X (재발급)</td><td>O</td></tr>
 *   <tr><td>TERMINATED</td><td>X</td><td>X</td><td>-</td></tr>
 * </table>
 *
 * <h3>상태 전이 규칙:</h3>
 * <pre>
 * ACTIVE ─────────────────────────────────────────┐
 *   │                                             │
 *   ├── 비활성화 요청 ──▶ INACTIVE                │
 *   ├── 분실/도난 신고 ──▶ BLOCKED               │
 *   ├── 유효기간 만료 ──▶ EXPIRED                │
 *   └── 해지 요청 ──────────────────────────────▶│
 *                                                 │
 * INACTIVE ─┬── 활성화 ──▶ ACTIVE                │
 *           └── 해지 ─────────────────────────────▶│
 *                                                 │
 * BLOCKED ──┬── 신고 해제 ──▶ ACTIVE/INACTIVE   │
 *           └── 해지 ─────────────────────────────▶│
 *                                                 │
 * EXPIRED ──── 해지 ───────────────────────────────▶│
 *                                                 │
 *                                           TERMINATED (최종)
 * </pre>
 *
 * @see Card
 */
public enum CardStatus {

    /**
     * 정상
     * <p>결제 가능한 정상 상태</p>
     */
    ACTIVE("정상", true, false, true),

    /**
     * 비활성화
     * <p>
     * 사용자 요청으로 일시 정지된 상태.
     * 언제든 다시 활성화할 수 있습니다.
     * </p>
     */
    INACTIVE("비활성화", false, true, true),

    /**
     * 분실/도난 신고
     * <p>
     * 분실 또는 도난 신고된 상태.
     * 모든 결제가 차단됩니다.
     * 관리자 승인 후 해제 가능합니다.
     * </p>
     */
    BLOCKED("정지", false, true, true),

    /**
     * 만료
     * <p>
     * 유효기간이 지난 상태.
     * 재발급이 필요합니다.
     * </p>
     */
    EXPIRED("만료", false, false, true),

    /**
     * 해지
     * <p>
     * 완전히 해지된 최종 상태.
     * 복구가 불가능합니다.
     * </p>
     */
    TERMINATED("해지", false, false, false);

    private final String description;
    private final boolean canPay;        // 결제 가능 여부
    private final boolean canReactivate; // 재활성화 가능 여부
    private final boolean canTerminate;  // 해지 가능 여부

    CardStatus(String description, boolean canPay, boolean canReactivate, boolean canTerminate) {
        this.description = description;
        this.canPay = canPay;
        this.canReactivate = canReactivate;
        this.canTerminate = canTerminate;
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
     * 결제 가능 여부 확인
     *
     * @return 결제 가능하면 true
     */
    public boolean canPay() {
        return canPay;
    }

    /**
     * 재활성화 가능 여부 확인
     *
     * @return 재활성화 가능하면 true
     */
    public boolean canReactivate() {
        return canReactivate;
    }

    /**
     * 해지 가능 여부 확인
     *
     * @return 해지 가능하면 true
     */
    public boolean canTerminate() {
        return canTerminate;
    }

    /**
     * 정상 상태 여부 확인
     *
     * @return ACTIVE이면 true
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

    /**
     * 최종 상태 여부 확인
     *
     * @return TERMINATED이면 true
     */
    public boolean isTerminated() {
        return this == TERMINATED;
    }

    /**
     * 분실/도난 신고 상태 여부 확인
     *
     * @return BLOCKED이면 true
     */
    public boolean isBlocked() {
        return this == BLOCKED;
    }

    /**
     * 만료 상태 여부 확인
     *
     * @return EXPIRED이면 true
     */
    public boolean isExpired() {
        return this == EXPIRED;
    }

    /**
     * 특정 상태로 전환 가능 여부 확인
     *
     * @param target 전환하려는 상태
     * @return 전환 가능하면 true
     */
    public boolean canTransitionTo(CardStatus target) {
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
    public Set<CardStatus> getAllowedTransitions() {
        return switch (this) {
            case ACTIVE -> EnumSet.of(INACTIVE, BLOCKED, EXPIRED, TERMINATED);
            case INACTIVE -> EnumSet.of(ACTIVE, BLOCKED, TERMINATED);
            case BLOCKED -> EnumSet.of(ACTIVE, INACTIVE, TERMINATED);
            case EXPIRED -> EnumSet.of(TERMINATED);
            case TERMINATED -> EnumSet.noneOf(CardStatus.class);  // 최종 상태
        };
    }
}