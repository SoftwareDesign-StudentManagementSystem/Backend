package com.iEdu.global.common.enums;

public enum Semester {
    FIRST_SEMESTER, SECOND_SEMESTER, ALL;

    public String toKoreanString() {
        return switch (this) {
            case FIRST_SEMESTER -> "1학기";
            case SECOND_SEMESTER -> "2학기";
            case ALL -> "1, 2학기";
        };
    }
}
