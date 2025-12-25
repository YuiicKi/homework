package com.ruangong.graphql;

import com.ruangong.entity.ExamResultImportJobStatus;
import com.ruangong.model.ExamCenterModel;
import com.ruangong.model.ExamRoomModel;
import com.ruangong.model.ExamScheduleModel;
import com.ruangong.model.ExamSessionModel;
import com.ruangong.model.ExamSubjectLogModel;
import com.ruangong.model.ExamSubjectModel;
import com.ruangong.model.ExamRegistrationWindowLogModel;
import com.ruangong.model.ExamRegistrationViewModel;
import com.ruangong.model.ExamRegistrationWindowModel;
import com.ruangong.model.ExamResultModel;
import com.ruangong.model.ExamResultPreNotificationModel;
import com.ruangong.model.ExamResultReleaseSettingModel;
import com.ruangong.model.ExamResultImportJobModel;
import com.ruangong.model.ExamResultImportTemplateModel;
import com.ruangong.model.ExamCertificateFileModel;
import com.ruangong.model.InvigilatorAssignmentModel;
import com.ruangong.model.InvigilatorAssignmentStatsModel;
import com.ruangong.model.NotificationLogModel;
import com.ruangong.model.NotificationModel;
import com.ruangong.model.NotificationTemplateModel;
import com.ruangong.model.JwtPayload;
import com.ruangong.model.RegistrationAuditLogModel;
import com.ruangong.model.RegistrationInfoModel;
import com.ruangong.model.RegistrationMaterialModel;
import com.ruangong.model.RegistrationMaterialTemplateModel;
import com.ruangong.model.SeatAssignmentModel;
import com.ruangong.model.SeatAssignmentStatsModel;
import com.ruangong.model.SeatAssignmentTaskModel;
import com.ruangong.model.AdmitCardLogModel;
import com.ruangong.model.AdmitCardModel;
import com.ruangong.model.AdmitCardTemplateModel;
import com.ruangong.model.MyExamScheduleModel;
import com.ruangong.model.input.ExamCenterInput;
import com.ruangong.model.input.ExamRoomInput;
import com.ruangong.model.input.ExamScheduleInput;
import com.ruangong.model.input.ExamSessionInput;
import com.ruangong.model.input.ExamSubjectInput;
import com.ruangong.model.input.UpdateExamRoomStatusInput;
import com.ruangong.model.input.UpdateExamScheduleStatusInput;
import com.ruangong.model.input.UpdateExamSubjectStatusInput;
import com.ruangong.model.input.BatchExamSubjectStatusInput;
import com.ruangong.model.input.BatchDeleteExamSubjectInput;
import com.ruangong.model.input.ExamRegistrationWindowInput;
import com.ruangong.model.input.UpdateExamRegistrationStatusInput;
import com.ruangong.model.input.BatchUpdateExamRegistrationStatusInput;
import com.ruangong.model.input.BatchDeleteExamRegistrationInput;
import com.ruangong.model.input.ExamResultPreNotificationInput;
import com.ruangong.model.input.ExamResultReleaseBatchInput;
import com.ruangong.model.input.ExamResultReleaseSettingInput;
import com.ruangong.model.input.ExamResultImportInput;
import com.ruangong.model.input.AssignInvigilatorsInput;
import com.ruangong.model.input.NotificationInput;
import com.ruangong.model.input.NotificationTemplateInput;
import com.ruangong.model.input.PublishNotificationInput;
import com.ruangong.model.input.ExamResultQueryInput;
import com.ruangong.model.input.UpsertExamResultInput;
import com.ruangong.model.input.RegistrationInfoInput;
import com.ruangong.model.input.RegistrationMaterialInput;
import com.ruangong.model.input.RegistrationRejectInput;
import com.ruangong.model.input.RegistrationMaterialTemplateInput;
import com.ruangong.model.input.SeatAssignmentInput;
import com.ruangong.model.input.AdmitCardTemplateInput;
import com.ruangong.service.AuthorizationService;
import com.ruangong.service.ExamCenterService;
import com.ruangong.service.ExamRoomService;
import com.ruangong.service.ExamRegistrationWindowService;
import com.ruangong.service.ExamResultService;
import com.ruangong.service.ExamResultPreNotificationService;
import com.ruangong.service.ExamResultReleaseSettingService;
import com.ruangong.service.ExamResultImportService;
import com.ruangong.service.ExamCertificateService;
import com.ruangong.service.InvigilatorAssignmentService;
import com.ruangong.service.ExamScheduleService;
import com.ruangong.service.ExamSessionService;
import com.ruangong.service.ExamSubjectService;
import com.ruangong.service.NotificationService;
import com.ruangong.service.RegistrationService;
import com.ruangong.service.SeatAssignmentService;
import com.ruangong.service.AdmitCardService;
import com.ruangong.service.MyExamScheduleService;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Controller
@Validated
public class ExamGraphqlController {

