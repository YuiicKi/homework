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
class RegistrationManagementGraphqlIntegrationTest {

    private static final String ADMIN_PHONE = "13899990002";
    private static final String ADMIN_PASSWORD = "Admin@2024";
    private static final String STUDENT_PHONE = "13900001112";
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

    @BeforeEach
    void setupUsers() {
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
    }

    @Test
    void examRegistrationWindowManagement_crudOperations() {
        Map<String, Object> subjectInput = new HashMap<>();
        subjectInput.put("code", "SUB003");
        subjectInput.put("name", "建设工程经济");
        subjectInput.put("durationMinutes", 120);
        subjectInput.put("questionCount", 100);

        JsonNode subjectResponse = executeGraphQl(
            "mutation CreateSubject($input: ExamSubjectInput!) {" +
                " createExamSubject(input: $input) { id code name } }",
            Map.of("input", subjectInput),
            adminToken
        );

        String subjectId = subjectResponse.path("createExamSubject").path("id").asText();

        Map<String, Object> windowInput = new HashMap<>();
        windowInput.put("subjectId", subjectId);
        windowInput.put("registrationStartTime", "2025-01-01T00:00:00Z");
        windowInput.put("registrationEndTime", "2025-01-31T23:59:59Z");
        windowInput.put("description", "第一季度报名");

        JsonNode createWindowResponse = executeGraphQl(
            "mutation CreateWindow($input: ExamRegistrationWindowInput!) {" +
                " upsertExamRegistrationWindow(input: $input) { id subject { name } registrationStartTime registrationEndTime description } }",
            Map.of("input", windowInput),
            adminToken
        );

        String windowId = createWindowResponse.path("upsertExamRegistrationWindow").path("id").asText();
        Assertions.assertEquals("建设工程经济", createWindowResponse.path("upsertExamRegistrationWindow").path("subject").path("name").asText());

        JsonNode windowsResponse = executeGraphQl(
            "query GetWindows($subjectId: ID!) { examRegistrationWindows(subjectId: $subjectId) { id subject { name } registrationStartTime registrationEndTime } }",
            Map.of("subjectId", subjectId),
            adminToken
        );

        Assertions.assertTrue(windowsResponse.path("examRegistrationWindows").size() > 0);
        Assertions.assertEquals("建设工程经济", windowsResponse.path("examRegistrationWindows").get(0).path("subject").path("name").asText());

        Map<String, Object> updateInput = new HashMap<>();
        updateInput.put("subjectId", subjectId);
        updateInput.put("registrationStartTime", "2025-01-15T00:00:00Z");
        updateInput.put("registrationEndTime", "2025-02-15T23:59:59Z");
        updateInput.put("description", "延期报名窗口");

        JsonNode updateResponse = executeGraphQl(
            "mutation UpdateWindow($input: ExamRegistrationWindowInput!) {" +
                " upsertExamRegistrationWindow(id: \"" + windowId + "\", input: $input) { id description } }",
            Map.of("input", updateInput),
            adminToken
        );

        Assertions.assertEquals("延期报名窗口", updateResponse.path("upsertExamRegistrationWindow").path("description").asText());
    }

    @Test
    void studentRegistration_flow() {
        Map<String, Object> subjectInput = new HashMap<>();
        subjectInput.put("code", "SUB004");
        subjectInput.put("name", "建设工程项目管理");
        subjectInput.put("durationMinutes", 150);
        subjectInput.put("questionCount", 120);

        JsonNode subjectResponse = executeGraphQl(
            "mutation CreateSubject($input: ExamSubjectInput!) {" +
                " createExamSubject(input: $input) { id code name } }",
            Map.of("input", subjectInput),
            adminToken
        );

        String subjectId = subjectResponse.path("createExamSubject").path("id").asText();

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
        registrationInput.put("fullName", "张三");
        registrationInput.put("idCardNumber", "110101199001010011");
        registrationInput.put("gender", "男");
        registrationInput.put("birthDate", "1990-01-01");
        registrationInput.put("email", "zhangsan@example.com");
        registrationInput.put("address", "北京市朝阳区");
        registrationInput.put("phone", "13800138000");
        registrationInput.put("education", "本科");
        registrationInput.put("workUnit", "某建筑公司");
        registrationInput.put("workExperience", "5年");

        JsonNode registrationResponse = executeGraphQl(
            "mutation SubmitRegistration($input: RegistrationInfoInput!) {" +
                " upsertRegistrationInfo(input: $input) { id fullName idCardNumber subject { name } status } }",
            Map.of("input", registrationInput),
            studentToken
        );

        String registrationId = registrationResponse.path("upsertRegistrationInfo").path("id").asText();
        Assertions.assertEquals("张三", registrationResponse.path("upsertRegistrationInfo").path("fullName").asText());
        Assertions.assertEquals("建设工程项目管理", registrationResponse.path("upsertRegistrationInfo").path("subject").path("name").asText());

        JsonNode availableExamsResponse = executeGraphQl(
            "query AvailableExams { availableExams { registrationId subjectName subjectCode status actionLabel } }",
            Map.of(),
            studentToken
        );

        Assertions.assertTrue(availableExamsResponse.path("availableExams").size() > 0);
    }

