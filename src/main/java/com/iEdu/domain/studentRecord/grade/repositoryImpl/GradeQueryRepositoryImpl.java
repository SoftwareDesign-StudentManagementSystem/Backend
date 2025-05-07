package com.iEdu.domain.studentRecord.grade.repositoryImpl;

import com.iEdu.domain.studentRecord.grade.entity.Grade;
import com.iEdu.domain.studentRecord.grade.repository.GradeQueryRepository;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.iEdu.domain.account.member.entity.QMember.member;
import static com.iEdu.domain.studentRecord.grade.entity.QGrade.grade;

@RequiredArgsConstructor
@Repository
public class GradeQueryRepositoryImpl implements GradeQueryRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<Grade> findAllByClassAndSemester(Integer year, Integer classId, Integer number, Grade.Semester semester) {
        return queryFactory
                .selectFrom(grade)
                .join(grade.member, member).fetchJoin()
                .where(
                        member.year.eq(year),
                        member.classId.eq(classId),
                        eqNumber(number),
                        grade.semester.eq(semester)
                )
                .orderBy(member.number.asc())
                .fetch();
    }

    private BooleanExpression eqNumber(Integer number) {
        return (number != null) ? member.number.eq(number) : null;
    }
}
