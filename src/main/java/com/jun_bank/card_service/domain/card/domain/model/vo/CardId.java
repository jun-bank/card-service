package com.jun_bank.card_service.domain.card.domain.model.vo;

import com.jun_bank.common_lib.util.UuidUtils;
import com.jun_bank.card_service.domain.card.domain.exception.CardException;

/**
 * 카드 식별자 VO (Value Object)
 * <p>
 * 카드의 고유 식별자입니다.
 *
 * <h3>ID 형식:</h3>
 * <pre>CRD-xxxxxxxx (예: CRD-a1b2c3d4)</pre>
 *
 * @param value 카드 ID 문자열 (CRD-xxxxxxxx 형식)
 */
public record CardId(String value) {

    public static final String PREFIX = "CRD";

    public CardId {
        if (!UuidUtils.isValidDomainId(value, PREFIX)) {
            throw CardException.invalidCardIdFormat(value);
        }
    }

    public static CardId of(String value) {
        return new CardId(value);
    }

    public static String generateId() {
        return UuidUtils.generateDomainId(PREFIX);
    }
}