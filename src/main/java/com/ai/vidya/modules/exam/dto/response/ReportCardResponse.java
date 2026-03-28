package com.ai.vidya.modules.exam.dto.response;

import com.ai.vidya.modules.exam.entity.ExamResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportCardResponse {

    private UUID       studentId;
    private String     studentName;
    private String     admissionNo;
    private UUID       academicYearId;
    private UUID       termId;

    private List<SubjectResult> subjectResults;

    private BigDecimal totalMarksObtained;
    private BigDecimal totalMaxMarks;
    private BigDecimal overallPercentage;
    private String     overallGrade;
    private BigDecimal overallGradePoint;
    private Integer    classRank;

    private BigDecimal attendancePercentage;
    private Integer    daysPresent;
    private Integer    totalWorkingDays;

    private boolean    passed;
    private String     result;         // PROMOTED | DETAINED | PENDING

    private String     principalRemarks;
    private String     teacherRemarks;
    private boolean    published;
    private String     pdfUrl;

    @Data @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SubjectResult {
        private String                    subjectCode;
        private String                    subjectName;
        private BigDecimal                theoryMarks;
        private BigDecimal                practicalMarks;
        private BigDecimal                totalMarks;
        private BigDecimal                maxMarks;
        private BigDecimal                percentage;
        private String                    grade;
        private BigDecimal                gradePoint;
        private ExamResult.ResultStatus   status;
        private boolean                   absent;
    }
}
