package com.jun_bank.card_service.domain.card.domain.model.vo;

import com.jun_bank.card_service.domain.card.domain.exception.CardException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * 금액 VO (Value Object) - Card Service
 * <p>
 * 결제 금액을 안전하게 다루기 위한 불변 객체입니다.
 *
 * @param amount 금액 (BigDecimal, 0 이상)
 */
public record Money(BigDecimal amount) implements Comparable<Money> {

    private static final int SCALE = 0;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        if (amount == null) {
            throw CardException.invalidAmount(null);
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw CardException.invalidAmount(amount);
        }
        amount = amount.setScale(SCALE, ROUNDING_MODE);
    }

    public static Money of(long amount) {
        if (amount == 0) {
            return ZERO;
        }
        return new Money(BigDecimal.valueOf(amount));
    }

    public static Money of(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return ZERO;
        }
        return new Money(amount);
    }

    public static Money of(String amount) {
        return of(new BigDecimal(amount));
    }

    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isGreaterThan(Money other) {
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        return this.amount.compareTo(other.amount) >= 0;
    }

    public boolean isLessThanOrEqual(Money other) {
        return this.amount.compareTo(other.amount) <= 0;
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money subtract(Money other) {
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw CardException.insufficientBalance(this.amount, other.amount);
        }
        return new Money(result);
    }

    /**
     * 한도 검증
     * <p>
     * 현재 금액 + 요청 금액이 한도를 초과하는지 확인합니다.
     * </p>
     *
     * @param requestAmount 요청 금액
     * @param limit 한도
     * @return 한도 이내이면 true
     */
    public boolean isWithinLimit(Money requestAmount, Money limit) {
        return this.add(requestAmount).isLessThanOrEqual(limit);
    }

    public String formatted() {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.KOREA);
        return format.format(amount) + "원";
    }

    public long toLong() {
        return amount.longValue();
    }

    @Override
    public int compareTo(Money other) {
        return this.amount.compareTo(other.amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return amount.toPlainString();
    }
}