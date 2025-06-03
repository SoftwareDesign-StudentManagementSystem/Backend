package com.iEdu.domain.studentRecord.grade.entity;

import com.iEdu.domain.account.member.entity.Member;
import com.iEdu.global.common.enums.Semester;
import com.iEdu.global.common.utils.AESUtil;
import com.iEdu.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class Grade extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Member member;

    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private Semester semester;

    private String koreanLanguageScore;
    private String mathematicsScore;
    private String englishScore;
    private String socialStudiesScore;
    private String historyScore;
    private String ethicsScore;
    private String economicsScore;
    private String physicsScore;
    private String chemistryScore;
    private String biologyScore;
    private String earthScienceScore;
    private String musicScore;
    private String artScore;
    private String physicalEducationScore;
    private String technologyAndHomeEconomicScore;
    private String computerScienceScore;
    private String secondForeignLanguageScore;

    // --- AES 암호화/복호화 커스텀 getter/setter ---

    public void setKoreanLanguageScore(Double score) {
        this.koreanLanguageScore = encryptScore(score);
    }
    public Double getKoreanLanguageScore() {
        return decryptScore(koreanLanguageScore);
    }

    public void setMathematicsScore(Double score) {
        this.mathematicsScore = encryptScore(score);
    }
    public Double getMathematicsScore() {
        return decryptScore(mathematicsScore);
    }

    public void setEnglishScore(Double score) {
        this.englishScore = encryptScore(score);
    }
    public Double getEnglishScore() {
        return decryptScore(englishScore);
    }

    public void setSocialStudiesScore(Double score) {
        this.socialStudiesScore = encryptScore(score);
    }
    public Double getSocialStudiesScore() {
        return decryptScore(socialStudiesScore);
    }

    public void setHistoryScore(Double score) {
        this.historyScore = encryptScore(score);
    }
    public Double getHistoryScore() {
        return decryptScore(historyScore);
    }

    public void setEthicsScore(Double score) {
        this.ethicsScore = encryptScore(score);
    }
    public Double getEthicsScore() {
        return decryptScore(ethicsScore);
    }

    public void setEconomicsScore(Double score) {
        this.economicsScore = encryptScore(score);
    }
    public Double getEconomicsScore() {
        return decryptScore(economicsScore);
    }

    public void setPhysicsScore(Double score) {
        this.physicsScore = encryptScore(score);
    }
    public Double getPhysicsScore() {
        return decryptScore(physicsScore);
    }

    public void setChemistryScore(Double score) {
        this.chemistryScore = encryptScore(score);
    }
    public Double getChemistryScore() {
        return decryptScore(chemistryScore);
    }

    public void setBiologyScore(Double score) {
        this.biologyScore = encryptScore(score);
    }
    public Double getBiologyScore() {
        return decryptScore(biologyScore);
    }

    public void setEarthScienceScore(Double score) {
        this.earthScienceScore = encryptScore(score);
    }
    public Double getEarthScienceScore() {
        return decryptScore(earthScienceScore);
    }

    public void setMusicScore(Double score) {
        this.musicScore = encryptScore(score);
    }
    public Double getMusicScore() {
        return decryptScore(musicScore);
    }

    public void setArtScore(Double score) {
        this.artScore = encryptScore(score);
    }
    public Double getArtScore() {
        return decryptScore(artScore);
    }

    public void setPhysicalEducationScore(Double score) {
        this.physicalEducationScore = encryptScore(score);
    }
    public Double getPhysicalEducationScore() {
        return decryptScore(physicalEducationScore);
    }

    public void setTechnologyAndHomeEconomicScore(Double score) {
        this.technologyAndHomeEconomicScore = encryptScore(score);
    }
    public Double getTechnologyAndHomeEconomicScore() {
        return decryptScore(technologyAndHomeEconomicScore);
    }

    public void setComputerScienceScore(Double score) {
        this.computerScienceScore = encryptScore(score);
    }
    public Double getComputerScienceScore() {
        return decryptScore(computerScienceScore);
    }

    public void setSecondForeignLanguageScore(Double score) {
        this.secondForeignLanguageScore = encryptScore(score);
    }
    public Double getSecondForeignLanguageScore() {
        return decryptScore(secondForeignLanguageScore);
    }

    // 암호화 및 복호화 헬퍼 메서드
    private String encryptScore(Double score) {
        if (score == null) return null;
        try {
            return AESUtil.encrypt(String.valueOf(score));
        } catch (Exception e) {
            throw new RuntimeException("점수 암호화 실패", e);
        }
    }

    private Double decryptScore(String encryptedScore) {
        if (encryptedScore == null) return null;
        try {
            String decrypted = AESUtil.decrypt(encryptedScore);  // 복호화된 문자열
            return Double.valueOf(decrypted);
        } catch (Exception e) {
            throw new RuntimeException("점수 복호화 실패", e);
        }
    }
}
