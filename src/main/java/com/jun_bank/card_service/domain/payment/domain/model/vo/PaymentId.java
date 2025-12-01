package com.jun_bank.card_service.domain.payment.domain.model.vo;

import com.jun_bank.common_lib.util.UuidUtils;
import com.jun_bank.card_service.domain.payment.domain.exception.PaymentException;

/**
 * 결제 식별자 VO (Value Object)
 * <p>
 * 결제의 고유 식별자입니다.
 *
 * <h3>ID 형식:</h3>
 * <pre>PAY-xxxxxxxx (예: PAY-a1b2c3d4)</pre>
 *
 * @param value 결제 ID 문자열 (PAY-xxxxxxxx 형식)
 */
public record PaymentId(String value) {

    public static final String PREFIX = "PAY";

    public PaymentId {
        if (!UuidUtils.isValidDomainId(value, PREFIX)) {
            throw PaymentException.invalidPaymentIdFormat(value);
        }
    }

    public static PaymentId of(String value) {
        return new PaymentId(value);
    }

    public static String generateId() {
        return UuidUtils.generateDomainId(PREFIX);
    }
}