package com.ruangong.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ruangong.entity.ExamResultDetailEntity;
import com.ruangong.entity.ExamResultRecordEntity;
import com.ruangong.entity.ExamSubjectEntity;
import com.ruangong.entity.RegistrationInfoEntity;
import com.ruangong.model.ExamResultModel;
import com.ruangong.model.input.ExamResultDetailInput;
import com.ruangong.model.input.ExamResultQueryInput;
import com.ruangong.model.input.UpsertExamResultInput;
import com.ruangong.repository.ExamResultRecordRepository;
import com.ruangong.repository.ExamSubjectRepository;
import com.ruangong.repository.RegistrationInfoRepository;
import java.lang.reflect.Proxy;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExamResultServiceTest {

    private final InMemoryStore store = new InMemoryStore();
    private ExamResultService examResultService;
    private RegistrationInfoEntity registrationInfo;
    private ExamSubjectEntity subject;

    @BeforeEach
    void setup() {
        ExamResultRecordRepository recordRepository = createRecordRepository();
        RegistrationInfoRepository registrationInfoRepository = createRegistrationRepository();
        ExamSubjectRepository examSubjectRepository = createSubjectRepository();
        ExamResultReleaseSettingService releaseSettingService = new NoopReleaseSettingService();
        examResultService = new ExamResultService(
            recordRepository,
            registrationInfoRepository,
            examSubjectRepository,
            releaseSettingService
        );

        subject = new ExamSubjectEntity();
        subject.setId(store.nextSubjectId++);
        subject.setCode("SUB-A");
        subject.setName("法规");
        subject.setDurationMinutes(120);
        subject.setQuestionCount(100);
        store.subjects.put(subject.getId(), subject);

        registrationInfo = new RegistrationInfoEntity();
        registrationInfo.setId(store.nextRegistrationId++);
        registrationInfo.setFullName("张三");
        registrationInfo.setIdCardNumber("110101199001010011");
        registrationInfo.setSubject(subject);
        store.registrations.put(registrationInfo.getId(), registrationInfo);
    }

    @Test
    void queryResult_returnsResultWhenReleased() {
        ExamResultRecordEntity record = new ExamResultRecordEntity();
        record.setId(store.nextResultId++);
        record.setRegistrationInfo(registrationInfo);
        record.setExamType("一级建造师");
        record.setExamYear(2025);
        record.setTicketNumber("T2025");
        record.setReleaseTime(OffsetDateTime.now().minusDays(1));

        ExamResultDetailEntity detail = new ExamResultDetailEntity();
        detail.setResult(record);
        detail.setSubject(subject);
        detail.setSubjectName(subject.getName());
        detail.setScore(96.0);
        record.getDetails().add(detail);

        store.results.put(record.getId(), record);

        ExamResultQueryInput input = new ExamResultQueryInput();
        input.setExamType("一级建造师");
        input.setExamYear(2025);
        input.setTicketNumber("T2025");

        ExamResultModel model = examResultService.queryResult(input);
        assertEquals("张三", model.fullName());
        assertEquals(96.0, model.subjects().get(0).score());
    }

    @Test
    void queryResult_throwsWhenNotReleased() {
        ExamResultRecordEntity record = new ExamResultRecordEntity();
        record.setId(store.nextResultId++);
        record.setRegistrationInfo(registrationInfo);
        record.setExamType("教资");
        record.setExamYear(2024);
        record.setTicketNumber("TK001");
        record.setReleaseTime(OffsetDateTime.now().plusDays(2));
        store.results.put(record.getId(), record);

        ExamResultQueryInput input = new ExamResultQueryInput();
        input.setExamType("教资");
        input.setExamYear(2024);
        input.setTicketNumber("TK001");

        assertThrows(IllegalArgumentException.class, () -> examResultService.queryResult(input));
    }

    @Test
    void upsertResult_writesTotalsAndSubjects() {
        ExamResultDetailInput detailInput = new ExamResultDetailInput();
        detailInput.setSubjectId(subject.getId());
        detailInput.setSubjectName(subject.getName());
        detailInput.setScore(88.0);
        detailInput.setPassLine(60.0);
        detailInput.setIsPass(true);
        detailInput.setNationalRank(150);
        detailInput.setRemark("首批考生");

        UpsertExamResultInput input = new UpsertExamResultInput();
        input.setRegistrationInfoId(registrationInfo.getId());
        input.setExamType("一级建造师");
        input.setExamYear(2025);
        input.setTicketNumber("GK2025");
        input.setReleaseTime(OffsetDateTime.now().plusDays(3).toString());
        input.setTotalScore(330.0);
        input.setTotalPassLine(240.0);
        input.setQualificationStatus("待确认");
        input.setQualificationNote("批次A");
        input.setReportUrl("https://example.com/report.pdf");
        input.setSubjects(List.of(detailInput));

        ExamResultModel model = examResultService.upsertResult(input);
        assertEquals(330.0, model.totalScore());
        assertEquals(1, model.subjects().size());
        assertEquals(subject.getId(), model.subjects().get(0).subjectId());
    }

    private ExamResultRecordRepository createRecordRepository() {
        return (ExamResultRecordRepository) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class[] { ExamResultRecordRepository.class },
            (proxy, method, args) -> switch (method.getName()) {
                case "findByExamTypeIgnoreCaseAndExamYearAndTicketNumberIgnoreCase" -> store.results.values().stream()
                    .filter(it -> equalsIgnoreCase(it.getExamType(), (String) args[0]))
                    .filter(it -> it.getExamYear().equals(args[1]))
                    .filter(it -> equalsIgnoreCase(it.getTicketNumber(), (String) args[2]))
                    .findFirst();
                case "findByExamTypeIgnoreCaseAndExamYearAndRegistrationInfo_FullNameIgnoreCaseAndRegistrationInfo_IdCardNumber" ->
                    store.results.values().stream()
                        .filter(it -> equalsIgnoreCase(it.getExamType(), (String) args[0]))
                        .filter(it -> it.getExamYear().equals(args[1]))
                        .filter(it -> it.getRegistrationInfo() != null)
                        .filter(it -> equalsIgnoreCase(it.getRegistrationInfo().getFullName(), (String) args[2]))
                        .filter(it -> equalsIgnoreCase(it.getRegistrationInfo().getIdCardNumber(), (String) args[3]))
                        .findFirst();
                case "findByRegistrationInfo_Id" -> store.results.values().stream()
                    .filter(it -> it.getRegistrationInfo() != null && it.getRegistrationInfo().getId().equals(args[0]))
                    .findFirst();
                case "save" -> {
                    ExamResultRecordEntity entity = (ExamResultRecordEntity) args[0];
                    if (entity.getId() == null) {
                        entity.setId(store.nextResultId++);
                    }
                    store.results.put(entity.getId(), entity);
                    yield entity;
                }
                default -> defaultInvocation(method.getName());
            }
        );
    }

    private RegistrationInfoRepository createRegistrationRepository() {
        return (RegistrationInfoRepository) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class[] { RegistrationInfoRepository.class },
            (proxy, method, args) -> switch (method.getName()) {
                case "findById" -> Optional.ofNullable(store.registrations.get(args[0]));
                default -> defaultInvocation(method.getName());
            }
        );
    }

    private ExamSubjectRepository createSubjectRepository() {
        return (ExamSubjectRepository) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class[] { ExamSubjectRepository.class },
            (proxy, method, args) -> switch (method.getName()) {
                case "findById" -> Optional.ofNullable(store.subjects.get(args[0]));
                default -> defaultInvocation(method.getName());
            }
        );
    }

    private Object defaultInvocation(String methodName) {
        if ("toString".equals(methodName)) {
            return "InMemoryRepository";
        }
        if ("hashCode".equals(methodName)) {
            return System.identityHashCode(this);
        }
        if ("equals".equals(methodName)) {
            return false;
        }
        throw new UnsupportedOperationException("Unsupported repository method: " + methodName);
    }

    private boolean equalsIgnoreCase(String a, String b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equalsIgnoreCase(b);
    }

    private static class InMemoryStore {

        private final Map<Long, ExamResultRecordEntity> results = new HashMap<>();
        private final Map<Long, RegistrationInfoEntity> registrations = new HashMap<>();
        private final Map<Long, ExamSubjectEntity> subjects = new HashMap<>();
        private long nextResultId = 1;
        private long nextRegistrationId = 1;
        private long nextSubjectId = 1;
    }

    private static class NoopReleaseSettingService extends ExamResultReleaseSettingService {

        NoopReleaseSettingService() {
            super(null, null, null, null, null);
        }

        @Override
        public void processSchedules() {
            // no-op for unit tests
        }

        @Override
        public void applyReleaseTimeIfReleased(ExamResultRecordEntity entity) {
            // no-op for unit tests
        }

        @Override
        public OffsetDateTime plannedReleaseTime(Long subjectId, Integer examYear) {
            return null;
        }
    }
}
