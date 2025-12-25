package com.ruangong.graphql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import com.ruangong.entity.RoleEntity;
import com.ruangong.entity.UserEntity;
import com.ruangong.repository.RoleRepository;
import com.ruangong.repository.UserRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.junit.jupiter.api.AfterAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureWebTestClient
class ExamResultManagementGraphqlIntegrationTest {

    private static final String ADMIN_PHONE = "13899990004";
    private static final String ADMIN_PASSWORD = "Admin@2024";
    private static final String STUDENT_PHONE = "13900001114";
    private static final String STUDENT_PASSWORD = "Student@123";

    private static EmbeddedPostgres embeddedPostgres;

    static {
        try {
            embeddedPostgres = EmbeddedPostgres.builder().setPort(0).start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start embedded Postgres", e);
        }
    }

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
            () -> embeddedPostgres.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
        registry.add("spring.datasource.driver-class-name",
            () -> "org.postgresql.Driver");
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.sql.init.schema-locations", () -> "classpath:test-schema.sql");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }
    
    @AfterAll
    void stopEmbeddedPostgres() {
        if (embeddedPostgres != null) {
            try {
                embeddedPostgres.close();
            } catch (IOException ignored) {
            }
        }
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;
    
    @Autowired
    private WebTestClient baseWebTestClient;

    private String adminToken;
    private String studentToken;
    private String subjectId;
    private String registrationId;

    @BeforeEach
    void setupTestData() {
        userRepository.deleteAll();
        RoleEntity adminRole = roleRepository.findByName("admin").orElseThrow();
        RoleEntity studentRole = roleRepository.findByName("student").orElseThrow();

        UserEntity admin = new UserEntity();
        admin.setPhone(ADMIN_PHONE);
        admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setIsActive(true);
        admin.setRoles(new HashSet<>());
        admin.getRoles().add(adminRole);
        admin.getRoles().add(studentRole);
        userRepository.save(admin);

        UserEntity student = new UserEntity();
        student.setPhone(STUDENT_PHONE);
        student.setPasswordHash(passwordEncoder.encode(STUDENT_PASSWORD));
        student.setIsActive(true);
        student.setRoles(new HashSet<>());
        student.getRoles().add(studentRole);
        userRepository.save(student);

        this.webTestClient = baseWebTestClient.mutate()
            .baseUrl("http://localhost:" + port + "/graphql")
            .build();
        
        this.adminToken = loginAndGetToken(ADMIN_PHONE, ADMIN_PASSWORD);
        this.studentToken = loginAndGetToken(STUDENT_PHONE, STUDENT_PASSWORD);

        setupExamAndRegistrationData();
    }

    private void setupExamAndRegistrationData() {
        Map<String, Object> subjectInput = new HashMap<>();
        subjectInput.put("code", "SUB007");
        subjectInput.put("name", "建筑工程案例分析");
        subjectInput.put("durationMinutes", 240);
        subjectInput.put("questionCount", 200);

        JsonNode subjectResponse = executeGraphQl(
            "mutation CreateSubject($input: ExamSubjectInput!) {" +
                " createExamSubject(input: $input) { id code name } }",
            Map.of("input", subjectInput),
            adminToken
        );

        this.subjectId = subjectResponse.path("createExamSubject").path("id").asText();

        Map<String, Object> windowInput = new HashMap<>();
        windowInput.put("subjectId", subjectId);
        windowInput.put("registrationStartTime", "2025-01-01T00:00:00Z");
        windowInput.put("registrationEndTime", "2025-12-31T23:59:59Z");

        executeGraphQl(
            "mutation CreateWindow($input: ExamRegistrationWindowInput!) {" +
                " upsertExamRegistrationWindow(input: $input) { id } }",
            Map.of("input", windowInput),
            adminToken
        );

        Map<String, Object> registrationInput = new HashMap<>();
        registrationInput.put("subjectId", subjectId);
        registrationInput.put("fullName", "赵六");
        registrationInput.put("idCardNumber", "110101199004040044");
        registrationInput.put("gender", "女");
        registrationInput.put("birthDate", "1990-04-04");
        registrationInput.put("email", "zhaoliu@example.com");
        registrationInput.put("address", "杭州市西湖区");
        registrationInput.put("phone", "13600136000");
        registrationInput.put("education", "硕士");
        registrationInput.put("workUnit", "某设计院");
        registrationInput.put("workExperience", "6年");

        JsonNode registrationResponse = executeGraphQl(
            "mutation SubmitRegistration($input: RegistrationInfoInput!) {" +
                " upsertRegistrationInfo(input: $input) { id fullName status } }",
            Map.of("input", registrationInput),
            studentToken
        );

        this.registrationId = registrationResponse.path("upsertRegistrationInfo").path("id").asText();

        executeGraphQl(
            "mutation ApproveRegistration($registrationInfoId: ID!) {" +
                " approveRegistration(registrationInfoId: $registrationInfoId) { id status } }",
            Map.of("registrationInfoId", registrationId),
            adminToken
        );
    }

    @Test
    void examResultManagement_crudOperations() {
        Map<String, Object> detailInput = new HashMap<>();
        detailInput.put("subjectId", subjectId);
        detailInput.put("subjectName", "建筑工程案例分析");
        detailInput.put("score", 85.5);
        detailInput.put("passLine", 60.0);
        detailInput.put("isPass", true);
        detailInput.put("nationalRank", 1200);
        detailInput.put("remark", "表现良好");

        Map<String, Object> resultInput = new HashMap<>();
        resultInput.put("registrationInfoId", registrationId);
        resultInput.put("examType", "一级建造师");
        resultInput.put("examYear", 2025);
        resultInput.put("ticketNumber", "T2025001");
        resultInput.put("releaseTime", "2025-06-25T00:00:00Z");
        resultInput.put("totalScore", 342.0);
        resultInput.put("totalPassLine", 240.0);
        resultInput.put("qualificationStatus", "待审核");
        resultInput.put("qualificationNote", "符合条件");
        resultInput.put("reportUrl", "https://example.com/report.pdf");
        resultInput.put("subjects", Map.of("detailInput", detailInput));

        JsonNode upsertResponse = executeGraphQl(
            "mutation UpsertResult($input: UpsertExamResultInput!) {" +
                " upsertExamResult(input: $input) { id fullName examType examYear ticketNumber totalScore qualificationStatus } }",
            Map.of("input", resultInput),
            adminToken
        );

        Assertions.assertEquals("赵六", upsertResponse.path("upsertExamResult").path("fullName").asText());
        Assertions.assertEquals("一级建造师", upsertResponse.path("upsertExamResult").path("examType").asText());
        Assertions.assertEquals(342.0, upsertResponse.path("upsertExamResult").path("totalScore").asDouble());
        Assertions.assertEquals("待审核", upsertResponse.path("upsertExamResult").path("qualificationStatus").asText());
    }

    @Test
    void examResult_queryByStudent() {
        Map<String, Object> detailInput = new HashMap<>();
        detailInput.put("subjectId", subjectId);
        detailInput.put("subjectName", "建筑工程案例分析");
        detailInput.put("score", 78.0);
        detailInput.put("passLine", 60.0);
        detailInput.put("isPass", true);

        Map<String, Object> resultInput = new HashMap<>();
        resultInput.put("registrationInfoId", registrationId);
        resultInput.put("examType", "一级建造师");
        resultInput.put("examYear", 2025);
        resultInput.put("ticketNumber", "T2025002");
        resultInput.put("releaseTime", "2025-06-20T00:00:00Z");
        resultInput.put("totalScore", 315.0);
        resultInput.put("totalPassLine", 240.0);
        resultInput.put("subjects", Map.of("detailInput", detailInput));

        executeGraphQl(
            "mutation UpsertResult($input: UpsertExamResultInput!) {" +
                " upsertExamResult(input: $input) { id } }",
            Map.of("input", resultInput),
            adminToken
        );

        Map<String, Object> queryInput = new HashMap<>();
        queryInput.put("examType", "一级建造师");
        queryInput.put("examYear", 2025);
        queryInput.put("ticketNumber", "T2025002");

        JsonNode queryResponse = executeGraphQl(
            "query QueryResult($input: ExamResultQueryInput!) {" +
                " examResult(input: $input) { fullName examType examYear ticketNumber totalScore subjects { subjectName score isPass } } }",
            Map.of("input", queryInput),
            studentToken
        );

        Assertions.assertEquals("赵六", queryResponse.path("examResult").path("fullName").asText());
        Assertions.assertEquals(315.0, queryResponse.path("examResult").path("totalScore").asDouble());
        Assertions.assertTrue(queryResponse.path("examResult").path("subjects").size() > 0);
        Assertions.assertEquals("建筑工程案例分析", queryResponse.path("examResult").path("subjects").get(0).path("subjectName").asText());
        Assertions.assertEquals(78.0, queryResponse.path("examResult").path("subjects").get(0).path("score").asDouble());
    }

    @Test
    void resultPreNotificationManagement() {
        Map<String, Object> notificationInput = new HashMap<>();
        notificationInput.put("title", "2025年一级建造师成绩查询预告");
        notificationInput.put("content", "成绩将于2025年6月25日上午9:00发布，请考生做好准备。");
        notificationInput.put("scheduledTime", "2025-06-24T09:00:00Z");
        notificationInput.put("isActive", true);

        JsonNode createNotificationResponse = executeGraphQl(
            "mutation CreatePreNotification($input: ExamResultPreNotificationInput!) {" +
                " createResultPreNotification(input: $input) { id title content scheduledTime isActive } }",
            Map.of("input", notificationInput),
            adminToken
        );

        String notificationId = createNotificationResponse.path("createResultPreNotification").path("id").asText();
        Assertions.assertEquals("2025年一级建造师成绩查询预告", createNotificationResponse.path("createResultPreNotification").path("title").asText());
        Assertions.assertTrue(createNotificationResponse.path("createResultPreNotification").path("isActive").asBoolean());

        JsonNode notificationsResponse = executeGraphQl(
            "query GetPreNotifications { resultPreNotifications { id title content scheduledTime isActive } }",
            Map.of(),
            adminToken
        );

        Assertions.assertTrue(notificationsResponse.path("resultPreNotifications").size() > 0);
        Assertions.assertEquals("2025年一级建造师成绩查询预告", notificationsResponse.path("resultPreNotifications").get(0).path("title").asText());

        Map<String, Object> updateInput = new HashMap<>();
        updateInput.put("title", "2025年一级建造师成绩查询预告（更新）");
        updateInput.put("content", "成绩将于2025年6月26日上午10:00发布，请考生做好准备。");
        updateInput.put("scheduledTime", "2025-06-25T10:00:00Z");
        updateInput.put("isActive", true);

        JsonNode updateResponse = executeGraphQl(
            "mutation UpdatePreNotification($id: ID!, $input: ExamResultPreNotificationInput!) {" +
                " updateResultPreNotification(id: $id, input: $input) { id title content scheduledTime } }",
            Map.of("id", notificationId, "input", updateInput),
            adminToken
        );

        Assertions.assertEquals("2025年一级建造师成绩查询预告（更新）", updateResponse.path("updateResultPreNotification").path("title").asText());

        JsonNode publishResponse = executeGraphQl(
            "mutation PublishPreNotification($id: ID!) {" +
                " publishResultPreNotification(id: $id) { id isActive publishedAt } }",
            Map.of("id", notificationId),
            adminToken
        );

        Assertions.assertTrue(publishResponse.path("publishResultPreNotification").path("isActive").asBoolean());
        Assertions.assertTrue(publishResponse.path("publishResultPreNotification").path("publishedAt").isTextual());
    }

    @Test
    void resultReleaseSettingManagement() {
        Map<String, Object> settingInput = new HashMap<>();
        settingInput.put("subjectId", subjectId);
        settingInput.put("examYear", 2025);
        settingInput.put("releaseTime", "2025-06-30T00:00:00Z");
        settingInput.put("description", "2025年建筑工程案例分析成绩发布时间");

        JsonNode createSettingResponse = executeGraphQl(
            "mutation CreateReleaseSetting($input: ExamResultReleaseSettingInput!) {" +
                " upsertResultReleaseSetting(input: $input) { id subject { name } examYear releaseTime description } }",
            Map.of("input", settingInput),
            adminToken
        );

        String settingId = createSettingResponse.path("upsertResultReleaseSetting").path("id").asText();
        Assertions.assertEquals("建筑工程案例分析", createSettingResponse.path("upsertResultReleaseSetting").path("subject").path("name").asText());
        Assertions.assertEquals(2025, createSettingResponse.path("upsertResultReleaseSetting").path("examYear").asInt());

        JsonNode settingsResponse = executeGraphQl(
            "query GetReleaseSettings($subjectId: ID!, $examYear: Int!) {" +
                " resultReleaseSettings(subjectId: $subjectId, examYear: $examYear) { id subject { name } examYear releaseTime description } } }",
            Map.of("subjectId", subjectId, "examYear", 2025),
            adminToken
        );

        Assertions.assertTrue(settingsResponse.path("resultReleaseSettings").size() > 0);
        Assertions.assertEquals("建筑工程案例分析", settingsResponse.path("resultReleaseSettings").get(0).path("subject").path("name").asText());

        Map<String, Object> batchInput = new HashMap<>();
        batchInput.put("subjectId", subjectId);
        batchInput.put("examYear", 2025);
        batchInput.put("releaseTime", "2025-07-01T00:00:00Z");
        batchInput.put("description", "批量更新的发布时间");

        JsonNode batchResponse = executeGraphQl(
            "mutation BatchSetRelease($input: ExamResultReleaseBatchInput!) {" +
                " batchSetResultRelease(input: $input) { id releaseTime description } }",
            Map.of("input", batchInput),
            adminToken
        );

        Assertions.assertTrue(batchResponse.path("batchSetResultRelease").size() > 0);
    }

    @Test
    void resultImportManagement() {
        Map<String, Object> importInput = new HashMap<>();
        importInput.put("fileName", "results_2025.csv");
        importInput.put("fileSize", 1048576L);
        importInput.put("totalRecords", 100);
        importInput.put("description", "2025年第一批成绩导入");

        JsonNode createImportResponse = executeGraphQl(
            "mutation CreateImport($input: ExamResultImportInput!) {" +
                " createExamResultImport(input: $input) { id fileName fileSize totalRecords status description } }",
            Map.of("input", importInput),
            adminToken
        );

        String importId = createImportResponse.path("createExamResultImport").path("id").asText();
        Assertions.assertEquals("results_2025.csv", createImportResponse.path("createExamResultImport").path("fileName").asText());
        Assertions.assertEquals(100, createImportResponse.path("createExamResultImport").path("totalRecords").asInt());

        JsonNode importsResponse = executeGraphQl(
            "query GetImports { examResultImports { id fileName fileSize totalRecords status createdAt } }",
            Map.of(),
            adminToken
        );

        Assertions.assertTrue(importsResponse.path("examResultImports").size() > 0);
        Assertions.assertEquals("results_2025.csv", importsResponse.path("examResultImports").get(0).path("fileName").asText());

        JsonNode itemsResponse = executeGraphQl(
            "query GetImportItems($importId: ID!) {" +
                " examResultImportItems(importId: $importId) { id rowNumber status errorMessage processedAt } }",
            Map.of("importId", importId),
            adminToken
        );

        Assertions.assertTrue(itemsResponse.path("examResultImportItems").size() >= 0);
    }

    @Test
    void examCertificateManagement() {
        Map<String, Object> certificateInput = new HashMap<>();
        certificateInput.put("fullName", "赵六");
        certificateInput.put("idCardNumber", "110101199004040044");
        certificateInput.put("examType", "一级建造师");
        certificateInput.put("examYear", 2025);
        certificateInput.put("ticketNumber", "T2025001");

        JsonNode certificateResponse = executeGraphQl(
            "mutation GenerateCertificate($input: ExamCertificateRequestInput!) {" +
                " generateExamCertificate(input: $input) { id fullName certificateNumber fileUrl qrContent } }",
            Map.of("input", certificateInput),
            studentToken
        );

        Assertions.assertEquals("赵六", certificateResponse.path("generateExamCertificate").path("fullName").asText());
        Assertions.assertTrue(certificateResponse.path("generateExamCertificate").path("certificateNumber").isTextual());
        Assertions.assertTrue(certificateResponse.path("generateExamCertificate").path("fileUrl").isTextual());
    }

    private String loginAndGetToken(String identifier, String password) {
        JsonNode node = executeGraphQl(
            "mutation Login($phone:String!, $pwd:String!){"
                + " login(phone:$phone, password:$pwd){ token } }",
            Map.of("phone", identifier, "pwd", password),
            null
        );
        return node.path("login").path("token").asText();
    }

    private JsonNode executeGraphQl(String document, Map<String, Object> variables, String token) {
        return executeGraphQlRoot(document, variables, token).path("data");
    }

    private JsonNode executeGraphQlRoot(String document, Map<String, Object> variables, String token) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("query", document);
        payload.put("variables", variables);

        WebTestClient.RequestHeadersSpec<?> request = webTestClient.post()
            .uri("")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(payload);

        if (token != null) {
            request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }

        String body = request.exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse GraphQL response", e);
        }
    }
}