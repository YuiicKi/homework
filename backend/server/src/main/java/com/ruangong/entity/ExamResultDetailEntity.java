package com.ruangong.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "exam_result_details")
public class ExamResultDetailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_id", nullable = false)
    private ExamResultRecordEntity result;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private ExamSubjectEntity subject;

    @Column(name = "subject_name")
    private String subjectName;

    @Column(name = "score")
    private Double score;

    @Column(name = "pass_line")
    private Double passLine;

    @Column(name = "is_pass")
    private Boolean pass;

    @Column(name = "national_rank")
    private Integer nationalRank;

    @Column(name = "remark")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ExamResultRecordEntity getResult() {
        return result;
    }

    public void setResult(ExamResultRecordEntity result) {
        this.result = result;
    }

    public ExamSubjectEntity getSubject() {
        return subject;
    }

    public void setSubject(ExamSubjectEntity subject) {
        this.subject = subject;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Double getPassLine() {
        return passLine;
    }

    public void setPassLine(Double passLine) {
        this.passLine = passLine;
    }

    public Boolean getPass() {
        return pass;
    }

    public void setPass(Boolean pass) {
        this.pass = pass;
    }

    public Integer getNationalRank() {
        return nationalRank;
    }

    public void setNationalRank(Integer nationalRank) {
        this.nationalRank = nationalRank;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
