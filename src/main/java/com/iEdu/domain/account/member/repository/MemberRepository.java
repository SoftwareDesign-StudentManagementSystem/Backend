package com.iEdu.domain.account.member.repository;

import com.iEdu.domain.account.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    // 계정ID로 회원 존재여부 확인
    boolean existsByAccountId(Long accountId);

    // 계정ID&역할로 회원 존재여부 확인
    boolean existsByAccountIdAndRole(Long accountId, Member.MemberRole role);

    // 이메일&역할로 회원 존재여부 확인
    boolean existsByEmailAndRole(String email, Member.MemberRole role);

    // 이메일로 회원 조회
    Optional<Member> findByEmail(String email);

    // 계정ID로 회원 조회
    Optional<Member> findByAccountId(Long accountId);

    // 역할로 회원 조회
    Optional<Member> findByIdAndRole(Long id, Member.MemberRole role);

    // 계정ID&이름으로 회원 검색하기
    @Query("SELECT m FROM Member m WHERE m.name LIKE %:keyword% OR str(m.accountId) LIKE %:keyword%")
    Page<Member> findByKeyword(Pageable pageable, @Param("keyword") String keyword);

    // 계정ID&이름&역할로 회원 검색하기
    @Query("SELECT m FROM Member m " +
            "WHERE (m.name LIKE %:keyword% OR str(m.accountId) LIKE %:keyword%) " +
            "AND m.role = :role")
    Page<Member> findByKeywordAndRole(Pageable pageable, @Param("keyword") String keyword, @Param("role") Member.MemberRole role);

    // 학년&반으로 학생 조회
    Page<Member> findAllByYearAndClassIdAndRole(Integer year, Integer classId, Member.MemberRole role, Pageable pageable);

    // 학년&반&번호로 학생 조회
    Page<Member> findByYearAndClassIdAndNumberAndRole(
            Integer year, Integer classId, Integer number, Member.MemberRole role, Pageable pageable);
}
