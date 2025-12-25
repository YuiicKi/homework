package com.ruangong.model.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ExamResultDetailInput {

    private Long subjectId;

    @NotBlank
    private String subjectName;

    @NotNull
    private Double score;

    private Double passLine;

    private Boolean isPass;

    private Integer nationalRank;

    private String remark;

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
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

    public Boolean getIsPass() {
        return isPass;
    }

    public void setIsPass(Boolean pass) {
        this.isPass = pass;
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
