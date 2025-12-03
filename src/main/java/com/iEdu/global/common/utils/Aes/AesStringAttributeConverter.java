package com.iEdu.global.common.utils.Aes;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AesStringAttributeConverter implements AttributeConverter<String, String> {
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        return AesUtil.encrypt(attribute); // GCM 방식으로 저장
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return AesUtil.decrypt(dbData); // GCM 방식으로 복호화
    }
}
