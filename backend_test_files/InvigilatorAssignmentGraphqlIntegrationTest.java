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
class InvigilatorAssignmentGraphqlIntegrationTest {

    private static final String ADMIN_PHONE = "13899990004";
    private static final String ADMIN_PASSWORD = "Admin@2024";
    private static final String INVIGILATOR_PHONE = "13900001115";
    private static final String INVIGILATOR_PASSWORD = "Invigilator@123";

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
    private String invigilatorToken;
    private String centerId;
    private String roomId;
    private String subjectId;
    private String sessionId;
    private String scheduleId;
    private String invigilatorId;

    @BeforeEach
    void setupTestData() {
        userRepository.deleteAll();
        RoleEntity adminRole = roleRepository.findByName("admin").orElseThrow();
        RoleEntity invigilatorRole = roleRepository.findByName("invigilator").orElseThrow();

        UserEntity admin = new UserEntity();
        admin.setPhone(ADMIN_PHONE);
        admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setIsActive(true);
        admin.setRoles(new HashSet<>());
        admin.getRoles().add(adminRole);
        userRepository.save(admin);

        UserEntity invigilator = new UserEntity();
        invigilator.setPhone(INVIGILATOR_PHONE);
        invigilator.setPasswordHash(passwordEncoder.encode(INVIGILATOR_PASSWORD));
        invigilator.setIsActive(true);
        invigilator.setRoles(new HashSet<>());
        invigilator.getRoles().add(invigilatorRole);
        userRepository.save(invigilator);

        this.webTestClient = baseWebTestClient.mutate()
            .baseUrl("http://localhost:" + port + "/graphql")
            .build();
        
        this.adminToken = loginAndGetToken(ADMIN_PHONE, ADMIN_PASSWORD);
        this.invigilatorToken = loginAndGetToken(INVIGILATOR_PHONE, INVIGILATOR_PASSWORD);

        setupExamData();
        setupInvigilatorData();
    }

    private void setupExamData() {
        Map<String, Object> centerInput = new HashMap<>();
        centerInput.put("name", "北京考试中心");
        centerInput.put("address", "北京市朝阳区");
        centerInput.put("capacity", 500);
        centerInput.put("contactPhone", "010-12345678");
        centerInput.put("contactPerson", "张主任");

        JsonNode centerResponse = executeGraphQl(
            "mutation CreateCenter($input: ExamCenterInput!) {" +
                " createExamCenter(input: $input) { id name address capacity } }",
            Map.of("input", centerInput),
            adminToken
        );

        this.centerId = centerResponse.path("createExamCenter").path("id").asText();

        Map<String, Object> roomInput = new HashMap<>();
        roomInput.put("centerId", centerId);
        roomInput.put("roomNumber", "D401");
        roomInput.put("building", "D栋");
        roomInput.put("floor", 4);
        roomInput.put("capacity", 40);
        roomInput.put("hasAirConditioning", true);
        roomInput.put("hasProjector", true);

        JsonNode roomResponse = executeGraphQl(
            "mutation CreateRoom($input: ExamRoomInput!) {" +
                " createExamRoom(input: $input) { id roomNumber building floor capacity } }",
            Map.of("input", roomInput),
            adminToken
        );

        this.roomId = roomResponse.path("createExamRoom").path("id").asText();

        Map<String, Object> subjectInput = new HashMap<>();
        subjectInput.put("code", "SUB009");
        subjectInput.put("name", "网络安全工程师");
        subjectInput.put("durationMinutes", 180);
        subjectInput.put("questionCount", 120);

        JsonNode subjectResponse = executeGraphQl(
            "mutation CreateSubject($input: ExamSubjectInput!) {" +
                " createExamSubject(input: $input) { id code name } }",
            Map.of("input", subjectInput),
            adminToken
        );

        this.subjectId = subjectResponse.path("createExamSubject").path("id").asText();

        Map<String, Object> sessionInput = new HashMap<>();
        sessionInput.put("name", "上午场");
        sessionInput.put("startTime", "09:00:00");
        sessionInput.put("endTime", "12:00:00");

        JsonNode sessionResponse = executeGraphQl(
            "mutation CreateSession($input: ExamSessionInput!) {" +
                " createExamSession(input: $input) { id name startTime endTime } }",
            Map.of("input", sessionInput),
            adminToken
        );

        this.sessionId = sessionResponse.path("createExamSession").path("id").asText();

        Map<String, Object> scheduleInput = new HashMap<>();
        scheduleInput.put("roomId", roomId);
        scheduleInput.put("subjectId", subjectId);
        scheduleInput.put("sessionId", sessionId);
        scheduleInput.put("examDate", "2025-06-15");
        scheduleInput.put("note", "网络安全工程师考试");

        JsonNode scheduleResponse = executeGraphQl(
            "mutation CreateSchedule($input: ExamScheduleInput!) {" +
                " createExamSchedule(input: $input) { id examRoom { roomNumber } examSubject { name } examSession { name } examDate } }",
            Map.of("input", scheduleInput),
            adminToken
        );

        this.scheduleId = scheduleResponse.path("createExamSchedule").path("id").asText();
    }

