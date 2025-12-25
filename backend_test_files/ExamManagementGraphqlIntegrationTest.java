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
class ExamManagementGraphqlIntegrationTest {

    private static final String ADMIN_PHONE = "13899990001";
    private static final String ADMIN_PASSWORD = "Admin@2024";

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

    @BeforeEach
    void setupAdminUser() {
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

        this.webTestClient = baseWebTestClient.mutate()
            .baseUrl("http://localhost:" + port + "/graphql")
            .build();
        
        this.adminToken = loginAndGetToken(ADMIN_PHONE, ADMIN_PASSWORD);
    }

    @Test
    void examCenterManagement_crudOperations() {
        Map<String, Object> centerInput = new HashMap<>();
        centerInput.put("name", "北京考试中心");
        centerInput.put("address", "北京市朝阳区");
        centerInput.put("description", "主要考试中心");

        JsonNode createResponse = executeGraphQl(
            "mutation CreateCenter($input: ExamCenterInput!) {" +
                " createExamCenter(input: $input) { id name address description } }",
            Map.of("input", centerInput),
            adminToken
        );

        String centerId = createResponse.path("createExamCenter").path("id").asText();
        Assertions.assertEquals("北京考试中心", createResponse.path("createExamCenter").path("name").asText());

        JsonNode queryResponse = executeGraphQl(
            "query GetCenter($id: ID!) { examCenter(id: $id) { id name address description } }",
            Map.of("id", centerId),
            adminToken
        );

        Assertions.assertEquals("北京考试中心", queryResponse.path("examCenter").path("name").asText());

        Map<String, Object> updateInput = new HashMap<>();
        updateInput.put("name", "北京考试中心（更新）");
        updateInput.put("address", "北京市朝阳区新地址");
        updateInput.put("description", "更新后的描述");

        JsonNode updateResponse = executeGraphQl(
            "mutation UpdateCenter($id: ID!, $input: ExamCenterInput!) {" +
                " updateExamCenter(id: $id, input: $input) { id name address description } }",
            Map.of("id", centerId, "input", updateInput),
            adminToken
        );

        Assertions.assertEquals("北京考试中心（更新）", updateResponse.path("updateExamCenter").path("name").asText());

        JsonNode deleteResponse = executeGraphQl(
            "mutation DeleteCenter($id: ID!) { deleteExamCenter(id: $id) }",
            Map.of("id", centerId),
            adminToken
        );

        Assertions.assertTrue(deleteResponse.path("deleteExamCenter").asBoolean());
    }

    @Test
    void examRoomManagement_crudOperations() {
        Map<String, Object> centerInput = new HashMap<>();
        centerInput.put("name", "上海考试中心");
        centerInput.put("address", "上海市浦东新区");

        JsonNode centerResponse = executeGraphQl(
            "mutation CreateCenter($input: ExamCenterInput!) {" +
                " createExamCenter(input: $input) { id name } }",
            Map.of("input", centerInput),
            adminToken
        );

        String centerId = centerResponse.path("createExamCenter").path("id").asText();

        Map<String, Object> roomInput = new HashMap<>();
        roomInput.put("centerId", centerId);
        roomInput.put("roomNumber", "A101");
        roomInput.put("name", "101考场");
        roomInput.put("capacity", 50);
        roomInput.put("location", "1楼");

        JsonNode createRoomResponse = executeGraphQl(
            "mutation CreateRoom($input: ExamRoomInput!) {" +
                " createExamRoom(input: $input) { id roomNumber name capacity location } }",
            Map.of("input", roomInput),
            adminToken
        );

        String roomId = createRoomResponse.path("createExamRoom").path("id").asText();
        Assertions.assertEquals("A101", createRoomResponse.path("createExamRoom").path("roomNumber").asText());

        JsonNode roomsResponse = executeGraphQl(
            "query GetRooms($centerId: ID!) { examRooms(centerId: $centerId) { id roomNumber name capacity } }",
            Map.of("centerId", centerId),
            adminToken
        );

        Assertions.assertTrue(roomsResponse.path("examRooms").size() > 0);
        Assertions.assertEquals("A101", roomsResponse.path("examRooms").get(0).path("roomNumber").asText());
    }

