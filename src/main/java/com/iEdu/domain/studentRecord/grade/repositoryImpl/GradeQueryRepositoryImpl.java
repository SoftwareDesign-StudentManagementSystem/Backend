package com.iEdu.domain.studentRecord.grade.repositoryImpl;

import com.iEdu.domain.studentRecord.grade.entity.Grade;
import com.iEdu.domain.studentRecord.grade.entity.QGrade;
import com.iEdu.domain.studentRecord.grade.repository.GradeQueryRepository;
import com.iEdu.global.common.enums.Semester;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.iEdu.domain.account.member.entity.QMember.member;

@RequiredArgsConstructor
@Repository
public class GradeQueryRepositoryImpl implements GradeQueryRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<Grade> findAllByStudentInfoAndSemesterAndYear(
            Integer studentYear, Integer classId, Integer number,
            Semester semester) {

        QGrade grade = QGrade.grade;

        return queryFactory
                .selectFrom(grade)
                .where(
                        grade.member.year.eq(studentYear),
                        grade.member.classId.eq(classId),
                        eqNumber(grade, number),
                        grade.semester.eq(semester)
                )
                .fetch();
    }

    // grade를 인자로 받고, member.number.eq(number) 처리
    private BooleanExpression eqNumber(QGrade grade, Integer number) {
        return number != null ? grade.member.number.eq(number) : null;
    }
}
