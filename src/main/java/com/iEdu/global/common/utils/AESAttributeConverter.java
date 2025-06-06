package com.iEdu.global.common.utils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AESAttributeConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        return AESUtil.encrypt(attribute); // CBC 방식으로 저장
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return AESUtil.safeDecrypt(dbData); // CBC → 실패 시 ECB 시도
    }
}