    private static final Logger log = LoggerFactory.getLogger(ExamGraphqlController.class);

    private final AuthorizationService authorizationService;
    private final ExamCenterService examCenterService;
    private final ExamRoomService examRoomService;
    private final ExamSubjectService examSubjectService;
    private final ExamSessionService examSessionService;
    private final ExamScheduleService examScheduleService;
    private final ExamRegistrationWindowService examRegistrationWindowService;
    private final NotificationService notificationService;
    private final RegistrationService registrationService;
    private final SeatAssignmentService seatAssignmentService;
    private final AdmitCardService admitCardService;
    private final MyExamScheduleService myExamScheduleService;
    private final ExamResultService examResultService;
    private final ExamResultPreNotificationService examResultPreNotificationService;
    private final ExamResultReleaseSettingService examResultReleaseSettingService;
    private final ExamResultImportService examResultImportService;
    private final ExamCertificateService examCertificateService;
    private final InvigilatorAssignmentService invigilatorAssignmentService;

    public ExamGraphqlController(
        AuthorizationService authorizationService,
        ExamCenterService examCenterService,
        ExamRoomService examRoomService,
        ExamSubjectService examSubjectService,
        ExamSessionService examSessionService,
        ExamScheduleService examScheduleService,
        ExamRegistrationWindowService examRegistrationWindowService,
        NotificationService notificationService,
        RegistrationService registrationService,
        SeatAssignmentService seatAssignmentService,
        AdmitCardService admitCardService,
        MyExamScheduleService myExamScheduleService,
        ExamResultService examResultService,
        ExamResultPreNotificationService examResultPreNotificationService,
        ExamResultReleaseSettingService examResultReleaseSettingService,
        ExamResultImportService examResultImportService,
        ExamCertificateService examCertificateService,
        InvigilatorAssignmentService invigilatorAssignmentService
    ) {
        this.authorizationService = authorizationService;
        this.examCenterService = examCenterService;
        this.examRoomService = examRoomService;
        this.examSubjectService = examSubjectService;
        this.examSessionService = examSessionService;
        this.examScheduleService = examScheduleService;
        this.examRegistrationWindowService = examRegistrationWindowService;
        this.notificationService = notificationService;
        this.registrationService = registrationService;
        this.seatAssignmentService = seatAssignmentService;
        this.admitCardService = admitCardService;
        this.myExamScheduleService = myExamScheduleService;
        this.examResultService = examResultService;
        this.examResultPreNotificationService = examResultPreNotificationService;
        this.examResultReleaseSettingService = examResultReleaseSettingService;
        this.examResultImportService = examResultImportService;
        this.examCertificateService = examCertificateService;
        this.invigilatorAssignmentService = invigilatorAssignmentService;
    }