    @Test
    void registrationAudit_approvalAndRejection() {
        Map<String, Object> subjectInput = new HashMap<>();
        subjectInput.put("code", "SUB005");
        subjectInput.put("name", "专业工程管理与实务");
        subjectInput.put("durationMinutes", 180);
        subjectInput.put("questionCount", 150);

        JsonNode subjectResponse = executeGraphQl(
            "mutation CreateSubject($input: ExamSubjectInput!) {" +
                " createExamSubject(input: $input) { id code name } }",
            Map.of("input", subjectInput),
            adminToken
        );

        String subjectId = subjectResponse.path("createExamSubject").path("id").asText();

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
        registrationInput.put("fullName", "李四");
        registrationInput.put("idCardNumber", "110101199002020022");
        registrationInput.put("gender", "女");
        registrationInput.put("birthDate", "1990-02-02");
        registrationInput.put("email", "lisi@example.com");
        registrationInput.put("address", "上海市浦东新区");
        registrationInput.put("phone", "13900139000");
        registrationInput.put("education", "硕士");
        registrationInput.put("workUnit", "某设计院");
        registrationInput.put("workExperience", "3年");

        JsonNode registrationResponse = executeGraphQl(
            "mutation SubmitRegistration($input: RegistrationInfoInput!) {" +
                " upsertRegistrationInfo(input: $input) { id fullName status } }",
            Map.of("input", registrationInput),
            studentToken
        );

        String registrationId = registrationResponse.path("upsertRegistrationInfo").path("id").asText();

        JsonNode auditDetailResponse = executeGraphQl(
            "query AuditDetail($id: ID!) { registrationAuditDetail(id: $id) { id fullName idCardNumber status } }",
            Map.of("id", registrationId),
            adminToken
        );

        Assertions.assertEquals("李四", auditDetailResponse.path("registrationAuditDetail").path("fullName").asText());

        JsonNode approveResponse = executeGraphQl(
            "mutation ApproveRegistration($registrationInfoId: ID!) {" +
                " approveRegistration(registrationInfoId: $registrationInfoId) { id status } }",
            Map.of("registrationInfoId", registrationId),
            adminToken
        );

        Assertions.assertTrue(approveResponse.path("approveRegistration").path("status").asText().contains("APPROVED"));

        Map<String, Object> rejectInput = new HashMap<>();
        rejectInput.put("registrationInfoId", registrationId);
        rejectInput.put("reason", "材料不完整");

        JsonNode rejectResponse = executeGraphQl(
            "mutation RejectRegistration($input: RegistrationRejectInput!) {" +
                " rejectRegistration(input: $input) { id status } }",
            Map.of("input", rejectInput),
            adminToken
        );

        Assertions.assertTrue(rejectResponse.path("rejectRegistration").path("status").asText().contains("REJECTED"));
    }

