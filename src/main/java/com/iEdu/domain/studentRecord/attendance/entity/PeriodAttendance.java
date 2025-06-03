package com.iEdu.domain.studentRecord.attendance.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.iEdu.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PeriodAttendance extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_id", nullable = false)
    private Attendance attendance;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private State state;
    public enum State {
        출석, 결석, 지각, 조퇴;
    }

    @Enumerated(EnumType.STRING)
    @Column(length = 8)
    private Period period;
    public enum Period {
        PERIOD_1(1),
        PERIOD_2(2),
        PERIOD_3(3),
        PERIOD_4(4),
        PERIOD_5(5),
        PERIOD_6(6),
        PERIOD_7(7),
        PERIOD_8(8);

        private final int value;

        Period(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static Period fromValue(int value) {
            for (Period p : values()) {
                if (p.value == value) return p;
            }
            throw new IllegalArgumentException("Invalid period value: " + value);
        }
    }
}
