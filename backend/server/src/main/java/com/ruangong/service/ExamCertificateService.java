package com.ruangong.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.ruangong.model.ExamCertificateFileModel;
import com.ruangong.model.ExamResultDetailModel;
import com.ruangong.model.ExamResultModel;
import com.ruangong.model.input.ExamCertificateRequestInput;
import com.ruangong.model.input.ExamResultQueryInput;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ExamCertificateService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日", Locale.CHINA);
    private static final DecimalFormat SCORE_FORMAT = new DecimalFormat("0.##");

    private final ExamResultService examResultService;

    public ExamCertificateService(ExamResultService examResultService) {
        this.examResultService = examResultService;
    }

    public ExamCertificateFileModel downloadCertificate(ExamCertificateRequestInput input) {
        validateRequest(input);
        ExamResultQueryInput queryInput = new ExamResultQueryInput();
        queryInput.setExamType(input.getExamType());
        queryInput.setExamYear(input.getExamYear());
        queryInput.setTicketNumber(input.getTicketNumber());
        queryInput.setFullName(input.getFullName());
        queryInput.setIdCardNumber(input.getIdCardNumber());
        ExamResultModel result = examResultService.queryResult(queryInput);
        ensureMatches(result, input);
        ensureQualified(result);
        byte[] pdfBytes = buildPdf(result);
        String fileName = result.fullName() + "-" + result.examType() + "-certificate.pdf";
        return new ExamCertificateFileModel(
            fileName,
            "application/pdf",
            Base64.getEncoder().encodeToString(pdfBytes)
        );
    }

    private void validateRequest(ExamCertificateRequestInput input) {
        if (!StringUtils.hasText(input.getFullName()) || !StringUtils.hasText(input.getTicketNumber())) {
            throw new IllegalArgumentException("请输入姓名与准考证号后再查询证书");
        }
    }

    private void ensureMatches(ExamResultModel result, ExamCertificateRequestInput input) {
        if (!input.getFullName().trim().equalsIgnoreCase(result.fullName())) {
            throw new IllegalArgumentException("姓名与成绩记录不一致");
        }
        if (!input.getTicketNumber().trim().equalsIgnoreCase(result.ticketNumber())) {
            throw new IllegalArgumentException("准考证号与成绩记录不一致");
        }
        if (StringUtils.hasText(input.getIdCardNumber())) {
            String candidateId = result.idCardNumber() != null ? result.idCardNumber().trim() : "";
            if (!candidateId.equalsIgnoreCase(input.getIdCardNumber().trim())) {
                throw new IllegalArgumentException("身份证号与成绩记录不一致");
            }
        }
    }

    private void ensureQualified(ExamResultModel result) {
        Boolean qualified = result.isQualified();
        if (qualified == null || !qualified) {
            throw new IllegalStateException("仅在成绩合格后可下载电子证书");
        }
        if (result.subjects() != null) {
            boolean anyFailed = result.subjects().stream()
                .anyMatch(detail -> detail.isPass() != null && !detail.isPass());
            if (anyFailed) {
                throw new IllegalStateException("仅在所有科目合格后可下载电子证书");
            }
        }
    }

    private byte[] buildPdf(ExamResultModel result) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String html = renderHtml(result);
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("生成电子证书失败: " + ex.getMessage(), ex);
        }
    }

    private String renderHtml(ExamResultModel result) {
        String issuedDate = DATE_FORMATTER.format(OffsetDateTime.now());
        StringBuilder rows = new StringBuilder();
        List<ExamResultDetailModel> subjects = result.subjects();
        if (subjects != null) {
            for (ExamResultDetailModel detail : subjects) {
                rows.append("<tr>");
                rows.append("<td>").append(safe(detail.subjectName())).append("</td>");
                rows.append("<td>").append(formatScore(detail.score())).append("</td>");
                rows.append("<td>").append(formatScore(detail.passLine())).append("</td>");
                rows.append("<td>").append(Boolean.TRUE.equals(detail.isPass()) ? "合格" : "—").append("</td>");
                rows.append("</tr>");
            }
        }
        return """
            <html>
            <head>
            <style>
            body { font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif; padding: 40px; color: #333; }
            .card { border: 2px solid #b71c1c; padding: 30px; border-radius: 6px; position: relative; }
            h1 { text-align: center; color: #b71c1c; letter-spacing: 4px; }
            .meta { margin-top: 20px; line-height: 1.8; }
            table { width: 100%%; border-collapse: collapse; margin-top: 20px; }
            th, td { border: 1px solid #ddd; padding: 10px; text-align: center; }
            th { background: #f7f7f7; }
            .seal { position: absolute; right: 40px; bottom: 40px; width: 160px; height: 160px;
                    border: 4px solid #d32f2f; border-radius: 50%%; color: #d32f2f;
                    display: flex; align-items: center; justify-content: center; font-size: 16px;
                    transform: rotate(-15deg); opacity: 0.85; }
            .footer { margin-top: 30px; text-align: right; font-size: 14px; }
            </style>
            </head>
            <body>
              <div class="card">
                <h1>电子合格证书</h1>
                <div class="meta">
                  <strong>姓名：</strong>%s<br/>
                  <strong>身份证号：</strong>%s<br/>
                  <strong>准考证号：</strong>%s<br/>
                  <strong>考试名称：</strong>%s<br/>
                  <strong>考试年度：</strong>%s<br/>
                  <strong>资格结论：</strong>%s
                </div>
                <table>
                  <thead>
                    <tr>
                      <th>科目</th>
                      <th>成绩</th>
                      <th>合格线</th>
                      <th>结果</th>
                    </tr>
                  </thead>
                  <tbody>
                    %s
                  </tbody>
                </table>
                <div class="footer">
                  发证日期：%s<br/>
                  电子证书编号：%s
                </div>
                <div class="seal">官方认证<br/>电子印章</div>
              </div>
            </body>
            </html>
            """.formatted(
            safe(result.fullName()),
            maskId(result.idCardNumber()),
            safe(result.ticketNumber()),
            safe(result.examType()),
            result.examYear(),
            "合格",
            rows.toString(),
            issuedDate,
            buildCertificateNumber(result)
        );
    }

    private String buildCertificateNumber(ExamResultModel result) {
        String base = result.ticketNumber() != null ? result.ticketNumber() : "CERT";
        return base + "-" + result.examYear();
    }

    private String maskId(String idCard) {
        if (!StringUtils.hasText(idCard) || idCard.length() < 6) {
            return safe(idCard);
        }
        return idCard.substring(0, 3) + "******" + idCard.substring(idCard.length() - 3);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String formatScore(Double value) {
        if (value == null) {
            return "—";
        }
        return SCORE_FORMAT.format(value);
    }
}
