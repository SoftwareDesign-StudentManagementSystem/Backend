package com.iEdu.global.common.utils;

import com.iEdu.global.common.enums.Semester;
import com.iEdu.global.exception.ReturnCode;
import com.iEdu.global.exception.ServiceException;

public class Converter {
    // 학기 Integer -> Enum 변환
    public static Semester convertToSemesterEnum(Integer semester) {
        return switch (semester) {
            case 1 -> Semester.FIRST_SEMESTER;
            case 2 -> Semester.SECOND_SEMESTER;
            default -> throw new ServiceException(ReturnCode.INVALID_SEMESTER);
        };
    }
}
