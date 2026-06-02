package com.diet.controller;

import com.diet.entity.BodySymptom;
import com.diet.entity.User;
import com.diet.mapper.BodySymptomMapper;
import com.diet.mapper.UserMapper;
import com.diet.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import javax.servlet.http.Cookie;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BodySymptomControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserMapper userMapper;
    @Autowired private BodySymptomMapper bodySymptomMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    private String token;
    private String otherToken;
    private Long userId;

    @BeforeEach
    void setUp() {
        bodySymptomMapper.delete(null);
        userMapper.delete(null);

        // Main test user
        User user = new User();
        user.setUsername("symptomtest");
        user.setPassword(passwordEncoder.encode("123456"));
        user.setRole("user");
        user.setGender("male");
        user.setAge(25);
        user.setHeight(170.0);
        user.setWeight(65.0);
        userMapper.insert(user);
        userId = user.getId();
        token = jwtUtil.generateToken(userId, "symptomtest", "user");

        // Another user for cross-user permission tests
        User other = new User();
        other.setUsername("other");
        other.setPassword(passwordEncoder.encode("123456"));
        other.setRole("user");
        other.setGender("female");
        other.setAge(30);
        other.setHeight(160.0);
        other.setWeight(55.0);
        userMapper.insert(other);
        otherToken = jwtUtil.generateToken(other.getId(), "other", "user");
    }

    // ========== addRecord ==========

    @Test
    void addRecord_success() throws Exception {
        String body = "{\"recordDate\":\"2026-05-12\",\"symptomType\":\"headache\","
                + "\"severity\":5,\"description\":\"轻微头痛\",\"possibleCause\":\"睡眠不足\"}";

        mockMvc.perform(post("/symptom")
                        .cookie(new Cookie("diet_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("添加成功"));
    }

    @Test
    void addRecord_noAuth_returns401() throws Exception {
        String body = "{\"recordDate\":\"2026-05-12\",\"symptomType\":\"headache\",\"severity\":5}";

        mockMvc.perform(post("/symptom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addRecord_missingSymptomType_returns400() throws Exception {
        String body = "{\"recordDate\":\"2026-05-12\",\"severity\":5}";

        mockMvc.perform(post("/symptom")
                        .cookie(new Cookie("diet_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addRecord_invalidSeverity_returns400() throws Exception {
        String body = "{\"recordDate\":\"2026-05-12\",\"symptomType\":\"headache\",\"severity\":11}";

        mockMvc.perform(post("/symptom")
                        .cookie(new Cookie("diet_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ========== deleteRecord ==========

    @Test
    void deleteRecord_success() throws Exception {
        // Create a record first
        String createBody = "{\"recordDate\":\"2026-05-12\",\"symptomType\":\"headache\",\"severity\":5}";

        mockMvc.perform(post("/symptom")
                        .cookie(new Cookie("diet_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk());

        BodySymptom record = bodySymptomMapper.selectList(null).get(0);

        mockMvc.perform(delete("/symptom/" + record.getId())
                        .cookie(new Cookie("diet_token", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deleteRecord_wrongUser_returns403() throws Exception {
        // Create record as main user
        String createBody = "{\"recordDate\":\"2026-05-12\",\"symptomType\":\"headache\",\"severity\":5}";

        mockMvc.perform(post("/symptom")
                        .cookie(new Cookie("diet_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk());

        BodySymptom record = bodySymptomMapper.selectList(null).get(0);

        // Try to delete as other user
        mockMvc.perform(delete("/symptom/" + record.getId())
                        .cookie(new Cookie("diet_token", otherToken)))
                .andExpect(status().isForbidden());
    }

    // ========== listByDateRange ==========

    @Test
    void listByDateRange_success() throws Exception {
        // Add a record first
        String createBody = "{\"recordDate\":\"2026-05-12\",\"symptomType\":\"headache\",\"severity\":5}";

        mockMvc.perform(post("/symptom")
                        .cookie(new Cookie("diet_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk());

        mockMvc.perform(get("/symptom/list")
                        .cookie(new Cookie("diet_token", token))
                        .param("start", "2026-05-01")
                        .param("end", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].symptomType").value("headache"));
    }

    // ========== getSymptomAnalysis ==========

    @Test
    void getSymptomAnalysis_success() throws Exception {
        // Add some records first
        String body1 = "{\"recordDate\":\"2026-05-10\",\"symptomType\":\"headache\",\"severity\":5}";
        String body2 = "{\"recordDate\":\"2026-05-11\",\"symptomType\":\"headache\",\"severity\":7}";
        String body3 = "{\"recordDate\":\"2026-05-12\",\"symptomType\":\"fatigue\",\"severity\":3}";

        mockMvc.perform(post("/symptom")
                        .cookie(new Cookie("diet_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body1))
                .andExpect(status().isOk());
        mockMvc.perform(post("/symptom")
                        .cookie(new Cookie("diet_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body2))
                .andExpect(status().isOk());
        mockMvc.perform(post("/symptom")
                        .cookie(new Cookie("diet_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body3))
                .andExpect(status().isOk());

        mockMvc.perform(get("/symptom/analysis")
                        .cookie(new Cookie("diet_token", token))
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalRecords").value(3))
                .andExpect(jsonPath("$.data.avgSeverity").isMap());
    }
}