    private void setupInvigilatorData() {
        Map<String, Object> invigilatorInput = new HashMap<>();
        invigilatorInput.put("name", "李监考");
        invigilatorInput.put("phone", "13800138000");
        invigilatorInput.put("email", "li@example.com");
        invigilatorInput.put("workUnit", "北京某大学");
        invigilatorInput.put("qualification", "教师资格证");
        invigilatorInput.put("experience", "5年监考经验");
        invigilatorInput.put("specialties", new String[]{"计算机科学", "网络安全"});
        invigilatorInput.put("maxAssignmentsPerDay", 2);
        invigilatorInput.put("isAvailable", true);

        JsonNode invigilatorResponse = executeGraphQl(
            "mutation CreateInvigilator($input: InvigilatorInput!) {" +
                " createInvigilator(input: $input) { id name phone email workUnit qualification experience specialties maxAssignmentsPerDay isAvailable } }",
            Map.of("input", invigilatorInput),
            adminToken
        );

        this.invigilatorId = invigilatorResponse.path("createInvigilator").path("id").asText();
    }

    @Test
    void invigilatorManagement_crudOperations() {
        Map<String, Object> invigilatorInput = new HashMap<>();
        invigilatorInput.put("name", "王监考");
        invigilatorInput.put("phone", "13900139000");
        invigilatorInput.put("email", "wang@example.com");
        invigilatorInput.put("workUnit", "上海某学院");
        invigilatorInput.put("qualification", "高级教师资格证");
        invigilatorInput.put("experience", "8年监考经验");
        invigilatorInput.put("specialties", new String[]{"软件工程", "数据库"});
        invigilatorInput.put("maxAssignmentsPerDay", 3);
        invigilatorInput.put("isAvailable", true);

        JsonNode createResponse = executeGraphQl(
            "mutation CreateInvigilator($input: InvigilatorInput!) {" +
                " createInvigilator(input: $input) { id name phone email workUnit qualification experience specialties maxAssignmentsPerDay isAvailable } }",
            Map.of("input", invigilatorInput),
            adminToken
        );

        String newInvigilatorId = createResponse.path("createInvigilator").path("id").asText();
        Assertions.assertEquals("王监考", createResponse.path("createInvigilator").path("name").asText());
        Assertions.assertEquals("13900139000", createResponse.path("createInvigilator").path("phone").asText());
        Assertions.assertEquals("上海某学院", createResponse.path("createInvigilator").path("workUnit").asText());
        Assertions.assertEquals(3, createResponse.path("createInvigilator").path("maxAssignmentsPerDay").asInt());
        Assertions.assertTrue(createResponse.path("createInvigilator").path("isAvailable").asBoolean());

        Map<String, Object> updateInput = new HashMap<>();
        updateInput.put("name", "王监考（更新）");
        updateInput.put("phone", "13900139000");
        updateInput.put("email", "wang@example.com");
        updateInput.put("workUnit", "上海某学院");
        updateInput.put("qualification", "高级教师资格证");
        updateInput.put("experience", "9年监考经验");
        updateInput.put("specialties", new String[]{"软件工程", "数据库", "人工智能"});
        updateInput.put("maxAssignmentsPerDay", 2);
        updateInput.put("isAvailable", false);

        JsonNode updateResponse = executeGraphQl(
            "mutation UpdateInvigilator($id: ID!, $input: InvigilatorInput!) {" +
                " updateInvigilator(id: $id, input: $input) { id name experience maxAssignmentsPerDay isAvailable } }",
            Map.of("id", newInvigilatorId, "input", updateInput),
            adminToken
        );

        Assertions.assertEquals("王监考（更新）", updateResponse.path("updateInvigilator").path("name").asText());
        Assertions.assertEquals("9年监考经验", updateResponse.path("updateInvigilator").path("experience").asText());
        Assertions.assertEquals(2, updateResponse.path("updateInvigilator").path("maxAssignmentsPerDay").asInt());
        Assertions.assertFalse(updateResponse.path("updateInvigilator").path("isAvailable").asBoolean());

        JsonNode queryResponse = executeGraphQl(
            "query GetInvigilator($id: ID!) {" +
                " invigilator(id: $id) { id name phone email workUnit qualification experience specialties maxAssignmentsPerDay isAvailable createdAt } }",
            Map.of("id", newInvigilatorId),
            adminToken
        );

        Assertions.assertEquals("王监考（更新）", queryResponse.path("invigilator").path("name").asText());
        Assertions.assertEquals("9年监考经验", queryResponse.path("invigilator").path("experience").asText());
    }

