package com.iEdu.domain.studentRecord.grade.repositoryImpl;

import com.iEdu.domain.account.member.entity.QMember;
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
    public List<Grade> findAllByStudentInfoAndSemesterAndYearWithMember(
            Integer studentYear, Integer classId, Integer number, Semester semester) {
        QGrade grade = QGrade.grade;
        QMember member = QMember.member;

        return queryFactory
                .selectFrom(grade)
                .join(grade.member, member).fetchJoin()   // fetchJoin
                .where(
                        member.year.eq(studentYear),
                        member.classId.eq(classId),
                        number != null ? member.number.eq(number) : null,
                        grade.semester.eq(semester),
                        grade.year.eq(studentYear)
                )
                .orderBy(member.id.asc())                 // DB에서 정렬
                .fetch();
    }
}
