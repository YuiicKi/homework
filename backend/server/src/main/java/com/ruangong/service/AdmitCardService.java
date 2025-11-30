package com.ruangong.service;

import com.ruangong.entity.AdmitCardLogEntity;
import com.ruangong.entity.AdmitCardTemplateEntity;
import com.ruangong.entity.ExamRoomEntity;
import com.ruangong.entity.ExamScheduleEntity;
import com.ruangong.entity.ExamSessionEntity;
import com.ruangong.entity.ExamSubjectEntity;
import com.ruangong.entity.RegistrationInfoEntity;
import com.ruangong.entity.SeatAssignmentEntity;
import com.ruangong.model.AdmitCardModel;
import com.ruangong.model.input.AdmitCardTemplateInput;
import com.ruangong.repository.AdmitCardLogRepository;
import com.ruangong.repository.AdmitCardTemplateRepository;
import com.ruangong.repository.ExamScheduleRepository;
import com.ruangong.repository.RegistrationInfoRepository;
import com.ruangong.repository.SeatAssignmentRepository;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.nio.file.StandardOpenOption;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.core.io.ClassPathResource;

@Service
@Transactional
public class AdmitCardService {

    private final SeatAssignmentRepository seatAssignmentRepository;
    private final RegistrationInfoRepository registrationInfoRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final AdmitCardTemplateRepository admitCardTemplateRepository;
    private final AdmitCardLogRepository admitCardLogRepository;

    public AdmitCardService(
        SeatAssignmentRepository seatAssignmentRepository,
        RegistrationInfoRepository registrationInfoRepository,
        ExamScheduleRepository examScheduleRepository,
        AdmitCardTemplateRepository admitCardTemplateRepository,
        AdmitCardLogRepository admitCardLogRepository
    ) {
        this.seatAssignmentRepository = seatAssignmentRepository;
        this.registrationInfoRepository = registrationInfoRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.admitCardTemplateRepository = admitCardTemplateRepository;
        this.admitCardLogRepository = admitCardLogRepository;
    }

    @Transactional(readOnly = true)
    public AdmitCardModel myAdmitCard(Long registrationInfoId, Long templateId) {
        RegistrationInfoEntity info = registrationInfoRepository.findById(registrationInfoId)
            .orElseThrow(() -> new IllegalArgumentException("报名信息不存在"));
        List<SeatAssignmentEntity> assignments = seatAssignmentRepository.findByRegistrationInfoId(registrationInfoId);
        if (assignments.isEmpty()) {
            throw new IllegalStateException("尚未分配座位");
        }
        SeatAssignmentEntity seat = assignments.get(0);
        ExamSubjectEntity subject = seat.getSubject();
        ExamRoomEntity room = seat.getRoom();
        ExamSessionEntity session = seat.getSession();
        AdmitCardTemplateEntity template = resolveTemplate(templateId);
        String qrContent = seat.getTicketNumber();

        return new AdmitCardModel(
            info.getId(),
            seat.getTicketNumber(),
            subject != null ? subject.getName() : null,
            session != null ? session.getName() : null,
            session != null && session.getStartTime() != null ? session.getStartTime().toString() : null,
            session != null && session.getEndTime() != null ? session.getEndTime().toString() : null,
            room != null ? room.getName() : null,
            room != null ? room.getRoomNumber() : null,
            seat.getSeatNumber(),
            info.getFullName(),
            info.getIdCardNumber(),
            template != null ? template.getExamNotice() : null,
            template != null ? template.getLogoUrl() : null,
            qrContent,
            null
        );
    }

    public AdmitCardModel refreshAdmitCard(Long registrationInfoId, Long templateId) {
        AdmitCardModel model = myAdmitCard(registrationInfoId, templateId);
        String filePath = generatePdf(model);
        recordLog(registrationInfoId, model.ticketNumber(), "GENERATED", null, filePath);
        return new AdmitCardModel(
            model.registrationInfoId(),
            model.ticketNumber(),
            model.subjectName(),
            model.sessionName(),
            model.sessionStartTime(),
            model.sessionEndTime(),
            model.roomName(),
            model.roomNumber(),
            model.seatNumber(),
            model.fullName(),
            model.idCardNumber(),
            model.examNotice(),
            model.logoUrl(),
            model.qrContent(),
            filePath
        );
    }

    public AdmitCardTemplateEntity resolveTemplate(Long templateId) {
        if (templateId == null) {
            return admitCardTemplateRepository.findAll().stream().findFirst().orElse(null);
        }
        return admitCardTemplateRepository.findById(templateId)
            .orElseThrow(() -> new IllegalArgumentException("准考证模板不存在"));
    }

    public AdmitCardTemplateEntity upsertTemplate(AdmitCardTemplateInput input) {
        AdmitCardTemplateEntity entity = input.getId() != null
            ? admitCardTemplateRepository.findById(input.getId()).orElse(new AdmitCardTemplateEntity())
            : new AdmitCardTemplateEntity();
        entity.setName(input.getName());
        entity.setLogoUrl(normalize(input.getLogoUrl()));
        entity.setExamNotice(normalize(input.getExamNotice()));
        entity.setQrStyle(normalize(input.getQrStyle()));
        return admitCardTemplateRepository.save(entity);
    }

    public boolean deleteTemplate(Long id) {
        if (!admitCardTemplateRepository.existsById(id)) {
            throw new IllegalArgumentException("准考证模板不存在");
        }
        admitCardTemplateRepository.deleteById(id);
        return true;
    }

    @Transactional(readOnly = true)
    public List<AdmitCardTemplateEntity> listTemplates() {
        return admitCardTemplateRepository.findAll();
    }