    @Test
    void invigilatorAssignmentManagement() {
        Map<String, Object> assignmentInput = new HashMap<>();
        assignmentInput.put("invigilatorId", invigilatorId);
        assignmentInput.put("scheduleId", scheduleId);
        assignmentInput.put("role", "PRIMARY");
        assignmentInput.put("assignmentDate", "2025-06-15");
        assignmentInput.put("status", "ASSIGNED");
        assignmentInput.put("notes", "主监考");

        JsonNode createAssignmentResponse = executeGraphQl(
            "mutation CreateAssignment($input: InvigilatorAssignmentInput!) {" +
                " createInvigilatorAssignment(input: $input) { id invigilator { name } examSchedule { examRoom { roomNumber } examSubject { name } } role assignmentDate status notes } }",
            Map.of("input", assignmentInput),
            adminToken
        );

        String assignmentId = createAssignmentResponse.path("createInvigilatorAssignment").path("id").asText();
        Assertions.assertEquals("李监考", createAssignmentResponse.path("createInvigilatorAssignment").path("invigilator").path("name").asText());
        Assertions.assertEquals("D401", createAssignmentResponse.path("createInvigilatorAssignment").path("examSchedule").path("examRoom").path("roomNumber").asText());
        Assertions.assertEquals("网络安全工程师", createAssignmentResponse.path("createInvigilatorAssignment").path("examSchedule").path("examSubject").path("name").asText());
        Assertions.assertEquals("PRIMARY", createAssignmentResponse.path("createInvigilatorAssignment").path("role").asText());
        Assertions.assertEquals("ASSIGNED", createAssignmentResponse.path("createInvigilatorAssignment").path("status").asText());

        Map<String, Object> updateAssignmentInput = new HashMap<>();
        updateAssignmentInput.put("role", "SECONDARY");
        updateAssignmentInput.put("status", "CONFIRMED");
        updateAssignmentInput.put("notes", "副监考");

        JsonNode updateAssignmentResponse = executeGraphQl(
            "mutation UpdateAssignment($id: ID!, $input: InvigilatorAssignmentInput!) {" +
                " updateInvigilatorAssignment(id: $id, input: $input) { id role status notes } }",
            Map.of("id", assignmentId, "input", updateAssignmentInput),
            adminToken
        );

        Assertions.assertEquals("SECONDARY", updateAssignmentResponse.path("updateInvigilatorAssignment").path("role").asText());
        Assertions.assertEquals("CONFIRMED", updateAssignmentResponse.path("updateInvigilatorAssignment").path("status").asText());
        Assertions.assertEquals("副监考", updateAssignmentResponse.path("updateInvigilatorAssignment").path("notes").asText());

        JsonNode assignmentsResponse = executeGraphQl(
            "query GetAssignmentsByInvigilator($invigilatorId: ID!) {" +
                " invigilatorAssignments(invigilatorId: $invigilatorId) { id invigilator { name } examSchedule { examRoom { roomNumber } examSubject { name } examDate } role status } }",
            Map.of("invigilatorId", invigilatorId),
            adminToken
        );

        Assertions.assertTrue(assignmentsResponse.path("invigilatorAssignments").size() >= 1);
        Assertions.assertEquals("李监考", assignmentsResponse.path("invigilatorAssignments").get(0).path("invigilator").path("name").asText());
    }

