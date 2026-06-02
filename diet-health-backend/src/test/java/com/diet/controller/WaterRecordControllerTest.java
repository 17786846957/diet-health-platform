package com.diet.controller;

import com.diet.entity.WaterRecord;
import com.diet.mapper.WaterRecordMapper;
import com.diet.mapper.UserMapper;
import com.diet.entity.User;
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
class WaterRecordControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private WaterRecordMapper waterRecordMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    private String token;
    private String otherToken;
    private Long userId;

    @BeforeEach
    void setUp() {
        waterRecordMapper.delete(null);
        userMapper.delete(null);

        // Main test user
        User user = new User();
        user.setUsername("watertest");
        user.setPassword(passwordEncoder.encode("123456"));
        user.setRole("user");
        user.setGender("male");
        user.setAge(25);
        user.setHeight(170.0);
        user.setWeight(65.0);
        userMapper.insert(user);
        userId = user.getId();
        token = jwtUtil.generateToken(userId, "watertest", "user");

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
        String body = "{\"recordDate\":\"2026-05-12\",\"amount\":500,\"drinkType\":\"water\",\"recordTime\":\"10:30:00\"}";

        mockMvc.perform(post("/water")
                        .cookie(new Cookie("diet_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("添加成功"));
    }

    @Test
    void addRecord_noAuth_returns401() throws Exception {
        String body = "{\"recordDate\":\"2026-05-12\",\"amount\":500}";

        mockMvc.perform(post("/water")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addRecord_invalidAmount_returns400() throws Exception {
        String body = "{\"recordDate\":\"2026-05-12\",\"amount\":0}";

        mockMvc.perform(post("/water")
                        .cookie(new Cookie("diet_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ========== deleteRecord ==========

    @Test
    void deleteRecord_success() throws Exception {
        // Create a record first
        String createBody = "{\"recordDate\":\"2026-05-12\",\"amount\":500,\"drinkType\":\"water\"}";

        mockMvc.perform(post("/water")
                        .cookie(new Cookie("diet_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk());

        WaterRecord record = waterRecordMapper.selectList(null).get(0);

        mockMvc.perform(delete("/water/" + record.getId())
                        .cookie(new Cookie("diet_token", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deleteRecord_wrongUser_returns403() throws Exception {
        // Create record as main user
        String createBody = "{\"recordDate\":\"2026-05-12\",\"amount\":500,\"drinkType\":\"water\"}";

        mockMvc.perform(post("/water")
                        .cookie(new Cookie("diet_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk());

        WaterRecord record = waterRecordMapper.selectList(null).get(0);

        // Try to delete as other user
        mockMvc.perform(delete("/water/" + record.getId())
                        .cookie(new Cookie("diet_token", otherToken)))
                .andExpect(status().isForbidden());
    }

    // ========== listByDate ==========

    @Test
    void listByDate_success() throws Exception {
        // Add a record first
        String createBody = "{\"recordDate\":\"2026-05-12\",\"amount\":500,\"drinkType\":\"water\"}";

        mockMvc.perform(post("/water")
                        .cookie(new Cookie("diet_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk());

        mockMvc.perform(get("/water/list")
                        .cookie(new Cookie("diet_token", token))
                        .param("date", "2026-05-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].amount").value(500));
    }
}