    @Test
    void examSubjectManagement_crudOperations() {
        Map<String, Object> subjectInput = new HashMap<>();
        subjectInput.put("code", "SUB001");
        subjectInput.put("name", "建筑工程法规");
        subjectInput.put("description", "建筑工程相关法规知识");
        subjectInput.put("durationMinutes", 120);
        subjectInput.put("questionCount", 100);
        subjectInput.put("passLine", 60);

        JsonNode createSubjectResponse = executeGraphQl(
            "mutation CreateSubject($input: ExamSubjectInput!) {" +
                " createExamSubject(input: $input) { id code name description durationMinutes questionCount passLine } }",
            Map.of("input", subjectInput),
            adminToken
        );

        String subjectId = createSubjectResponse.path("createExamSubject").path("id").asText();
        Assertions.assertEquals("SUB001", createSubjectResponse.path("createExamSubject").path("code").asText());

        JsonNode subjectsResponse = executeGraphQl(
            "query GetSubjects { examSubjects { id code name durationMinutes questionCount } }",
            Map.of(),
            adminToken
        );

        Assertions.assertTrue(subjectsResponse.path("examSubjects").size() > 0);
        Assertions.assertEquals("SUB001", subjectsResponse.path("examSubjects").get(0).path("code").asText());

        Map<String, Object> updateInput = new HashMap<>();
        updateInput.put("code", "SUB001");
        updateInput.put("name", "建筑工程法规（更新）");
        updateInput.put("description", "更新后的描述");
        updateInput.put("durationMinutes", 150);
        updateInput.put("questionCount", 120);
        updateInput.put("passLine", 72);

        JsonNode updateResponse = executeGraphQl(
            "mutation UpdateSubject($id: ID!, $input: ExamSubjectInput!) {" +
                " updateExamSubject(id: $id, input: $input) { id code name durationMinutes questionCount } }",
            Map.of("id", subjectId, "input", updateInput),
            adminToken
        );

        Assertions.assertEquals("建筑工程法规（更新）", updateResponse.path("updateExamSubject").path("name").asText());
        Assertions.assertEquals(150, updateResponse.path("updateExamSubject").path("durationMinutes").asInt());
    }

    @Test
    void examSessionManagement_crudOperations() {
        Map<String, Object> sessionInput = new HashMap<>();
        sessionInput.put("name", "上午场");
        sessionInput.put("startTime", "2025-06-15T09:00:00Z");
        sessionInput.put("endTime", "2025-06-15T11:30:00Z");
        sessionInput.put("note", "第一场考试");

        JsonNode createSessionResponse = executeGraphQl(
            "mutation CreateSession($input: ExamSessionInput!) {" +
                " createExamSession(input: $input) { id name startTime endTime note } }",
            Map.of("input", sessionInput),
            adminToken
        );

        String sessionId = createSessionResponse.path("createExamSession").path("id").asText();
        Assertions.assertEquals("上午场", createSessionResponse.path("createExamSession").path("name").asText());

        JsonNode sessionsResponse = executeGraphQl(
            "query GetSessions { examSessions { id name startTime endTime note } }",
            Map.of(),
            adminToken
        );

        Assertions.assertTrue(sessionsResponse.path("examSessions").size() > 0);
        Assertions.assertEquals("上午场", sessionsResponse.path("examSessions").get(0).path("name").asText());

        Map<String, Object> updateInput = new HashMap<>();
        updateInput.put("name", "上午场（更新）");
        updateInput.put("startTime", "2025-06-15T08:30:00Z");
        updateInput.put("endTime", "2025-06-15T11:00:00Z");
        updateInput.put("note", "更新后的时间安排");

        JsonNode updateResponse = executeGraphQl(
            "mutation UpdateSession($id: ID!, $input: ExamSessionInput!) {" +
                " updateExamSession(id: $id, input: $input) { id name startTime endTime note } }",
            Map.of("id", sessionId, "input", updateInput),
            adminToken
        );

        Assertions.assertEquals("上午场（更新）", updateResponse.path("updateExamSession").path("name").asText());
    }

