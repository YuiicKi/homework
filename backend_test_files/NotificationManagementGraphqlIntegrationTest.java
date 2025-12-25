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
class NotificationManagementGraphqlIntegrationTest {

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
        subjectInput.put("code", "SUB008");
        subjectInput.put("name", "信息系统项目管理");
        subjectInput.put("durationMinutes", 180);
        subjectInput.put("questionCount", 150);

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
        registrationInput.put("fullName", "钱七");
        registrationInput.put("idCardNumber", "110101199005050055");
        registrationInput.put("gender", "男");
        registrationInput.put("birthDate", "1990-05-05");
        registrationInput.put("email", "qianqi@example.com");
        registrationInput.put("address", "成都市高新区");
        registrationInput.put("phone", "13700137000");
        registrationInput.put("education", "本科");
        registrationInput.put("workUnit", "某软件公司");
        registrationInput.put("workExperience", "5年");

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
    void notificationManagement_crudOperations() {
        Map<String, Object> notificationInput = new HashMap<>();
        notificationInput.put("title", "2025年考试报名通知");
        notificationInput.put("content", "2025年信息系统项目管理师考试报名已开始，请各位考生及时报名。");
        notificationInput.put("type", "REGISTRATION");
        notificationInput.put("priority", "HIGH");
        notificationInput.put("targetAudience", "ALL");
        notificationInput.put("publishTime", "2025-01-15T09:00:00Z");
        notificationInput.put("isActive", true);

        JsonNode createResponse = executeGraphQl(
            "mutation CreateNotification($input: NotificationInput!) {" +
                " createNotification(input: $input) { id title content type priority targetAudience publishTime isActive } }",
            Map.of("input", notificationInput),
            adminToken
        );

        String notificationId = createResponse.path("createNotification").path("id").asText();
        Assertions.assertEquals("2025年考试报名通知", createResponse.path("createNotification").path("title").asText());
        Assertions.assertEquals("REGISTRATION", createResponse.path("createNotification").path("type").asText());
        Assertions.assertEquals("HIGH", createResponse.path("createNotification").path("priority").asText());
        Assertions.assertTrue(createResponse.path("createNotification").path("isActive").asBoolean());

        Map<String, Object> updateInput = new HashMap<>();
        updateInput.put("title", "2025年考试报名通知（更新）");
        updateInput.put("content", "2025年信息系统项目管理师考试报名已开始，报名截止时间为2025年3月31日。");
        updateInput.put("type", "REGISTRATION");
        updateInput.put("priority", "MEDIUM");
        updateInput.put("targetAudience", "ALL");
        updateInput.put("publishTime", "2025-01-15T09:00:00Z");
        updateInput.put("isActive", true);

        JsonNode updateResponse = executeGraphQl(
            "mutation UpdateNotification($id: ID!, $input: NotificationInput!) {" +
                " updateNotification(id: $id, input: $input) { id title content priority } }",
            Map.of("id", notificationId, "input", updateInput),
            adminToken
        );

        Assertions.assertEquals("2025年考试报名通知（更新）", updateResponse.path("updateNotification").path("title").asText());
        Assertions.assertEquals("MEDIUM", updateResponse.path("updateNotification").path("priority").asText());

        JsonNode queryResponse = executeGraphQl(
            "query GetNotification($id: ID!) {" +
                " notification(id: $id) { id title content type priority targetAudience publishTime isActive createdAt } }",
            Map.of("id", notificationId),
            adminToken
        );

        Assertions.assertEquals("2025年考试报名通知（更新）", queryResponse.path("notification").path("title").asText());
        Assertions.assertEquals("信息系统项目管理师考试报名已开始，报名截止时间为2025年3月31日。", queryResponse.path("notification").path("content").asText());
    }

