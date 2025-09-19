package com.iEdu.domain.studentRecord.counsel.repositoryImpl;

import com.iEdu.domain.studentRecord.counsel.entity.Counsel;
import com.iEdu.domain.studentRecord.counsel.entity.QCounsel;
import com.iEdu.domain.studentRecord.counsel.repository.CounselQueryRepository;
import com.iEdu.global.common.enums.Semester;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@RequiredArgsConstructor
@Repository
public class CounselQueryRepositoryImpl implements CounselQueryRepository {
    private final JPAQueryFactory queryFactory;

    // 여러 학생 id를 한 번에 조회
    @Override
    public List<Counsel> findByMemberIdInAndYearAndSemester(List<Long> memberIds, Integer year, Semester semester) {
        if (memberIds == null || memberIds.isEmpty()) {
            return List.of();
        }
        QCounsel counsel = QCounsel.counsel;

        return queryFactory
                .selectFrom(counsel)
                .where(
                        counsel.member.id.in(memberIds),
                        counsel.year.eq(year),
                        counsel.semester.eq(semester)
                )
                .fetch();
    }
}