    @Test
    void invigilatorAvailabilityManagement() {
        Map<String, Object> availabilityInput = new HashMap<>();
        availabilityInput.put("invigilatorId", invigilatorId);
        availabilityInput.put("date", "2025-06-15");
        availabilityInput.put("isAvailable", true);
        availabilityInput.put("maxAssignments", 2);
        availabilityInput.put("notes", "当天有空");

        JsonNode createAvailabilityResponse = executeGraphQl(
            "mutation CreateAvailability($input: InvigilatorAvailabilityInput!) {" +
                " createInvigilatorAvailability(input: $input) { id invigilator { name } date isAvailable maxAssignments notes } }",
            Map.of("input", availabilityInput),
            adminToken
        );

        String availabilityId = createAvailabilityResponse.path("createInvigilatorAvailability").path("id").asText();
        Assertions.assertEquals("李监考", createAvailabilityResponse.path("createInvigilatorAvailability").path("invigilator").path("name").asText());
        Assertions.assertTrue(createAvailabilityResponse.path("createInvigilatorAvailability").path("isAvailable").asBoolean());
        Assertions.assertEquals(2, createAvailabilityResponse.path("createInvigilatorAvailability").path("maxAssignments").asInt());

        Map<String, Object> updateAvailabilityInput = new HashMap<>();
        updateAvailabilityInput.put("isAvailable", false);
        updateAvailabilityInput.put("maxAssignments", 0);
        updateAvailabilityInput.put("notes", "临时有事");

        JsonNode updateAvailabilityResponse = executeGraphQl(
            "mutation UpdateAvailability($id: ID!, $input: InvigilatorAvailabilityInput!) {" +
                " updateInvigilatorAvailability(id: $id, input: $input) { id isAvailable maxAssignments notes } }",
            Map.of("id", availabilityId, "input", updateAvailabilityInput),
            adminToken
        );

        Assertions.assertFalse(updateAvailabilityResponse.path("updateInvigilatorAvailability").path("isAvailable").asBoolean());
        Assertions.assertEquals(0, updateAvailabilityResponse.path("updateInvigilatorAvailability").path("maxAssignments").asInt());
        Assertions.assertEquals("临时有事", updateAvailabilityResponse.path("updateInvigilatorAvailability").path("notes").asText());

        JsonNode availabilityListResponse = executeGraphQl(
            "query GetAvailabilityByInvigilator($invigilatorId: ID!, $startDate: String!, $endDate: String!) {" +
                " invigilatorAvailability(invigilatorId: $invigilatorId, startDate: $startDate, endDate: $endDate) { id date isAvailable maxAssignments notes } }",
            Map.of("invigilatorId", invigilatorId, "startDate", "2025-06-01", "endDate", "2025-06-30"),
            adminToken
        );

        Assertions.assertTrue(availabilityListResponse.path("invigilatorAvailability").size() >= 1);
    }