    public AdmitCardLogEntity recordLog(Long registrationInfoId, String ticketNumber, String status, String message, String path) {
        AdmitCardLogEntity log = new AdmitCardLogEntity();
        log.setRegistrationInfoId(registrationInfoId);
        log.setTicketNumber(ticketNumber);
        log.setStatus(status);
        log.setMessage(normalize(message));
        log.setFilePath(normalize(path));
        log.setCreatedAt(OffsetDateTime.now());
        return admitCardLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AdmitCardLogEntity> logs(Long registrationInfoId) {
        return admitCardLogRepository.findByRegistrationInfoIdOrderByCreatedAtDesc(registrationInfoId);
    }

    @Transactional(readOnly = true)
    public boolean isRegistrationOwnedBy(Long registrationInfoId, Long userId) {
        if (registrationInfoId == null || userId == null) {
            return false;
        }
        return registrationInfoRepository.findById(registrationInfoId)
            .map(info -> info.getUser() != null && userId.equals(info.getUser().getId()))
            .orElse(false);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String generatePdf(AdmitCardModel model) {
        try {
            String html = buildHtml(model);
            Path dir = Path.of("build", "admit-cards");
            Files.createDirectories(dir);
            String fileName = model.ticketNumber() + "-" + UUID.randomUUID() + ".pdf";
            Path filePath = dir.resolve(fileName);

            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            configureFonts(builder);
            builder.toStream(Files.newOutputStream(filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
            builder.run();
            return filePath.toAbsolutePath().toString();
        } catch (Exception ex) {
            throw new IllegalStateException("生成准考证失败: " + ex.getMessage(), ex);
        }
    }

    private String buildHtml(AdmitCardModel model) {
        String notice = model.examNotice() != null ? model.examNotice() : "请提前30分钟到场";
        String logo = model.logoUrl() != null ? "<img src='" + model.logoUrl() + "' style='max-height:60px;'/>" : "";
        String qr = model.qrContent() != null ? model.qrContent() : "";
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><meta charset='UTF-8'/>");
        sb.append("<style>");
        sb.append("@page { size: A4; margin: 20px; }");
        sb.append("body { font-family: 'MyCustomFont', 'Noto Sans SC', 'Microsoft YaHei', Arial, sans-serif; padding: 16px; font-weight: normal; }");
        sb.append(".header { display:flex; justify-content: space-between; align-items:center; }");
        sb.append(".card { border:1px solid #ccc; padding:16px; margin-top:12px; }");
        sb.append(".row { margin:6px 0; }");
        sb.append(".title { font-size:20px; font-weight:bold; }");
        sb.append(".qr { margin-top:12px; font-size:12px; color:#666; }");
        sb.append("b, strong, h1, h2, h3 { font-weight: normal; }");
        sb.append("</style></head><body>");
        sb.append("<div class='header'><div class='title'>准考证</div><div>").append(logo).append("</div></div>");
        sb.append("<div class='card'>");
        sb.append("<div class='row'>准考证号：").append(safe(model.ticketNumber())).append("</div>");
        sb.append("<div class='row'>姓名：").append(safe(model.fullName())).append("</div>");
        sb.append("<div class='row'>身份证号：").append(safe(model.idCardNumber())).append("</div>");
        sb.append("<div class='row'>科目：").append(safe(model.subjectName())).append("</div>");
        sb.append("<div class='row'>场次：").append(safe(model.sessionName())).append(" (")
            .append(safe(model.sessionStartTime())).append(" - ").append(safe(model.sessionEndTime())).append(")</div>");
        sb.append("<div class='row'>考场：").append(safe(model.roomName())).append(" (")
            .append(safe(model.roomNumber())).append(") 座位：").append(safeNum(model.seatNumber())).append("</div>");
        sb.append("<div class='row'>考试须知：</div>");
        sb.append("<div class='row'>").append(notice).append("</div>");
        sb.append("<div class='qr'>二维码/验证码：").append(qr).append("</div>");
        sb.append("</div></body></html>");
        return sb.toString();
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }

    private String safeNum(Integer v) {
        return v == null ? "" : v.toString();
    }

    private void configureFonts(PdfRendererBuilder builder) {
        // 优先使用打包在 classpath 下的字体，确保跨平台稳定
        try {
            File classpathFont = getFontFileFromClasspath("fonts/NotoSansSC-Regular.ttf");
            builder.useFont(classpathFont, "MyCustomFont", 400, BaseRendererBuilder.FontStyle.NORMAL, true);
            return;
        } catch (Exception ignored) {
            // fallback to system fonts if classpath font missing
        }

        String[] candidates = new String[] {
            "/System/Library/Fonts/PingFang.ttc",
            "/System/Library/Fonts/STHeiti Light.ttc",
            "/Library/Fonts/Arial Unicode.ttf",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc"
        };
        for (String path : candidates) {
            File f = new File(path);
            if (f.exists()) {
                builder.useFont(f, "MyCustomFont", 400, BaseRendererBuilder.FontStyle.NORMAL, true);
                break;
            }
        }
    }

    /**
     * 从 classpath 复制字体到临时文件，供 PDF 渲染使用。
     */
    private File getFontFileFromClasspath(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (!resource.exists()) {
                throw new IllegalStateException("字体文件未找到: " + path);
            }
            InputStream inputStream = resource.getInputStream();
            File tempFile = File.createTempFile("font_temp_", ".ttf");
            Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            tempFile.deleteOnExit();
            return tempFile;
        } catch (Exception ex) {
            throw new IllegalStateException("字体加载失败: " + ex.getMessage(), ex);
        }
    }
}
