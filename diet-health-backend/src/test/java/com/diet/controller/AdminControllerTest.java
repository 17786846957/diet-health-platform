package com.diet.controller;

import com.diet.entity.User;
import com.diet.mapper.UserMapper;
import com.diet.mapper.FoodMapper;
import com.diet.mapper.DietRecordMapper;
import com.diet.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import javax.servlet.http.Cookie;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserMapper userMapper;
    @Autowired private FoodMapper foodMapper;
    @Autowired private DietRecordMapper dietRecordMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    private String adminToken;
    private String userToken;
    private Long adminId;
    private Long userId;

    @BeforeEach
    void setUp() {
        dietRecordMapper.delete(null);
        foodMapper.delete(null);
        userMapper.delete(null);

        // Admin user
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole("admin");
        admin.setGender("male");
        admin.setAge(30);
        admin.setHeight(175.0);
        admin.setWeight(70.0);
        userMapper.insert(admin);
        adminId = admin.getId();
        adminToken = jwtUtil.generateToken(adminId, "admin", "admin");

        // Normal user
        User user = new User();
        user.setUsername("testuser");
        user.setPassword(passwordEncoder.encode("123456"));
        user.setRole("user");
        user.setGender("female");
        user.setAge(25);
        user.setHeight(160.0);
        user.setWeight(55.0);
        userMapper.insert(user);
        userId = user.getId();
        userToken = jwtUtil.generateToken(userId, "testuser", "user");
    }

    // ========== getDashboard ==========

    @Test
    void getDashboard_asAdmin_success() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                        .cookie(new Cookie("diet_token", adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalUsers").isNumber())
                .andExpect(jsonPath("$.data.totalFoods").isNumber())
                .andExpect(jsonPath("$.data.totalRecords").isNumber());
    }

    @Test
    void getDashboard_asUser_returns403() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                        .cookie(new Cookie("diet_token", userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getDashboard_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    // ========== listUsers ==========

    @Test
    void listUsers_asAdmin_success() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .cookie(new Cookie("diet_token", adminToken))
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(2));
    }

    @Test
    void listUsers_withKeyword_filtersCorrectly() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .cookie(new Cookie("diet_token", adminToken))
                        .param("keyword", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].username").value("admin"));
    }

    @Test
    void listUsers_withRole_filtersCorrectly() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .cookie(new Cookie("diet_token", adminToken))
                        .param("role", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].username").value("testuser"));
    }

    // ========== deleteUser ==========

    @Test
    void deleteUser_asAdmin_success() throws Exception {
        mockMvc.perform(delete("/admin/users/" + userId)
                        .cookie(new Cookie("diet_token", adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("删除成功"));
    }

    @Test
    void deleteUser_self_returns400() throws Exception {
        mockMvc.perform(delete("/admin/users/" + adminId)
                        .cookie(new Cookie("diet_token", adminToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteUser_asUser_returns403() throws Exception {
        mockMvc.perform(delete("/admin/users/" + adminId)
                        .cookie(new Cookie("diet_token", userToken)))
                .andExpect(status().isForbidden());
    }
}
