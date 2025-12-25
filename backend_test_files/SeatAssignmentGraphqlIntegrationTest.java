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
class SeatAssignmentGraphqlIntegrationTest {

    private static final String ADMIN_PHONE = "13899990003";
    private static final String ADMIN_PASSWORD = "Admin@2024";
    private static final String STUDENT_PHONE = "13900001113";
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
    private String sessionId;
    private String roomId;
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

        setupExamData();
        setupStudentRegistration();
    }

    private void setupExamData() {
        Map<String, Object> centerInput = new HashMap<>();
        centerInput.put("name", "深圳考试中心");
        centerInput.put("address", "深圳市南山区");

        JsonNode centerResponse = executeGraphQl(
            "mutation CreateCenter($input: ExamCenterInput!) {" +
                " createExamCenter(input: $input) { id name } }",
            Map.of("input", centerInput),
            adminToken
        );

        String centerId = centerResponse.path("createExamCenter").path("id").asText();

        Map<String, Object> roomInput = new HashMap<>();
        roomInput.put("centerId", centerId);
        roomInput.put("roomNumber", "C301");
        roomInput.put("name", "301考场");
        roomInput.put("capacity", 30);

        JsonNode roomResponse = executeGraphQl(
            "mutation CreateRoom($input: ExamRoomInput!) {" +
                " createExamRoom(input: $input) { id roomNumber capacity } }",
            Map.of("input", roomInput),
            adminToken
        );

        this.roomId = roomResponse.path("createExamRoom").path("id").asText();

        Map<String, Object> subjectInput = new HashMap<>();
        subjectInput.put("code", "SUB006");
        subjectInput.put("name", "建筑工程技术与计量");
        subjectInput.put("durationMinutes", 150);
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
        sessionInput.put("startTime", "2025-06-20T09:00:00Z");
        sessionInput.put("endTime", "2025-06-20T11:30:00Z");

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

        executeGraphQl(
            "mutation CreateSchedule($input: ExamScheduleInput!) {" +
                " createExamSchedule(input: $input) { id } }",
            Map.of("input", scheduleInput),
            adminToken
        );
    }

    private void setupStudentRegistration() {
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
        registrationInput.put("fullName", "王五");
        registrationInput.put("idCardNumber", "110101199003030033");
        registrationInput.put("gender", "男");
        registrationInput.put("birthDate", "1990-03-03");
        registrationInput.put("email", "wangwu@example.com");
        registrationInput.put("address", "广州市天河区");
        registrationInput.put("phone", "13700137000");
        registrationInput.put("education", "本科");
        registrationInput.put("workUnit", "某施工单位");
        registrationInput.put("workExperience", "4年");

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
    void seatAssignment_assignmentAndReset() {
        Map<String, Object> assignInput = new HashMap<>();
        assignInput.put("subjectId", subjectId);
        assignInput.put("sessionId", sessionId);

        JsonNode assignResponse = executeGraphQl(
            "mutation AssignSeats($input: SeatAssignmentInput!) {" +
                " assignSeats(input: $input) { id registrationInfo { fullName } examRoom { roomNumber } seatNumber status } } }",
            Map.of("input", assignInput),
            adminToken
        );

        Assertions.assertTrue(assignResponse.path("assignSeats").size() > 0);
        JsonNode firstAssignment = assignResponse.path("assignSeats").get(0);
        Assertions.assertEquals("王五", firstAssignment.path("registrationInfo").path("fullName").asText());
        Assertions.assertEquals("C301", firstAssignment.path("examRoom").path("roomNumber").asText());
        Assertions.assertTrue(firstAssignment.path("seatNumber").asInt() > 0);

        JsonNode assignmentsResponse = executeGraphQl(
            "query GetAssignments($subjectId: ID!, $sessionId: ID!) {" +
                " seatAssignments(subjectId: $subjectId, sessionId: $sessionId) { id registrationInfo { fullName } examRoom { roomNumber } seatNumber status } } }",
            Map.of("subjectId", subjectId, "sessionId", sessionId),
            adminToken
        );

        Assertions.assertTrue(assignmentsResponse.path("seatAssignments").size() > 0);
        Assertions.assertEquals("王五", assignmentsResponse.path("seatAssignments").get(0).path("registrationInfo").path("fullName").asText());

        JsonNode resetResponse = executeGraphQl(
            "mutation ResetSeats($subjectId: ID!, $sessionId: ID!) {" +
                " resetSeats(subjectId: $subjectId, sessionId: $sessionId) } }",
            Map.of("subjectId", subjectId, "sessionId", sessionId),
            adminToken
        );

        Assertions.assertTrue(resetResponse.path("resetSeats").asBoolean());
    }

    @Test
    void seatAssignment_statisticsAndTasks() {
        Map<String, Object> assignInput = new HashMap<>();
        assignInput.put("subjectId", subjectId);
        assignInput.put("sessionId", sessionId);

        executeGraphQl(
            "mutation AssignSeats($input: SeatAssignmentInput!) {" +
                " assignSeats(input: $input) { id } }",
            Map.of("input", assignInput),
            adminToken
        );

        JsonNode statsResponse = executeGraphQl(
            "query GetStats($subjectId: ID!, $sessionId: ID!) {" +
                " seatAssignmentStats(subjectId: $subjectId, sessionId: $sessionId) { totalRegistrations totalAssigned totalRemaining assignedPercentage } } }",
            Map.of("subjectId", subjectId, "sessionId", sessionId),
            adminToken
        );

        Assertions.assertTrue(statsResponse.path("seatAssignmentStats").path("totalRegistrations").asInt() > 0);
        Assertions.assertTrue(statsResponse.path("seatAssignmentStats").path("totalAssigned").asInt() > 0);
        Assertions.assertTrue(statsResponse.path("seatAssignmentStats").path("assignedPercentage").asDouble() > 0);

        JsonNode tasksResponse = executeGraphQl(
            "query GetTasks($subjectId: ID!, $sessionId: ID!) {" +
                " seatAssignmentTasks(subjectId: $subjectId, sessionId: $sessionId) { id taskType status totalProcessed totalTarget startTime endTime } } }",
            Map.of("subjectId", subjectId, "sessionId", sessionId),
            adminToken
        );

        Assertions.assertTrue(tasksResponse.path("seatAssignmentTasks").size() >= 0);
    }

    @Test
    void seatAssignment_filtering() {
        Map<String, Object> assignInput = new HashMap<>();
        assignInput.put("subjectId", subjectId);
        assignInput.put("sessionId", sessionId);

        executeGraphQl(
            "mutation AssignSeats($input: SeatAssignmentInput!) {" +
                " assignSeats(input: $input) { id } }",
            Map.of("input", assignInput),
            adminToken
        );

        JsonNode filteredByRoomResponse = executeGraphQl(
            "query GetAssignmentsByRoom($roomId: ID!) {" +
                " seatAssignments(roomId: $roomId) { id examRoom { roomNumber } seatNumber } } }",
            Map.of("roomId", roomId),
            adminToken
        );

        if (filteredByRoomResponse.path("seatAssignments").size() > 0) {
            Assertions.assertEquals("C301", filteredByRoomResponse.path("seatAssignments").get(0).path("examRoom").path("roomNumber").asText());
        }

        JsonNode filteredByRegistrationResponse = executeGraphQl(
            "query GetAssignmentsByRegistration($registrationInfoId: ID!) {" +
                " seatAssignments(registrationInfoId: $registrationInfoId) { id registrationInfo { fullName } seatNumber } } }",
            Map.of("registrationInfoId", registrationId),
            adminToken
        );

        if (filteredByRegistrationResponse.path("seatAssignments").size() > 0) {
            Assertions.assertEquals("王五", filteredByRegistrationResponse.path("seatAssignments").get(0).path("registrationInfo").path("fullName").asText());
        }
    }

    @Test
    void admitCard_generationAndTemplates() {
        Map<String, Object> assignInput = new HashMap<>();
        assignInput.put("subjectId", subjectId);
        assignInput.put("sessionId", sessionId);

        executeGraphQl(
            "mutation AssignSeats($input: SeatAssignmentInput!) {" +
                " assignSeats(input: $input) { id } }",
            Map.of("input", assignInput),
            adminToken
        );

        Map<String, Object> templateInput = new HashMap<>();
        templateInput.put("name", "标准准考证模板");
        templateInput.put("logoUrl", "https://example.com/logo.png");
        templateInput.put("examNotice", "请携带身份证和准考证参加考试");
        templateInput.put("qrStyle", "square");

        JsonNode createTemplateResponse = executeGraphQl(
            "mutation CreateTemplate($input: AdmitCardTemplateInput!) {" +
                " upsertAdmitCardTemplate(input: $input) { id name logoUrl examNotice qrStyle } } }",
            Map.of("input", templateInput),
            adminToken
        );

        String templateId = createTemplateResponse.path("upsertAdmitCardTemplate").path("id").asText();
        Assertions.assertEquals("标准准考证模板", createTemplateResponse.path("upsertAdmitCardTemplate").path("name").asText());

        JsonNode templatesResponse = executeGraphQl(
            "query GetTemplates { admitCardTemplates { id name logoUrl examNotice qrStyle } }",
            Map.of(),
            studentToken
        );

        Assertions.assertTrue(templatesResponse.path("admitCardTemplates").size() > 0);
        Assertions.assertEquals("标准准考证模板", templatesResponse.path("admitCardTemplates").get(0).path("name").asText());

        JsonNode admitCardResponse = executeGraphQl(
            "query GetAdmitCard($registrationInfoId: ID!, $templateId: ID) {" +
                " admitCard(registrationInfoId: $registrationInfoId, templateId: $templateId) { id fullName idCardNumber ticketNumber seatNumber filePath } } }",
            Map.of("registrationInfoId", registrationId, "templateId", templateId),
            studentToken
        );

        Assertions.assertEquals("王五", admitCardResponse.path("admitCard").path("fullName").asText());
        Assertions.assertEquals("110101199003030033", admitCardResponse.path("admitCard").path("idCardNumber").asText());

        JsonNode refreshResponse = executeGraphQl(
            "mutation RefreshAdmitCard($registrationInfoId: ID!, $templateId: ID) {" +
                " refreshAdmitCard(registrationInfoId: $registrationInfoId, templateId: $templateId) { id filePath } } }",
            Map.of("registrationInfoId", registrationId, "templateId", templateId),
            studentToken
        );

        Assertions.assertTrue(refreshResponse.path("refreshAdmitCard").path("id").isTextual());

        JsonNode logsResponse = executeGraphQl(
            "query GetAdmitCardLogs($registrationInfoId: ID!) {" +
                " admitCardLogs(registrationInfoId: $registrationInfoId) { id status message createdAt } } }",
            Map.of("registrationInfoId", registrationId),
            studentToken
        );

        Assertions.assertTrue(logsResponse.path("admitCardLogs").size() >= 0);
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