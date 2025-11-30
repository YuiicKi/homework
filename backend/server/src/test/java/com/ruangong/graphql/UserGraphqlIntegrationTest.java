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
class UserGraphqlIntegrationTest {

    private static final String ADMIN_PHONE = "13899990000";
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
    }

    @Test
    void registerStudentAndLoginSuccessfully() {
        Map<String, Object> input = new HashMap<>();
        input.put("phone", "13900001111");
        input.put("password", "Student@123");
        input.put("fullName", "测试考生");
        input.put("idCardNumber", "110101199001010011");
        input.put("photoUrl", null);

        JsonNode registerNode = executeGraphQl(
            "mutation Register($input: StudentRegisterInput!) {"
                + " registerStudent(input: $input) { token user { id phone roles { name } } } }",
            Map.of("input", input),
            null
        );

        JsonNode userNode = registerNode.path("registerStudent").path("user");
        Assertions.assertTrue(userNode.path("id").isTextual());
        Assertions.assertEquals("13900001111", userNode.path("phone").asText());
        Assertions.assertEquals("student",
            userNode.path("roles").get(0).path("name").asText());
        Assertions.assertTrue(registerNode.path("registerStudent").path("token").isTextual());

        JsonNode loginNode = executeGraphQl(
            "mutation Login($phone:String!, $pwd:String!){"
                + " login(phone:$phone, password:$pwd){ token user { roles { name } } } }",
            Map.of("phone", "13900001111", "pwd", "Student@123"),
            null
        );

        Assertions.assertTrue(loginNode.path("login").path("token").isTextual());
        Assertions.assertEquals("student",
            loginNode.path("login").path("user").path("roles").get(0).path("name").asText());
    }

    @Test
    void adminCanCreateTeacherUser() {
        String adminToken = loginAndGetToken(ADMIN_PHONE, ADMIN_PASSWORD);

        Map<String, Object> input = new HashMap<>();
        input.put("phone", "13988887777");
        input.put("password", "Teacher@123");
        input.put("roleName", "teacher");
        input.put("fullName", "魏老师");
        input.put("staffId", "T2024");
        input.put("schoolOrDepartment", "数学系");
        input.put("department", null);

        JsonNode response = executeGraphQl(
            "mutation Create($input: AdminCreateUserInput!){"
                + " adminCreateUser(input:$input){"
                + " id phone fullName roles { name } profile { ... on TeacherProfile { fullName staffId schoolOrDepartment } }"
                + " } }",
            Map.of("input", input),
            adminToken
        );

        JsonNode created = response.path("adminCreateUser");
        Assertions.assertTrue(created.path("id").isTextual());
        Assertions.assertEquals("teacher", created.path("roles").get(0).path("name").asText());
        Assertions.assertEquals("魏老师", created.path("profile").path("fullName").asText());
        Assertions.assertEquals("魏老师", created.path("fullName").asText());
    }

    @Test
    void teacherCannotCreateAdminUser() {
        String adminToken = loginAndGetToken(ADMIN_PHONE, ADMIN_PASSWORD);

        Map<String, Object> teacherInput = new HashMap<>();
        teacherInput.put("phone", "13988887779");
        teacherInput.put("password", "Teacher@123");
        teacherInput.put("roleName", "teacher");
        teacherInput.put("fullName", "李老师");
        teacherInput.put("staffId", "T3001");
        teacherInput.put("schoolOrDepartment", "物理系");
        teacherInput.put("department", null);

        executeGraphQl(
            "mutation Create($input: AdminCreateUserInput!){"
                + " adminCreateUser(input:$input){ id } }",
            Map.of("input", teacherInput),
            adminToken
        );

        String teacherToken = loginAndGetToken("13988887779", "Teacher@123");

        Map<String, Object> adminInput = new HashMap<>();
        adminInput.put("phone", "13977776666");
        adminInput.put("password", "Admin@123");
        adminInput.put("roleName", "admin");
        adminInput.put("fullName", "测试管理员");
        adminInput.put("staffId", "ADM002");
        adminInput.put("schoolOrDepartment", null);
        adminInput.put("department", "测试部");

        JsonNode root = executeGraphQlRoot(
            "mutation Create($input: AdminCreateUserInput!){"
                + " adminCreateUser(input:$input){ id } }",
            Map.of("input", adminInput),
            teacherToken
        );

        Assertions.assertTrue(root.has("errors"));
        Assertions.assertEquals("无权访问",
            root.path("errors").get(0).path("message").asText());
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
