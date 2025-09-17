package com.iEdu.global.common.utils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDate;

@Converter(autoApply = false)
public class AesLocalDateAttributeConverter implements AttributeConverter<LocalDate, String> {
    @Override
    public String convertToDatabaseColumn(LocalDate attribute) {
        if (attribute == null) return null;
        // ISO-8601 문자열(yyyy-MM-dd)로 직렬화 후 암호화
        return AesUtil.encrypt(attribute.toString());
    }

    @Override
    public LocalDate convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        String plain = AesUtil.decrypt(dbData);
        return LocalDate.parse(plain); // ISO-8601 파싱
    }
}