    @Test
    void registrationMaterialTemplateManagement() {
        Map<String, Object> templateInput = new HashMap<>();
        templateInput.put("name", "身份证复印件");
        templateInput.put("description", "身份证正反面复印件");
        templateInput.put("isRequired", true);
        templateInput.put("fileType", "PDF");
        templateInput.put("maxSize", 5242880L);

        JsonNode createTemplateResponse = executeGraphQl(
            "mutation CreateTemplate($input: RegistrationMaterialTemplateInput!) {" +
                " createRegistrationMaterialTemplate(input: $input) { id name description isRequired fileType maxSize } }",
            Map.of("input", templateInput),
            adminToken
        );

        String templateId = createTemplateResponse.path("createRegistrationMaterialTemplate").path("id").asText();
        Assertions.assertEquals("身份证复印件", createTemplateResponse.path("createRegistrationMaterialTemplate").path("name").asText());
        Assertions.assertTrue(createTemplateResponse.path("createRegistrationMaterialTemplate").path("isRequired").asBoolean());

        JsonNode templatesResponse = executeGraphQl(
            "query GetTemplates { registrationMaterialTemplates { id name description isRequired fileType } }",
            Map.of(),
            adminToken
        );

        Assertions.assertTrue(templatesResponse.path("registrationMaterialTemplates").size() > 0);
        Assertions.assertEquals("身份证复印件", templatesResponse.path("registrationMaterialTemplates").get(0).path("name").asText());

        Map<String, Object> updateInput = new HashMap<>();
        updateInput.put("name", "身份证复印件（更新）");
        updateInput.put("description", "身份证正反面复印件，需清晰");
        updateInput.put("isRequired", true);
        updateInput.put("fileType", "PDF,JPG");
        updateInput.put("maxSize", 10485760L);

        JsonNode updateResponse = executeGraphQl(
            "mutation UpdateTemplate($id: ID!, $input: RegistrationMaterialTemplateInput!) {" +
                " updateRegistrationMaterialTemplate(id: $id, input: $input) { id name description fileType maxSize } }",
            Map.of("id", templateId, "input", updateInput),
            adminToken
        );

        Assertions.assertEquals("身份证复印件（更新）", updateResponse.path("updateRegistrationMaterialTemplate").path("name").asText());
        Assertions.assertEquals("PDF,JPG", updateResponse.path("updateRegistrationMaterialTemplate").path("fileType").asText());
    }