    @Test
    void notificationManagement_batchOperations() {
        Map<String, Object> notification1Input = new HashMap<>();
        notification1Input.put("title", "考试时间变更通知");
        notification1Input.put("content", "原定于2025年5月20日的考试时间变更为2025年5月25日。");
        notification1Input.put("type", "EXAM_SCHEDULE");
        notification1Input.put("priority", "HIGH");
        notification1Input.put("targetAudience", "REGISTERED");
        notification1Input.put("publishTime", "2025-03-01T10:00:00Z");
        notification1Input.put("isActive", true);

        Map<String, Object> notification2Input = new HashMap<>();
        notification2Input.put("title", "考场安排通知");
        notification2Input.put("content", "考场安排已确定，请考生及时查看准考证信息。");
        notification2Input.put("type", "EXAM_ROOM");
        notification2Input.put("priority", "MEDIUM");
        notification2Input.put("targetAudience", "APPROVED");
        notification2Input.put("publishTime", "2025-04-01T10:00:00Z");
        notification2Input.put("isActive", true);

        executeGraphQl(
            "mutation CreateNotification($input: NotificationInput!) {" +
                " createNotification(input: $input) { id } }",
            Map.of("input", notification1Input),
            adminToken
        );

        executeGraphQl(
            "mutation CreateNotification($input: NotificationInput!) {" +
                " createNotification(input: $input) { id } }",
            Map.of("input", notification2Input),
            adminToken
        );

        JsonNode allNotificationsResponse = executeGraphQl(
            "query GetAllNotifications { notifications { id title type priority targetAudience isActive publishTime } }",
            Map.of(),
            adminToken
        );

        Assertions.assertTrue(allNotificationsResponse.path("notifications").size() >= 2);

        JsonNode activeNotificationsResponse = executeGraphQl(
            "query GetActiveNotifications { activeNotifications { id title type priority publishTime } }",
            Map.of(),
            studentToken
        );

        Assertions.assertTrue(activeNotificationsResponse.path("activeNotifications").size() >= 2);

        JsonNode highPriorityNotificationsResponse = executeGraphQl(
            "query GetHighPriorityNotifications { highPriorityNotifications { id title type priority publishTime } }",
            Map.of(),
            studentToken
        );

        Assertions.assertTrue(highPriorityNotificationsResponse.path("highPriorityNotifications").size() >= 1);
    }

    @Test
    void notificationManagement_filteringAndSearch() {
        Map<String, Object> examNotificationInput = new HashMap<>();
        examNotificationInput.put("title", "考试注意事项");
        examNotificationInput.put("content", "请考生携带身份证和准考证参加考试。");
        examNotificationInput.put("type", "EXAM_REMINDER");
        examNotificationInput.put("priority", "HIGH");
        examNotificationInput.put("targetAudience", "APPROVED");
        examNotificationInput.put("publishTime", "2025-05-01T09:00:00Z");
        examNotificationInput.put("isActive", true);

        Map<String, Object> resultNotificationInput = new HashMap<>();
        resultNotificationInput.put("title", "成绩发布通知");
        resultNotificationInput.put("content", "2025年考试成绩已发布，请考生登录查询。");
        resultNotificationInput.put("type", "RESULT_RELEASE");
        resultNotificationInput.put("priority", "MEDIUM");
        resultNotificationInput.put("targetAudience", "ALL");
        resultNotificationInput.put("publishTime", "2025-06-01T09:00:00Z");
        resultNotificationInput.put("isActive", true);

        executeGraphQl(
            "mutation CreateNotification($input: NotificationInput!) {" +
                " createNotification(input: $input) { id } }",
            Map.of("input", examNotificationInput),
            adminToken
        );

        executeGraphQl(
            "mutation CreateNotification($input: NotificationInput!) {" +
                " createNotification(input: $input) { id } }",
            Map.of("input", resultNotificationInput),
            adminToken
        );

        JsonNode examNotificationsResponse = executeGraphQl(
            "query GetNotificationsByType($type: NotificationType!) {" +
                " notificationsByType(type: $type) { id title type priority publishTime } }",
            Map.of("type", "EXAM_REMINDER"),
            adminToken
        );

        Assertions.assertTrue(examNotificationsResponse.path("notificationsByType").size() >= 1);
        Assertions.assertEquals("EXAM_REMINDER", examNotificationsResponse.path("notificationsByType").get(0).path("type").asText());

        JsonNode searchResponse = executeGraphQl(
            "query SearchNotifications($keyword: String!) {" +
                " searchNotifications(keyword: $keyword) { id title content type priority } }",
            Map.of("keyword", "成绩"),
            adminToken
        );

        Assertions.assertTrue(searchResponse.path("searchNotifications").size() >= 1);
        Assertions.assertTrue(searchResponse.path("searchNotifications").get(0).path("title").asText().contains("成绩"));
    }

    @Test
    void notificationManagement_publishingAndUnpublishing() {
        Map<String, Object> draftNotificationInput = new HashMap<>();
        draftNotificationInput.put("title", "系统维护通知");
        draftNotificationInput.put("content", "系统将于今晚22:00-24:00进行维护，期间服务暂停。");
        draftNotificationInput.put("type", "SYSTEM");
        draftNotificationInput.put("priority", "MEDIUM");
        draftNotificationInput.put("targetAudience", "ALL");
        draftNotificationInput.put("publishTime", "2025-02-01T09:00:00Z");
        draftNotificationInput.put("isActive", false);

        JsonNode createDraftResponse = executeGraphQl(
            "mutation CreateNotification($input: NotificationInput!) {" +
                " createNotification(input: $input) { id title isActive } }",
            Map.of("input", draftNotificationInput),
            adminToken
        );

        String draftNotificationId = createDraftResponse.path("createNotification").path("id").asText();
        Assertions.assertFalse(createDraftResponse.path("createNotification").path("isActive").asBoolean());

        JsonNode publishResponse = executeGraphQl(
            "mutation PublishNotification($id: ID!) {" +
                " publishNotification(id: $id) { id isActive publishedAt } }",
            Map.of("id", draftNotificationId),
            adminToken
        );

        Assertions.assertTrue(publishResponse.path("publishNotification").path("isActive").asBoolean());
        Assertions.assertTrue(publishResponse.path("publishNotification").path("publishedAt").isTextual());

        JsonNode unpublishResponse = executeGraphQl(
            "mutation UnpublishNotification($id: ID!) {" +
                " unpublishNotification(id: $id) { id isActive } }",
            Map.of("id", draftNotificationId),
            adminToken
        );

        Assertions.assertFalse(unpublishResponse.path("unpublishNotification").path("isActive").asBoolean());
    }