    @QueryMapping
    public List<ExamCenterModel> examCenters(DataFetchingEnvironment env) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "center.read");
        return examCenterService.listCenters();
    }

    @QueryMapping
    public ExamCenterModel examCenter(@Argument("id") Long id, DataFetchingEnvironment env) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "center.read");
        return examCenterService.getCenter(id);
    }

    @QueryMapping
    public List<ExamCenterModel> exportExamCenters(DataFetchingEnvironment env) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "center.read");
        return examCenterService.exportCenters();
    }

    @MutationMapping
    public ExamCenterModel createExamCenter(
        @Argument("input") @Valid ExamCenterInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "center.create");
        logAdminAction(user, "center.create", input.getName());
        return examCenterService.createCenter(input);
    }

    @MutationMapping
    public ExamCenterModel updateExamCenter(
        @Argument("id") Long id,
        @Argument("input") @Valid ExamCenterInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "center.update");
        logAdminAction(user, "center.update", "id=" + id);
        return examCenterService.updateCenter(id, input);
    }

    @MutationMapping
    public Boolean deleteExamCenter(@Argument("id") Long id, DataFetchingEnvironment env) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "center.update");
        logAdminAction(user, "center.delete", "id=" + id);
        return examCenterService.deleteCenter(id);
    }

    @MutationMapping
    public List<ExamCenterModel> importExamCenters(
        @Argument("inputs") @Valid List<ExamCenterInput> inputs,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "center.create");
        logAdminAction(user, "center.import", "count=" + inputs.size());
        return examCenterService.importCenters(inputs);
    }

    @QueryMapping
    public List<ExamRoomModel> examRooms(
        @Argument("centerId") Long centerId,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "room.read");
        return examRoomService.listRooms(centerId);
    }

    @QueryMapping
    public List<ExamRoomModel> exportExamRooms(
        @Argument("centerId") Long centerId,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "room.read");
        return examRoomService.exportRooms(centerId);
    }

    @QueryMapping
    public ExamRoomModel examRoom(@Argument("id") Long id, DataFetchingEnvironment env) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "room.read");
        return examRoomService.getRoom(id);
    }

    @MutationMapping
    public ExamRoomModel createExamRoom(
        @Argument("input") @Valid ExamRoomInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "room.create");
        logAdminAction(user, "room.create", "centerId=" + input.getCenterId());
        return examRoomService.createRoom(input);
    }

    @MutationMapping
    public List<ExamRoomModel> importExamRooms(
        @Argument("inputs") @Valid List<ExamRoomInput> inputs,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "room.create");
        logAdminAction(user, "room.import", "count=" + inputs.size());
        return examRoomService.importRooms(inputs);
    }

    @MutationMapping
    public ExamRoomModel updateExamRoom(
        @Argument("id") Long id,
        @Argument("input") @Valid ExamRoomInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "room.update");
        logAdminAction(user, "room.update", "id=" + id);
        return examRoomService.updateRoom(id, input);
    }

    @MutationMapping
    public ExamRoomModel changeExamRoomStatus(
        @Argument("input") @Valid UpdateExamRoomStatusInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "room.status.update");
        logAdminAction(user, "room.status.update", "roomId=" + input.getRoomId());
        return examRoomService.changeStatus(input.getRoomId(), input.getStatus(), input.getReason());
    }

    @QueryMapping
    public List<ExamSubjectModel> examSubjects(
        @Argument("keyword") String keyword,
        @Argument("status") String status,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "subject.read");
        return examSubjectService.listSubjects(keyword, status);
    }

    @QueryMapping
    public List<ExamSubjectModel> exportExamSubjects(
        @Argument("keyword") String keyword,
        @Argument("status") String status,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "subject.export");
        return examSubjectService.exportSubjects(keyword, status);
    }

    @QueryMapping
    public List<ExamSubjectLogModel> examSubjectLogs(
        @Argument("subjectId") Long subjectId,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "subject.read");
        return examSubjectService.getSubjectLogs(subjectId);
    }

    @QueryMapping
    public ExamSubjectModel examSubject(@Argument("id") Long id, DataFetchingEnvironment env) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "subject.read");
        return examSubjectService.getSubject(id);
    }

    @MutationMapping
    public ExamSubjectModel createExamSubject(
        @Argument("input") @Valid ExamSubjectInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "subject.create");
        logAdminAction(user, "subject.create", input.getCode());
        return examSubjectService.createSubject(input);
    }

    @MutationMapping
    public ExamSubjectModel updateExamSubject(
        @Argument("id") Long id,
        @Argument("input") @Valid ExamSubjectInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "subject.update");
        logAdminAction(user, "subject.update", "id=" + id);
        return examSubjectService.updateSubject(id, input);
    }

    @MutationMapping
    public ExamSubjectModel updateExamSubjectStatus(
        @Argument("input") @Valid UpdateExamSubjectStatusInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "subject.status.update");
        logAdminAction(user, "subject.status.update", "subjectId=" + input.getSubjectId());
        return examSubjectService.changeStatus(input, user.userId());
    }

    @MutationMapping
    public List<ExamSubjectModel> batchUpdateExamSubjectStatus(
        @Argument("input") @Valid BatchExamSubjectStatusInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "subject.status.update");
        logAdminAction(user, "subject.status.batch", "count=" + input.getSubjectIds().size());
        return examSubjectService.batchChangeStatus(input, user.userId());
    }

    @MutationMapping
    public Boolean batchDeleteExamSubjects(
        @Argument("input") @Valid BatchDeleteExamSubjectInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "subject.delete");
        logAdminAction(user, "subject.delete.batch", "count=" + input.getSubjectIds().size());
        return examSubjectService.batchDelete(input);
    }

    @MutationMapping
    public List<ExamSubjectModel> importExamSubjects(
        @Argument("inputs") @Valid List<ExamSubjectInput> inputs,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "subject.import");
        logAdminAction(user, "subject.import", "count=" + inputs.size());
        return examSubjectService.importSubjects(inputs);
    }

    @QueryMapping
    public List<ExamRegistrationWindowModel> examRegistrationWindows(
        @Argument("subjectId") Long subjectId,
        @Argument("status") String status,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "registration.read");
        return examRegistrationWindowService.list(subjectId, status);
    }

    @QueryMapping
    public ExamRegistrationWindowModel examRegistrationWindow(
        @Argument("id") Long id,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "registration.read");
        return examRegistrationWindowService.get(id);
    }

    @QueryMapping
    public List<ExamRegistrationWindowModel> exportExamRegistrationWindows(
        @Argument("subjectId") Long subjectId,
        @Argument("status") String status,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "registration.export");
        return examRegistrationWindowService.exportList(subjectId, status);
    }

    @QueryMapping
    public List<ExamRegistrationWindowLogModel> examRegistrationWindowLogs(
        @Argument("registrationId") Long registrationId,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "registration.read");
        return examRegistrationWindowService.logs(registrationId);
    }

    @MutationMapping
    public ExamRegistrationWindowModel upsertExamRegistrationWindow(
        @Argument("id") Long id,
        @Argument("input") @Valid ExamRegistrationWindowInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "registration.create");
        logAdminAction(user, "registration.upsert", "id=" + id);
        return examRegistrationWindowService.createOrUpdate(id, input);
    }

    @MutationMapping
    public ExamRegistrationWindowModel updateExamRegistrationWindowStatus(
        @Argument("input") @Valid UpdateExamRegistrationStatusInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "registration.status.update");
        logAdminAction(user, "registration.status.update", "id=" + input.getRegistrationId());
        return examRegistrationWindowService.updateStatus(input, user.userId());
    }

    @MutationMapping
    public List<ExamRegistrationWindowModel> batchUpdateExamRegistrationWindowStatus(
        @Argument("input") @Valid BatchUpdateExamRegistrationStatusInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "registration.status.update");
        logAdminAction(user, "registration.status.batch", "count=" + input.getRegistrationIds().size());
        return examRegistrationWindowService.batchUpdateStatus(input, user.userId());
    }

    @MutationMapping
    public Boolean batchDeleteExamRegistrationWindows(
        @Argument("input") @Valid BatchDeleteExamRegistrationInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "registration.delete");
        logAdminAction(user, "registration.delete.batch", "count=" + input.getRegistrationIds().size());
        return examRegistrationWindowService.batchDelete(input);
    }

    @QueryMapping
    public List<ExamRegistrationViewModel> availableExams(
        @Argument("keyword") String keyword,
        @Argument("status") String status,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        return examRegistrationWindowService.availableExams(keyword, status, user.userId());
    }

    @QueryMapping
    public ExamRegistrationViewModel availableExam(
        @Argument("id") Long id,
        DataFetchingEnvironment env
    ) {
        requireCurrentUser(env);
        return examRegistrationWindowService.availableExam(id);
    }

    @QueryMapping
    public List<NotificationModel> notifications(
        @Argument("keyword") String keyword,
        @Argument("status") String status,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "notification.read");
        return notificationService.list(keyword, status, user.userId(), user.roles());
    }

    @QueryMapping
    public NotificationModel notification(@Argument("id") Long id, DataFetchingEnvironment env) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "notification.read");
        return notificationService.detail(id);
    }

    @QueryMapping
    public List<NotificationTemplateModel> notificationTemplates(DataFetchingEnvironment env) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "notification.template");
        return notificationService.listTemplates();
    }

    @QueryMapping
    public NotificationTemplateModel notificationTemplate(@Argument("id") Long id, DataFetchingEnvironment env) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "notification.template");
        return notificationService.templateDetail(id);
    }

    @QueryMapping
    public List<NotificationLogModel> notificationLogs(@Argument("id") Long id, DataFetchingEnvironment env) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "notification.log");
        return notificationService.logs(id);
    }

    @MutationMapping
    public NotificationModel createNotification(
        @Argument("input") @Valid NotificationInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "notification.create");
        logAdminAction(user, "notification.create", input.getTitle());
        return notificationService.create(input, user.userId());
    }

    @MutationMapping
    public NotificationModel updateNotification(
        @Argument("id") Long id,
        @Argument("input") @Valid NotificationInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "notification.update");
        logAdminAction(user, "notification.update", "id=" + id);
        return notificationService.update(id, input, user.userId());
    }

    @MutationMapping
    public NotificationModel publishNotification(
        @Argument("input") @Valid PublishNotificationInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "notification.publish");
        logAdminAction(user, "notification.publish", "id=" + input.getNotificationId());
        return notificationService.publish(input, user.userId());
    }

    @MutationMapping
    public NotificationModel withdrawNotification(
        @Argument("input") @Valid PublishNotificationInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "notification.withdraw");
        logAdminAction(user, "notification.withdraw", "id=" + input.getNotificationId());
        return notificationService.withdraw(input, user.userId());
    }

    @MutationMapping
    public NotificationTemplateModel createNotificationTemplate(
        @Argument("input") @Valid NotificationTemplateInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "notification.template");
        logAdminAction(user, "notification.template.create", input.getName());
        return notificationService.createTemplate(input);
    }

    @MutationMapping
    public NotificationTemplateModel updateNotificationTemplate(
        @Argument("id") Long id,
        @Argument("input") @Valid NotificationTemplateInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "notification.template");
        logAdminAction(user, "notification.template.update", "id=" + id);
        return notificationService.updateTemplate(id, input);
    }

    @QueryMapping
    public RegistrationInfoModel registrationInfo(
        @Argument("subjectId") Long subjectId,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        return registrationService.current(user.userId(), subjectId);
    }

    @QueryMapping
    public List<RegistrationMaterialTemplateModel> registrationMaterialTemplates(DataFetchingEnvironment env) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "registration.material.template");
        return registrationService.listTemplates();
    }

    @QueryMapping
    public List<RegistrationInfoModel> pendingRegistrations(DataFetchingEnvironment env) {
        JwtPayload user = requireCurrentUser(env);
        if (!authorizationService.hasRole(user, "admin")) {
            authorizationService.ensureHasPermission(user, "registration.audit");
        }
        return registrationService.pendingList();
    }

    @QueryMapping
    public RegistrationInfoModel registrationAuditDetail(
        @Argument("id") Long id,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        if (!authorizationService.hasRole(user, "admin")) {
            authorizationService.ensureHasPermission(user, "registration.audit");
        }
        return registrationService.auditDetail(id);
    }

    @QueryMapping
    public List<RegistrationAuditLogModel> registrationAuditLogs(
        @Argument("registrationInfoId") Long registrationInfoId,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        if (!authorizationService.hasRole(user, "admin")) {
            authorizationService.ensureHasPermission(user, "registration.audit");
        }
        return registrationService.auditLogs(registrationInfoId);
    }

    @MutationMapping
    public RegistrationInfoModel upsertRegistrationInfo(
        @Argument("input") @Valid RegistrationInfoInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        return registrationService.upsertInfo(user.userId(), input);
    }

    @MutationMapping
    public RegistrationMaterialModel uploadRegistrationMaterial(
        @Argument("input") @Valid RegistrationMaterialInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        return registrationService.addOrUpdateMaterial(user.userId(), input);
    }

    @MutationMapping
    public Boolean deleteRegistrationMaterial(
        @Argument("id") Long id,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        return registrationService.deleteMaterial(user.userId(), id);
    }

    @MutationMapping
    public RegistrationMaterialTemplateModel upsertRegistrationMaterialTemplate(
        @Argument("input") @Valid RegistrationMaterialTemplateInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "registration.material.template");
        logAdminAction(user, "registration.material.template.upsert", input.getType());
        RegistrationMaterialTemplateModel model = new RegistrationMaterialTemplateModel(
            input.getId() != null ? Long.valueOf(input.getId()) : null,
            input.getType(),
            input.getAllowedFormats(),
            input.getMaxSize() != null ? Long.valueOf(input.getMaxSize()) : null,
            input.getRequired(),
            input.getDescription()
        );
        return registrationService.upsertTemplate(model);
    }

    @MutationMapping
    public Boolean deleteRegistrationMaterialTemplate(
        @Argument("id") Long id,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "registration.material.template");
        logAdminAction(user, "registration.material.template.delete", "id=" + id);
        return registrationService.deleteTemplate(id);
    }

    @MutationMapping
    public RegistrationInfoModel approveRegistration(
        @Argument("registrationInfoId") Long registrationInfoId,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        if (!authorizationService.hasRole(user, "admin")) {
            authorizationService.ensureHasPermission(user, "registration.audit");
        }
        logAdminAction(user, "registration.approve", "id=" + registrationInfoId);
        return registrationService.approve(registrationInfoId, user.userId());
    }

    @MutationMapping
    public RegistrationInfoModel rejectRegistration(
        @Argument("input") @Valid RegistrationRejectInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        if (!authorizationService.hasRole(user, "admin")) {
            authorizationService.ensureHasPermission(user, "registration.audit");
        }
        logAdminAction(user, "registration.reject", "id=" + input.getRegistrationInfoId());
        return registrationService.reject(input.getRegistrationInfoId(), input.getReason(), user.userId());
    }

    @QueryMapping
    public List<SeatAssignmentModel> seatAssignments(
        @Argument("subjectId") Long subjectId,
        @Argument("sessionId") Long sessionId,
        @Argument("roomId") Long roomId,
        @Argument("registrationInfoId") Long registrationInfoId,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "schedule.read");
        return seatAssignmentService.list(subjectId, sessionId, roomId, registrationInfoId);
    }

    @QueryMapping
    public SeatAssignmentStatsModel seatAssignmentStats(
        @Argument("subjectId") Long subjectId,
        @Argument("sessionId") Long sessionId,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "schedule.read");
        return seatAssignmentService.stats(subjectId, sessionId);
    }

    @QueryMapping
    public List<SeatAssignmentTaskModel> seatAssignmentTasks(
        @Argument("subjectId") Long subjectId,
        @Argument("sessionId") Long sessionId,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "schedule.read");
        return seatAssignmentService.tasks(subjectId, sessionId);
    }

    @MutationMapping
    public List<SeatAssignmentModel> assignSeats(
        @Argument("input") @Valid SeatAssignmentInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "schedule.update");
        logAdminAction(user, "seat.assign", "subjectId=" + input.getSubjectId() + ", sessionId=" + input.getSessionId());
        return seatAssignmentService.assignSeats(input, user.userId());
    }

    @MutationMapping
    public Boolean resetSeats(
        @Argument("subjectId") Long subjectId,
        @Argument("sessionId") Long sessionId,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "schedule.update");
        logAdminAction(user, "seat.reset", "subjectId=" + subjectId + ", sessionId=" + sessionId);
        return seatAssignmentService.reset(subjectId, sessionId);
    }

    @QueryMapping
    public AdmitCardModel admitCard(
        @Argument("registrationInfoId") Long registrationInfoId,
        @Argument("templateId") Long templateId,
        DataFetchingEnvironment env
    ) {
        requireCurrentUser(env);
        return admitCardService.myAdmitCard(registrationInfoId, templateId);
    }

    @QueryMapping
    public List<AdmitCardTemplateModel> admitCardTemplates(DataFetchingEnvironment env) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "registration.material.template");
        return admitCardService.listTemplates().stream()
            .map(t -> new AdmitCardTemplateModel(t.getId(), t.getName(), t.getLogoUrl(), t.getExamNotice(), t.getQrStyle()))
            .toList();
    }

    @QueryMapping
    public List<AdmitCardLogModel> admitCardLogs(
        @Argument("registrationInfoId") Long registrationInfoId,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        boolean isAdmin = authorizationService.hasRole(user, "admin");
        boolean isTeacher = authorizationService.hasRole(user, "teacher");
        if (!isAdmin && !isTeacher) {
            boolean owned = admitCardService.isRegistrationOwnedBy(registrationInfoId, user.userId());
            if (!owned) {
                throw new IllegalStateException("无权查看该报名的准考证日志");
            }
        }
        return admitCardService.logs(registrationInfoId).stream()
            .map(log -> new AdmitCardLogModel(
                log.getId(),
                log.getRegistrationInfoId(),
                log.getTicketNumber(),
                log.getFilePath(),
                log.getStatus(),
                log.getMessage(),
                log.getCreatedAt() != null ? log.getCreatedAt().toString() : null
            ))
            .toList();
    }

    @MutationMapping
    public AdmitCardModel refreshAdmitCard(
        @Argument("registrationInfoId") Long registrationInfoId,
        @Argument("templateId") Long templateId,
        DataFetchingEnvironment env
    ) {
        requireCurrentUser(env);
        return admitCardService.refreshAdmitCard(registrationInfoId, templateId);
    }

    @MutationMapping
    public AdmitCardTemplateModel upsertAdmitCardTemplate(
        @Argument("input") @Valid AdmitCardTemplateInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "registration.material.template");
        logAdminAction(user, "admitCard.template.upsert", input.getName());
        var saved = admitCardService.upsertTemplate(input);
        return new AdmitCardTemplateModel(saved.getId(), saved.getName(), saved.getLogoUrl(), saved.getExamNotice(), saved.getQrStyle());
    }

    @MutationMapping
    public Boolean deleteAdmitCardTemplate(
        @Argument("id") Long id,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "registration.material.template");
        logAdminAction(user, "admitCard.template.delete", "id=" + id);
        return admitCardService.deleteTemplate(id);
    }

    @QueryMapping
    public List<MyExamScheduleModel> myExamSchedules(
        @Argument("subjectId") Long subjectId,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        return myExamScheduleService.list(user.userId(), subjectId);
    }

    @QueryMapping
    public ExamResultModel examResult(
        @Argument("input") @Valid ExamResultQueryInput input,
        DataFetchingEnvironment env
    ) {
        requireCurrentUser(env);
        return examResultService.queryResult(input);
    }

    @QueryMapping
    public List<ExamResultModel> examResultsByRegistrationIds(
        @Argument("registrationInfoIds") List<Long> registrationInfoIds,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "result.manage");
        return examResultService.queryResultsByRegistrationIds(registrationInfoIds);
    }

    @QueryMapping
    public List<ExamResultPreNotificationModel> resultPreNotifications(DataFetchingEnvironment env) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "result.manage");
        return examResultPreNotificationService.list();
    }

    @QueryMapping
    public ExamResultPreNotificationModel resultPreNotification(
        @Argument("id") Long id,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "result.manage");
        return examResultPreNotificationService.detail(id);
    }

    @QueryMapping
    public List<ExamResultReleaseSettingModel> resultReleaseSettings(
        @Argument("subjectId") Long subjectId,
        @Argument("examYear") Integer examYear,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "result.manage");
        return examResultReleaseSettingService.list(subjectId, examYear);
    }

    @QueryMapping
    public ExamResultReleaseSettingModel resultReleaseSetting(
        @Argument("id") Long id,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "result.manage");
        return examResultReleaseSettingService.detail(id);
    }

    @QueryMapping
    public List<InvigilatorAssignmentModel> invigilatorAssignments(
        @Argument("scheduleId") Long scheduleId,
        @Argument("teacherUserId") Long teacherUserId,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "schedule.read");
        return invigilatorAssignmentService.list(scheduleId, teacherUserId);
    }

    @QueryMapping
    public InvigilatorAssignmentStatsModel invigilatorAssignmentStats(
        @Argument("subjectId") Long subjectId,
        @Argument("sessionId") Long sessionId,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "schedule.read");
        return invigilatorAssignmentService.stats(subjectId, sessionId);
    }

    @QueryMapping
    public ExamCertificateFileModel examCertificate(
        @Argument("input") @Valid com.ruangong.model.input.ExamCertificateRequestInput input,
        DataFetchingEnvironment env
    ) {
        requireCurrentUser(env);
        return examCertificateService.downloadCertificate(input);
    }

    @QueryMapping
    public ExamResultImportTemplateModel resultImportTemplate(
        @Argument("format") String format,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "result.manage");
        return examResultImportService.template(format);
    }

    @QueryMapping
    public List<ExamResultImportJobModel> examResultImportJobs(
        @Argument("status") ExamResultImportJobStatus status,
        @Argument("limit") Integer limit,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "result.manage");
        return examResultImportService.jobs(status, limit);
    }

    @QueryMapping
    public ExamResultImportJobModel examResultImportJob(
        @Argument("id") Long id,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "result.manage");
        return examResultImportService.job(id);
    }

    @MutationMapping
    public ExamResultPreNotificationModel createResultPreNotification(
        @Argument("input") @Valid ExamResultPreNotificationInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "result.manage");
        logAdminAction(user, "result.pre.notice.create", input.getExamType() + "-" + input.getExamYear());
        return examResultPreNotificationService.create(input, user.userId());
    }

    @MutationMapping
    public ExamResultPreNotificationModel updateResultPreNotification(
        @Argument("id") Long id,
        @Argument("input") @Valid ExamResultPreNotificationInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "result.manage");
        logAdminAction(user, "result.pre.notice.update", "id=" + id);
        return examResultPreNotificationService.update(id, input, user.userId());
    }

    @MutationMapping
    public ExamResultPreNotificationModel publishResultPreNotification(
        @Argument("id") Long id,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "result.manage");
        logAdminAction(user, "result.pre.notice.publish", "id=" + id);
        return examResultPreNotificationService.publish(id, user.userId());
    }

    @MutationMapping
    public ExamResultImportJobModel importExamResults(
        @Argument("input") @Valid ExamResultImportInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "result.manage");
        logAdminAction(user, "result.import", input.getFileName());
        return examResultImportService.importFile(input, user.userId());
    }

    @MutationMapping
    public ExamResultReleaseSettingModel upsertResultReleaseSetting(
        @Argument("id") Long id,
        @Argument("input") @Valid ExamResultReleaseSettingInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "result.manage");
        logAdminAction(
            user,
            "result.release.setting.upsert",
            "subjectId=" + input.getSubjectId() + ", year=" + input.getExamYear()
        );
        return examResultReleaseSettingService.upsert(id, input, user.userId());
    }

    @MutationMapping
    public List<ExamResultReleaseSettingModel> batchSetResultRelease(
        @Argument("input") @Valid ExamResultReleaseBatchInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "result.manage");
        logAdminAction(
            user,
            "result.release.setting.batch",
            "count=" + input.getSubjectIds().size() + ", year=" + input.getExamYear()
        );
        return examResultReleaseSettingService.batchUpsert(input, user.userId());
    }

    @MutationMapping
    public List<InvigilatorAssignmentModel> assignInvigilators(
        @Argument("input") @Valid AssignInvigilatorsInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "schedule.update");
        logAdminAction(user, "invigilator.assign", "scheduleId=" + input.getScheduleId());
        return invigilatorAssignmentService.assign(input, user.userId());
    }

    @MutationMapping
    public Boolean removeInvigilatorAssignment(
        @Argument("id") Long id,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "schedule.update");
        logAdminAction(user, "invigilator.remove", "id=" + id);
        return invigilatorAssignmentService.remove(id);
    }

    @MutationMapping
    public ExamResultModel upsertExamResult(
        @Argument("input") @Valid UpsertExamResultInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "result.manage");
        logAdminAction(user, "result.upsert", "registrationInfoId=" + input.getRegistrationInfoId());
        return examResultService.upsertResult(input);
    }

    @QueryMapping
    public List<ExamSessionModel> examSessions(DataFetchingEnvironment env) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "session.read");
        return examSessionService.listSessions();
    }

    @QueryMapping
    public ExamSessionModel examSession(@Argument("id") Long id, DataFetchingEnvironment env) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "session.read");
        return examSessionService.getSession(id);
    }

    @MutationMapping
    public ExamSessionModel createExamSession(
        @Argument("input") @Valid ExamSessionInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "session.create");
        logAdminAction(user, "session.create", input.getName());
        return examSessionService.createSession(input);
    }

    @MutationMapping
    public ExamSessionModel updateExamSession(
        @Argument("id") Long id,
        @Argument("input") @Valid ExamSessionInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "session.update");
        logAdminAction(user, "session.update", "id=" + id);
        return examSessionService.updateSession(id, input);
    }

    @QueryMapping
    public List<ExamScheduleModel> examSchedules(
        @Argument("roomId") Long roomId,
        @Argument("subjectId") Long subjectId,
        @Argument("sessionId") Long sessionId,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "schedule.read");
        return examScheduleService.listSchedules(roomId, subjectId, sessionId);
    }

    @QueryMapping
    public ExamScheduleModel examSchedule(@Argument("id") Long id, DataFetchingEnvironment env) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "schedule.read");
        return examScheduleService.getSchedule(id);
    }

    @MutationMapping
    public ExamScheduleModel createExamSchedule(
        @Argument("input") @Valid ExamScheduleInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "schedule.create");
        logAdminAction(user, "schedule.create", "roomId=" + input.getRoomId());
        return examScheduleService.createSchedule(input);
    }

    @MutationMapping
    public ExamScheduleModel updateExamSchedule(
        @Argument("id") Long id,
        @Argument("input") @Valid ExamScheduleInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "schedule.update");
        logAdminAction(user, "schedule.update", "id=" + id);
        return examScheduleService.updateSchedule(id, input);
    }

    @MutationMapping
    public ExamScheduleModel updateExamScheduleStatus(
        @Argument("input") @Valid UpdateExamScheduleStatusInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "schedule.status.update");
        logAdminAction(user, "schedule.status.update", "scheduleId=" + input.getScheduleId());
        return examScheduleService.updateScheduleStatus(input);
    }

    @MutationMapping
    public Boolean deleteExamSchedule(@Argument("id") Long id, DataFetchingEnvironment env) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "schedule.delete");
        logAdminAction(user, "schedule.delete", "id=" + id);
        return examScheduleService.deleteSchedule(id);
    }

    private JwtPayload requireCurrentUser(DataFetchingEnvironment env) {
        JwtPayload payload = env.getGraphQlContext().get("currentUser");
        if (payload == null) {
            throw new IllegalStateException("未登录");
        }
        return payload;
    }

    private void logAdminAction(JwtPayload actor, String action, String detail) {
        if (actor == null) {
            return;
        }
        log.info(
            "Audit action={} actorId={} roles={} tokenVersion={} detail={}",
            action,
            actor.userId(),
            actor.roles(),
            actor.tokenVersion(),
            detail
        );
    }
}