    @Test
    void registrationStatusNotification() {
        // 1. 创建考试科目和报名窗口
        Map<String, Object> subjectInput = new HashMap<>();
        subjectInput.put("code", "SUB006");
        subjectInput.put("name", "系统架构设计师");
        subjectInput.put("durationMinutes", 180);
        subjectInput.put("questionCount", 120);

        JsonNode subjectResponse = executeGraphQl(
            "mutation CreateSubject($input: ExamSubjectInput!) {" +
                " createExamSubject(input: $input) { id code name } }",
            Map.of("input", subjectInput),
            adminToken
        );

        String subjectId = subjectResponse.path("createExamSubject").path("id").asText();

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

        // 2. 学生提交报名
        Map<String, Object> registrationInput = new HashMap<>();
        registrationInput.put("subjectId", subjectId);
        registrationInput.put("fullName", "王五");
        registrationInput.put("idCardNumber", "110101199003030033");
        registrationInput.put("gender", "男");
        registrationInput.put("birthDate", "1990-03-03");
        registrationInput.put("email", "wangwu@example.com");
        registrationInput.put("address", "深圳市南山区");
        registrationInput.put("phone", "13500135000");
        registrationInput.put("education", "本科");
        registrationInput.put("workUnit", "某科技公司");
        registrationInput.put("workExperience", "4年");

        JsonNode registrationResponse = executeGraphQl(
            "mutation SubmitRegistration($input: RegistrationInfoInput!) {" +
                " upsertRegistrationInfo(input: $input) { id fullName status } }",
            Map.of("input", registrationInput),
            studentToken
        );

        String registrationId = registrationResponse.path("upsertRegistrationInfo").path("id").asText();
        Assertions.assertFalse(registrationId.isEmpty(), "报名提交失败");

        // 3. 管理员审批通过
        JsonNode approveResponse = executeGraphQl(
            "mutation ApproveRegistration($registrationInfoId: ID!) {" +
                " approveRegistration(registrationInfoId: $registrationInfoId) { id status } }",
            Map.of("registrationInfoId", registrationId),
            adminToken
        );

        Assertions.assertTrue(approveResponse.path("approveRegistration").path("status").asText().contains("APPROVED"),
            "报名审批应通过");

        // 4. 查询考生通知列表，验证收到"报名成功"通知
        JsonNode approvalNotificationsResponse = executeGraphQl(
            "query GetUserNotifications { userNotifications { id title content type isRead createdAt } }",
            Map.of(),
            studentToken
        );

        Assertions.assertTrue(approvalNotificationsResponse.path("userNotifications").size() >= 1,
            "报名审批通过后应生成通知");

        boolean hasApprovalNotification = false;
        for (JsonNode notification : approvalNotificationsResponse.path("userNotifications")) {
            String title = notification.path("title").asText();
            String content = notification.path("content").asText();
            if ((title.contains("报名") && title.contains("成功")) ||
                (title.contains("报名") && title.contains("通过")) ||
                (content.contains("报名") && content.contains("成功"))) {
                hasApprovalNotification = true;
                Assertions.assertTrue(content.contains("系统架构设计师") || content.contains("SUB006"),
                    "通知内容应包含科目名称或代码");
                break;
            }
        }
        Assertions.assertTrue(hasApprovalNotification, "应收到报名成功的通知");

        // 5. 重新提交另一个报名用于测试拒绝场景
        Map<String, Object> subjectInput2 = new HashMap<>();
        subjectInput2.put("code", "SUB007");
        subjectInput2.put("name", "网络规划设计师");
        subjectInput2.put("durationMinutes", 180);
        subjectInput2.put("questionCount", 100);

        JsonNode subjectResponse2 = executeGraphQl(
            "mutation CreateSubject($input: ExamSubjectInput!) {" +
                " createExamSubject(input: $input) { id code name } }",
            Map.of("input", subjectInput2),
            adminToken
        );

        String subjectId2 = subjectResponse2.path("createExamSubject").path("id").asText();

        Map<String, Object> windowInput2 = new HashMap<>();
        windowInput2.put("subjectId", subjectId2);
        windowInput2.put("registrationStartTime", "2025-01-01T00:00:00Z");
        windowInput2.put("registrationEndTime", "2025-12-31T23:59:59Z");

        executeGraphQl(
            "mutation CreateWindow($input: ExamRegistrationWindowInput!) {" +
                " upsertExamRegistrationWindow(input: $input) { id } }",
            Map.of("input", windowInput2),
            adminToken
        );

        Map<String, Object> registrationInput2 = new HashMap<>();
        registrationInput2.put("subjectId", subjectId2);
        registrationInput2.put("fullName", "王五");
        registrationInput2.put("idCardNumber", "110101199003030033");
        registrationInput2.put("gender", "男");
        registrationInput2.put("birthDate", "1990-03-03");
        registrationInput2.put("email", "wangwu@example.com");
        registrationInput2.put("address", "深圳市南山区");
        registrationInput2.put("phone", "13500135000");
        registrationInput2.put("education", "本科");
        registrationInput2.put("workUnit", "某科技公司");
        registrationInput2.put("workExperience", "4年");

        JsonNode registrationResponse2 = executeGraphQl(
            "mutation SubmitRegistration($input: RegistrationInfoInput!) {" +
                " upsertRegistrationInfo(input: $input) { id fullName status } }",
            Map.of("input", registrationInput2),
            studentToken
        );

        String registrationId2 = registrationResponse2.path("upsertRegistrationInfo").path("id").asText();

        // 6. 管理员审批拒绝（含拒绝原因）
        Map<String, Object> rejectInput = new HashMap<>();
        rejectInput.put("registrationInfoId", registrationId2);
        rejectInput.put("reason", "工作年限不足，需满5年工作经验");

        JsonNode rejectResponse = executeGraphQl(
            "mutation RejectRegistration($input: RegistrationRejectInput!) {" +
                " rejectRegistration(input: $input) { id status } }",
            Map.of("input", rejectInput),
            adminToken
        );

        Assertions.assertTrue(rejectResponse.path("rejectRegistration").path("status").asText().contains("REJECTED"),
            "报名审批应被拒绝");

        // 7. 查询考生通知列表，验证收到"报名失败"通知及拒绝原因
        JsonNode rejectionNotificationsResponse = executeGraphQl(
            "query GetUserNotifications { userNotifications { id title content type isRead createdAt } }",
            Map.of(),
            studentToken
        );

        Assertions.assertTrue(rejectionNotificationsResponse.path("userNotifications").size() >= 2,
            "报名拒绝后应生成新通知");

        boolean hasRejectionNotification = false;
        for (JsonNode notification : rejectionNotificationsResponse.path("userNotifications")) {
            String title = notification.path("title").asText();
            String content = notification.path("content").asText();
            if ((title.contains("报名") && (title.contains("失败") || title.contains("拒绝"))) ||
                (content.contains("报名") && (content.contains("失败") || content.contains("拒绝")))) {
                hasRejectionNotification = true;
                Assertions.assertTrue(content.contains("工作年限不足") || content.contains("5年"),
                    "通知内容应包含拒绝原因");
                break;
            }
        }
        Assertions.assertTrue(hasRejectionNotification, "应收到报名失败的通知");

        // 8. 测试未读通知数量查询
        JsonNode unreadCountResponse = executeGraphQl(
            "query GetUnreadNotificationCount { unreadNotificationCount }",
            Map.of(),
            studentToken
        );

        Assertions.assertTrue(unreadCountResponse.path("unreadNotificationCount").asInt() >= 0,
            "应能查询未读通知数量");
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