    @Test
    void invigilatorStatisticsManagement() {
        Map<String, Object> statsInput = new HashMap<>();
        statsInput.put("invigilatorId", invigilatorId);
        statsInput.put("year", 2025);
        statsInput.put("month", 6);
        statsInput.put("totalAssignments", 8);
        statsInput.put("completedAssignments", 7);
        statsInput.put("cancelledAssignments", 1);
        statsInput.put("totalHours", 24);
        statsInput.put("averageRating", 4.8);

        JsonNode createStatsResponse = executeGraphQl(
            "mutation CreateStatistics($input: InvigilatorStatisticsInput!) {" +
                " createInvigilatorStatistics(input: $input) { id invigilator { name } year month totalAssignments completedAssignments cancelledAssignments totalHours averageRating } }",
            Map.of("input", statsInput),
            adminToken
        );

        String statsId = createStatsResponse.path("createInvigilatorStatistics").path("id").asText();
        Assertions.assertEquals("李监考", createStatsResponse.path("createInvigilatorStatistics").path("invigilator").path("name").asText());
        Assertions.assertEquals(2025, createStatsResponse.path("createInvigilatorStatistics").path("year").asInt());
        Assertions.assertEquals(6, createStatsResponse.path("createInvigilatorStatistics").path("month").asInt());
        Assertions.assertEquals(8, createStatsResponse.path("createInvigilatorStatistics").path("totalAssignments").asInt());
        Assertions.assertEquals(7, createStatsResponse.path("createInvigilatorStatistics").path("completedAssignments").asInt());
        Assertions.assertEquals(24.0, createStatsResponse.path("createInvigilatorStatistics").path("totalHours").asDouble());
        Assertions.assertEquals(4.8, createStatsResponse.path("createInvigilatorStatistics").path("averageRating").asDouble());

        JsonNode monthlyStatsResponse = executeGraphQl(
            "query GetMonthlyStatistics($invigilatorId: ID!, $year: Int!, $month: Int!) {" +
                " invigilatorMonthlyStatistics(invigilatorId: $invigilatorId, year: $year, month: $month) { id year month totalAssignments completedAssignments cancelledAssignments totalHours averageRating } }",
            Map.of("invigilatorId", invigilatorId, "year", 2025, "month", 6),
            adminToken
        );

        Assertions.assertTrue(monthlyStatsResponse.path("invigilatorMonthlyStatistics").size() >= 1);
        Assertions.assertEquals(8, monthlyStatsResponse.path("invigilatorMonthlyStatistics").get(0).path("totalAssignments").asInt());

        JsonNode yearlyStatsResponse = executeGraphQl(
            "query GetYearlyStatistics($invigilatorId: ID!, $year: Int!) {" +
                " invigilatorYearlyStatistics(invigilatorId: $invigilatorId, year: $year) { id year totalAssignments completedAssignments cancelledAssignments totalHours averageRating } }",
            Map.of("invigilatorId", invigilatorId, "year", 2025),
            adminToken
        );

        Assertions.assertTrue(yearlyStatsResponse.path("invigilatorYearlyStatistics").size() >= 1);
    }

    @Test
    void invigilatorSearchAndFilter() {
        Map<String, Object> searchResponse = executeGraphQl(
            "query SearchInvigilators($keyword: String!) {" +
                " searchInvigilators(keyword: $keyword) { id name phone email workUnit qualification experience specialties isAvailable } }",
            Map.of("keyword", "李"),
            adminToken
        );

        Assertions.assertTrue(searchResponse.path("searchInvigilators").size() >= 1);
        Assertions.assertTrue(searchResponse.path("searchInvigilators").get(0).path("name").asText().contains("李"));

        Map<String, Object> availableResponse = executeGraphQl(
            "query GetAvailableInvigilators($date: String!) {" +
                " availableInvigilators(date: $date) { id name phone email workUnit qualification experience maxAssignmentsPerDay } }",
            Map.of("date", "2025-06-15"),
            adminToken
        );

        Assertions.assertTrue(availableResponse.path("availableInvigilators").size() >= 1);

        Map<String, Object> bySpecialtyResponse = executeGraphQl(
            "query GetInvigilatorsBySpecialty($specialty: String!) {" +
                " invigilatorsBySpecialty(specialty: $specialty) { id name phone email workUnit qualification experience specialties } }",
            Map.of("specialty", "网络安全"),
            adminToken
        );

        Assertions.assertTrue(bySpecialtyResponse.path("invigilatorsBySpecialty").size() >= 1);
    }

