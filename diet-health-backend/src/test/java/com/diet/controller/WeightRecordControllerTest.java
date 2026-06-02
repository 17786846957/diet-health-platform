package com.diet.controller;

import com.diet.entity.User;
import com.diet.entity.WeightRecord;
import com.diet.mapper.UserMapper;
import com.diet.mapper.WeightRecordMapper;
import com.diet.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class WeightRecordControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private WeightRecordMapper weightRecordMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    private String token;
    private String otherToken;
    private Long userId;

    @BeforeEach
    void setUp() {
        weightRecordMapper.delete(null);
        userMapper.delete(null);

        // Main test user
        User user = new User();
        user.setUsername("weighttest");
        user.setPassword(passwordEncoder.encode("123456"));
        user.setRole("user");
        user.setGender("male");
        user.setAge(25);
        user.setHeight(170.0);
        user.setWeight(65.0);
        userMapper.insert(user);
        userId = user.getId();
        token = jwtUtil.generateToken(userId, "weighttest", "user");

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

    // ========== addOrUpdateRecord ==========

    @Test
    void addOrUpdateRecord_success() throws Exception {
        String body = "{\"recordDate\":\"2026-05-20\",\"weight\":70.5,\"bodyFat\":18.0,\"notes\":\"test\"}";

        mockMvc.perform(post("/weight")
                        .cookie(new Cookie("diet_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("保存成功"));
    }

    @Test
    void addOrUpdateRecord_noAuth_returns401() throws Exception {
        String body = "{\"recordDate\":\"2026-05-20\",\"weight\":70.5}";

        mockMvc.perform(post("/weight")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addOrUpdateRecord_invalidWeight_returns400() throws Exception {
        String body = "{\"recordDate\":\"2026-05-20\",\"weight\":0}";

        mockMvc.perform(post("/weight")
                        .cookie(new Cookie("diet_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addOrUpdateRecord_upsert() throws Exception {
        String body1 = "{\"recordDate\":\"2026-05-20\",\"weight\":70.5,\"notes\":\"first\"}";
        String body2 = "{\"recordDate\":\"2026-05-20\",\"weight\":71.0,\"notes\":\"updated\"}";

        // First insert
        mockMvc.perform(post("/weight")
                        .cookie(new Cookie("diet_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Same date again — should update, not duplicate
        mockMvc.perform(post("/weight")
                        .cookie(new Cookie("diet_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Only one record should exist for this user + date
        int count = weightRecordMapper.selectList(null).size();
        assert count == 1 : "Expected 1 record after upsert, got " + count;

        // Verify the weight was updated
        WeightRecord record = weightRecordMapper.selectList(null).get(0);
        assert record.getWeight().doubleValue() == 71.0 : "Expected weight 71.0 after update";
    }

    // ========== deleteRecord ==========

    @Test
    void deleteRecord_wrongUser_returns403() throws Exception {
        // Create record as main user
        String body = "{\"recordDate\":\"2026-05-20\",\"weight\":70.5}";

        mockMvc.perform(post("/weight")
                        .cookie(new Cookie("diet_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        WeightRecord record = weightRecordMapper.selectList(null).get(0);

        // Try to delete as other user
        mockMvc.perform(delete("/weight/" + record.getId())
                        .cookie(new Cookie("diet_token", otherToken)))
                .andExpect(status().isForbidden());
    }

    // ========== getLatest ==========

    @Test
    void getLatest_success() throws Exception {
        // Add a record first
        String body = "{\"recordDate\":\"2026-05-20\",\"weight\":70.5,\"bodyFat\":18.0}";

        mockMvc.perform(post("/weight")
                        .cookie(new Cookie("diet_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // Get latest
        mockMvc.perform(get("/weight/latest")
                        .cookie(new Cookie("diet_token", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.weight").value(70.5))
                .andExpect(jsonPath("$.data.bodyFat").value(18.0));
    }
}