    @Test
    void examScheduleManagement_crudOperations() {
        Map<String, Object> centerInput = new HashMap<>();
        centerInput.put("name", "广州考试中心");
        centerInput.put("address", "广州市天河区");

        JsonNode centerResponse = executeGraphQl(
            "mutation CreateCenter($input: ExamCenterInput!) {" +
                " createExamCenter(input: $input) { id name } }",
            Map.of("input", centerInput),
            adminToken
        );

        String centerId = centerResponse.path("createExamCenter").path("id").asText();

        Map<String, Object> roomInput = new HashMap<>();
        roomInput.put("centerId", centerId);
        roomInput.put("roomNumber", "B201");
        roomInput.put("capacity", 40);

        JsonNode roomResponse = executeGraphQl(
            "mutation CreateRoom($input: ExamRoomInput!) {" +
                " createExamRoom(input: $input) { id roomNumber } }",
            Map.of("input", roomInput),
            adminToken
        );

        String roomId = roomResponse.path("createExamRoom").path("id").asText();

        Map<String, Object> subjectInput = new HashMap<>();
        subjectInput.put("code", "SUB002");
        subjectInput.put("name", "项目管理");
        subjectInput.put("durationMinutes", 180);
        subjectInput.put("questionCount", 150);

        JsonNode subjectResponse = executeGraphQl(
            "mutation CreateSubject($input: ExamSubjectInput!) {" +
                " createExamSubject(input: $input) { id code name } }",
            Map.of("input", subjectInput),
            adminToken
        );

        String subjectId = subjectResponse.path("createExamSubject").path("id").asText();

        Map<String, Object> sessionInput = new HashMap<>();
        sessionInput.put("name", "下午场");
        sessionInput.put("startTime", "2025-06-15T14:00:00Z");
        sessionInput.put("endTime", "2025-06-15T17:00:00Z");

        JsonNode sessionResponse = executeGraphQl(
            "mutation CreateSession($input: ExamSessionInput!) {" +
                " createExamSession(input: $input) { id name startTime endTime } }",
            Map.of("input", sessionInput),
            adminToken
        );

        String sessionId = sessionResponse.path("createExamSession").path("id").asText();

        Map<String, Object> scheduleInput = new HashMap<>();
        scheduleInput.put("roomId", roomId);
        scheduleInput.put("subjectId", subjectId);
        scheduleInput.put("sessionId", sessionId);
        scheduleInput.put("note", "重要考试安排");

        JsonNode createScheduleResponse = executeGraphQl(
            "mutation CreateSchedule($input: ExamScheduleInput!) {" +
                " createExamSchedule(input: $input) { id examRoom { roomNumber } examSubject { name } examSession { name } note } }",
            Map.of("input", scheduleInput),
            adminToken
        );

        String scheduleId = createScheduleResponse.path("createExamSchedule").path("id").asText();
        Assertions.assertEquals("B201", createScheduleResponse.path("createExamSchedule").path("examRoom").path("roomNumber").asText());
        Assertions.assertEquals("项目管理", createScheduleResponse.path("createExamSchedule").path("examSubject").path("name").asText());
        Assertions.assertEquals("下午场", createScheduleResponse.path("createExamSchedule").path("examSession").path("name").asText());

        JsonNode schedulesResponse = executeGraphQl(
            "query GetSchedules { examSchedules { id examRoom { roomNumber } examSubject { name } examSession { name } } }",
            Map.of(),
            adminToken
        );

        Assertions.assertTrue(schedulesResponse.path("examSchedules").size() > 0);
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