    @Test
    void invigilatorBatchOperations() {
        Map<String, Object> batchAssignmentInput = new HashMap<>();
        batchAssignmentInput.put("scheduleId", scheduleId);
        batchAssignmentInput.put("invigilatorIds", new String[]{invigilatorId});
        batchAssignmentInput.put("assignmentDate", "2025-06-15");
        batchAssignmentInput.put("notes", "批量分配");

        JsonNode batchAssignmentResponse = executeGraphQl(
            "mutation BatchAssignInvigilators($input: BatchInvigilatorAssignmentInput!) {" +
                " batchAssignInvigilators(input: $input) { id invigilator { name } examSchedule { examRoom { roomNumber } } role status } }",
            Map.of("input", batchAssignmentInput),
            adminToken
        );

        Assertions.assertTrue(batchAssignmentResponse.path("batchAssignInvigilators").size() >= 1);

        Map<String, Object> batchAvailabilityInput = new HashMap<>();
        batchAvailabilityInput.put("invigilatorIds", new String[]{invigilatorId});
        batchAvailabilityInput.put("startDate", "2025-06-01");
        batchAvailabilityInput.put("endDate", "2025-06-30");
        batchAvailabilityInput.put("isAvailable", true);
        batchAvailabilityInput.put("maxAssignments", 2);

        JsonNode batchAvailabilityResponse = executeGraphQl(
            "mutation BatchUpdateAvailability($input: BatchInvigilatorAvailabilityInput!) {" +
                " batchUpdateInvigilatorAvailability(input: $input) { id invigilator { name } date isAvailable maxAssignments } }",
            Map.of("input", batchAvailabilityInput),
            adminToken
        );

        Assertions.assertTrue(batchAvailabilityResponse.path("batchUpdateInvigilatorAvailability").size() >= 1);
    }

    @Test
    void invigilatorAssignmentNotification() {
        // 1. 创建监考分配
        Map<String, Object> assignmentInput = new HashMap<>();
        assignmentInput.put("invigilatorId", invigilatorId);
        assignmentInput.put("scheduleId", scheduleId);
        assignmentInput.put("role", "PRIMARY");
        assignmentInput.put("assignmentDate", "2025-06-15");
        assignmentInput.put("status", "ASSIGNED");
        assignmentInput.put("notes", "主监考-通知测试");

        JsonNode createAssignmentResponse = executeGraphQl(
            "mutation CreateAssignment($input: InvigilatorAssignmentInput!) {" +
                " createInvigilatorAssignment(input: $input) { id invigilator { id name } examSchedule { examRoom { roomNumber } examSubject { name } examDate } role status } }",
            Map.of("input", assignmentInput),
            adminToken
        );

        String assignmentId = createAssignmentResponse.path("createInvigilatorAssignment").path("id").asText();
        Assertions.assertFalse(assignmentId.isEmpty(), "监考分配创建失败");

        // 2. 查询监考员收到的通知列表
        JsonNode notificationsResponse = executeGraphQl(
            "query GetInvigilatorNotifications($invigilatorId: ID!) {" +
                " invigilatorNotifications(invigilatorId: $invigilatorId) { id title content type isRead createdAt } }",
            Map.of("invigilatorId", invigilatorId),
            adminToken
        );

        // 3. 验证通知已生成
        Assertions.assertTrue(notificationsResponse.path("invigilatorNotifications").size() >= 1,
            "监考分配后应生成通知");

        // 4. 验证通知内容
        JsonNode notification = notificationsResponse.path("invigilatorNotifications").get(0);
        String title = notification.path("title").asText();
        String content = notification.path("content").asText();

        Assertions.assertTrue(title.contains("监考") || title.contains("安排"),
            "通知标题应包含'监考'或'安排'关键字");
        Assertions.assertTrue(content.contains("D401") || content.contains("网络安全工程师") || content.contains("2025-06-15"),
            "通知内容应包含考场号、科目名称或考试日期");

        // 5. 验证通知状态为未读
        Assertions.assertFalse(notification.path("isRead").asBoolean(),
            "新生成的通知应为未读状态");

        // 6. 测试标记通知为已读
        String notificationId = notification.path("id").asText();
        JsonNode markReadResponse = executeGraphQl(
            "mutation MarkNotificationRead($id: ID!) {" +
                " markInvigilatorNotificationRead(id: $id) { id isRead readAt } }",
            Map.of("id", notificationId),
            invigilatorToken
        );

        Assertions.assertTrue(markReadResponse.path("markInvigilatorNotificationRead").path("isRead").asBoolean(),
            "通知应标记为已读");
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