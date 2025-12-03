package com.iEdu.global.common.utils.Aes;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDate;

@Converter
public class AesLocalDateAttributeConverter implements AttributeConverter<LocalDate, String> {
    @Override
    public String convertToDatabaseColumn(LocalDate attribute) {
        if (attribute == null) return null;
        return AesUtil.encrypt(attribute.toString());   // ISO-8601 문자열(yyyy-MM-dd)로 직렬화 후 암호화
    }

    @Override
    public LocalDate convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        String plain = AesUtil.decrypt(dbData);
        return LocalDate.parse(plain);      // ISO-8601 파싱
    }
}