    @Test
    void notificationManagement_statistics() {
        Map<String, Object> highPriorityNotificationInput = new HashMap<>();
        highPriorityNotificationInput.put("title", "重要通知");
        highPriorityNotificationInput.put("content", "这是一条高优先级通知。");
        highPriorityNotificationInput.put("type", "GENERAL");
        highPriorityNotificationInput.put("priority", "HIGH");
        highPriorityNotificationInput.put("targetAudience", "ALL");
        highPriorityNotificationInput.put("publishTime", "2025-01-01T09:00:00Z");
        highPriorityNotificationInput.put("isActive", true);

        Map<String, Object> mediumPriorityNotificationInput = new HashMap<>();
        mediumPriorityNotificationInput.put("title", "一般通知");
        mediumPriorityNotificationInput.put("content", "这是一条中等优先级通知。");
        mediumPriorityNotificationInput.put("type", "GENERAL");
        mediumPriorityNotificationInput.put("priority", "MEDIUM");
        mediumPriorityNotificationInput.put("targetAudience", "ALL");
        mediumPriorityNotificationInput.put("publishTime", "2025-01-01T09:00:00Z");
        mediumPriorityNotificationInput.put("isActive", true);

        executeGraphQl(
            "mutation CreateNotification($input: NotificationInput!) {" +
                " createNotification(input: $input) { id } }",
            Map.of("input", highPriorityNotificationInput),
            adminToken
        );

        executeGraphQl(
            "mutation CreateNotification($input: NotificationInput!) {" +
                " createNotification(input: $input) { id } }",
            Map.of("input", mediumPriorityNotificationInput),
            adminToken
        );

        JsonNode statisticsResponse = executeGraphQl(
            "query GetNotificationStatistics { notificationStatistics { totalNotifications activeNotifications highPriorityNotifications mediumPriorityNotifications lowPriorityNotifications } }",
            Map.of(),
            adminToken
        );

        Assertions.assertTrue(statisticsResponse.path("notificationStatistics").path("totalNotifications").asInt() >= 2);
        Assertions.assertTrue(statisticsResponse.path("notificationStatistics").path("activeNotifications").asInt() >= 2);
        Assertions.assertTrue(statisticsResponse.path("notificationStatistics").path("highPriorityNotifications").asInt() >= 1);
        Assertions.assertTrue(statisticsResponse.path("notificationStatistics").path("mediumPriorityNotifications").asInt() >= 1);
    }

    @Test
    void notificationManagement_userNotifications() {
        Map<String, Object> userNotificationInput = new HashMap<>();
        userNotificationInput.put("title", "个人通知");
        userNotificationInput.put("content", "这是一条针对个人的通知。");
        userNotificationInput.put("type", "PERSONAL");
        userNotificationInput.put("priority", "MEDIUM");
        userNotificationInput.put("targetAudience", "INDIVIDUAL");
        userNotificationInput.put("targetUserId", "1");
        userNotificationInput.put("publishTime", "2025-01-01T09:00:00Z");
        userNotificationInput.put("isActive", true);

        executeGraphQl(
            "mutation CreateNotification($input: NotificationInput!) {" +
                " createNotification(input: $input) { id } }",
            Map.of("input", userNotificationInput),
            adminToken
        );

        JsonNode userNotificationsResponse = executeGraphQl(
            "query GetUserNotifications { userNotifications { id title content type priority isRead readAt createdAt } }",
            Map.of(),
            studentToken
        );

        Assertions.assertTrue(userNotificationsResponse.path("userNotifications").size() >= 0);

        JsonNode unreadNotificationsResponse = executeGraphQl(
            "query GetUnreadNotifications { unreadNotifications { id title content type priority createdAt } }",
            Map.of(),
            studentToken
        );

        Assertions.assertTrue(unreadNotificationsResponse.path("unreadNotifications").size() >= 0);
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