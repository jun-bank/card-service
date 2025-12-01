package com.jun_bank.card_service.domain.card.domain.model.vo;

import com.jun_bank.card_service.domain.card.domain.exception.CardException;

import java.security.SecureRandom;
import java.util.regex.Pattern;

/**
 * 카드번호 VO (Value Object)
 * <p>
 * 16자리 카드번호를 관리합니다.
 * Luhn 알고리즘으로 유효성을 검증합니다.
 *
 * <h3>카드번호 형식:</h3>
 * <pre>
 * 9410-XXXX-XXXX-XXXX (16자리)
 * ├─ 앞 4자리: 카드사 식별번호 (BIN)
 * ├─ 중간 8자리: 카드 고유번호
 * └─ 마지막 4자리: Luhn 체크섬 포함
 * </pre>
 *
 * <h3>Luhn 알고리즘:</h3>
 * <p>
 * 신용카드 번호 검증에 사용되는 체크섬 알고리즘입니다.
 * </p>
 *
 * @param value 카드번호 문자열 (16자리 숫자만)
 */
public record CardNumber(String value) {

    /**
     * 카드사 식별번호 (BIN - Bank Identification Number)
     * <p>가상의 jun-bank 카드사 번호</p>
     */
    public static final String BIN = "9410";

    /**
     * 카드번호 패턴 (16자리 숫자)
     */
    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("^\\d{16}$");

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * CardNumber 생성자 (Compact Constructor)
     *
     * @param value 카드번호 문자열
     * @throws CardException 형식이 잘못되었거나 Luhn 검증 실패 시
     */
    public CardNumber {
        // 하이픈 제거
        value = value.replaceAll("-", "");

        if (!CARD_NUMBER_PATTERN.matcher(value).matches()) {
            throw CardException.invalidCardNumberFormat(value);
        }
        if (!validateLuhn(value)) {
            throw CardException.invalidCardNumberFormat(value);
        }
    }

    /**
     * 문자열로부터 CardNumber 객체 생성
     *
     * @param value 카드번호 문자열
     * @return CardNumber 객체
     */
    public static CardNumber of(String value) {
        return new CardNumber(value);
    }

    /**
     * 새로운 카드번호 생성
     * <p>
     * BIN + 랜덤 11자리 + Luhn 체크 디지트
     * </p>
     *
     * @return 생성된 CardNumber 객체
     */
    public static CardNumber generate() {
        // BIN (4자리) + 랜덤 (11자리) = 15자리
        StringBuilder builder = new StringBuilder(BIN);
        for (int i = 0; i < 11; i++) {
            builder.append(RANDOM.nextInt(10));
        }

        // Luhn 체크 디지트 계산 (1자리)
        String base = builder.toString();
        int checkDigit = calculateLuhnCheckDigit(base);
        builder.append(checkDigit);

        return new CardNumber(builder.toString());
    }

    /**
     * 마스킹된 카드번호 반환
     *
     * @return 마스킹된 문자열 (예: 9410-****-****-1234)
     */
    public String masked() {
        return value.substring(0, 4) + "-****-****-" + value.substring(12);
    }

    /**
     * 하이픈 포함 형식 반환
     *
     * @return 하이픈 포함 문자열 (예: 9410-1234-5678-9012)
     */
    public String formatted() {
        return value.substring(0, 4) + "-" +
                value.substring(4, 8) + "-" +
                value.substring(8, 12) + "-" +
                value.substring(12, 16);
    }

    /**
     * 앞 6자리 반환 (BIN + 2자리)
     * <p>
     * 카드 식별에 사용됩니다.
     * </p>
     *
     * @return 앞 6자리
     */
    public String getPrefix() {
        return value.substring(0, 6);
    }

    /**
     * 뒤 4자리 반환
     * <p>
     * 영수증 출력 등에 사용됩니다.
     * </p>
     *
     * @return 뒤 4자리
     */
    public String getLastFour() {
        return value.substring(12);
    }

    /**
     * Luhn 알고리즘으로 카드번호 유효성 검증
     *
     * @param cardNumber 검증할 카드번호
     * @return 유효하면 true
     */
    private static boolean validateLuhn(String cardNumber) {
        int sum = 0;
        boolean alternate = false;

        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            alternate = !alternate;
        }

        return sum % 10 == 0;
    }

    /**
     * Luhn 체크 디지트 계산
     *
     * @param base 15자리 기본 번호
     * @return 체크 디지트 (0-9)
     */
    private static int calculateLuhnCheckDigit(String base) {
        int sum = 0;
        boolean alternate = true;  // 체크 디지트가 추가될 자리 기준

        for (int i = base.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(base.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            alternate = !alternate;
        }

        return (10 - (sum % 10)) % 10;
    }

    @Override
    public String toString() {
        return masked();  // 보안을 위해 마스킹된 값 반환
    }
}