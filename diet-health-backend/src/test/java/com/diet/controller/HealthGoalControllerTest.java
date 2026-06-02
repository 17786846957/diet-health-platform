package com.diet.controller;

import com.diet.entity.HealthGoal;
import com.diet.entity.User;
import com.diet.mapper.HealthGoalMapper;
import com.diet.mapper.UserMapper;
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
class HealthGoalControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private HealthGoalMapper healthGoalMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    private String token;
    private String otherToken;
    private Long userId;

    @BeforeEach
    void setUp() {
        healthGoalMapper.delete(null);
        userMapper.delete(null);

        // Main test user
        User user = new User();
        user.setUsername("goaltest");
        user.setPassword(passwordEncoder.encode("123456"));
        user.setRole("user");
        user.setGender("male");
        user.setAge(25);
        user.setHeight(170.0);
        user.setWeight(65.0);
        userMapper.insert(user);
        userId = user.getId();
        token = jwtUtil.generateToken(userId, "goaltest", "user");

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

    // ========== createGoal ==========

    @Test
    void createGoal_success() throws Exception {
        String body = "{\"goalType\":\"lose_weight\",\"targetWeight\":60.0,\"targetCalories\":1800.0,"
                + "\"targetProtein\":120.0,\"targetFat\":50.0,\"targetCarbs\":200.0,\"targetWater\":2000.0}";

        mockMvc.perform(post("/health-goal")
                        .cookie(new Cookie("diet_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("创建成功"));
    }

    @Test
    void createGoal_noAuth_returns401() throws Exception {
        String body = "{\"goalType\":\"lose_weight\",\"targetWeight\":60.0}";

        mockMvc.perform(post("/health-goal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createGoal_missingGoalType_returns400() throws Exception {
        String body = "{\"targetWeight\":60.0,\"targetCalories\":1800.0}";

        mockMvc.perform(post("/health-goal")
                        .cookie(new Cookie("diet_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ========== completeGoal ==========

    @Test
    void completeGoal_success() throws Exception {
        // Create a goal first
        String body = "{\"goalType\":\"lose_weight\",\"targetWeight\":60.0}";

        mockMvc.perform(post("/health-goal")
                        .cookie(new Cookie("diet_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        HealthGoal goal = healthGoalMapper.selectList(null).get(0);

        mockMvc.perform(post("/health-goal/" + goal.getId() + "/complete")
                        .cookie(new Cookie("diet_token", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("已完成"));
    }

    // ========== cancelGoal ==========

    @Test
    void cancelGoal_success() throws Exception {
        // Create a goal first
        String body = "{\"goalType\":\"lose_weight\",\"targetWeight\":60.0}";

        mockMvc.perform(post("/health-goal")
                        .cookie(new Cookie("diet_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        HealthGoal goal = healthGoalMapper.selectList(null).get(0);

        mockMvc.perform(post("/health-goal/" + goal.getId() + "/cancel")
                        .cookie(new Cookie("diet_token", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("已取消"));
    }

    // ========== listGoals ==========

    @Test
    void listGoals_success() throws Exception {
        // Create a goal first
        String body = "{\"goalType\":\"lose_weight\",\"targetWeight\":60.0}";

        mockMvc.perform(post("/health-goal")
                        .cookie(new Cookie("diet_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(get("/health-goal/list")
                        .cookie(new Cookie("diet_token", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].goalType").value("lose_weight"));
    }
}
