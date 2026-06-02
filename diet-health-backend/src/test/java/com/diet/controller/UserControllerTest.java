package com.diet.controller;

import com.diet.entity.User;
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
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserMapper userMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    private String token;
    private Long userId;

    @BeforeEach
    void setUp() {
        userMapper.delete(null);

        User user = new User();
        user.setUsername("profiletest");
        user.setPassword(passwordEncoder.encode("123456"));
        user.setRole("user");
        user.setGender("male");
        user.setAge(25);
        user.setHeight(170.0);
        user.setWeight(65.0);
        user.setEmail("test@example.com");
        userMapper.insert(user);
        userId = user.getId();
        token = jwtUtil.generateToken(userId, "profiletest", "user");
    }

    // ========== getProfile ==========

    @Test
    void getProfile_success() throws Exception {
        mockMvc.perform(get("/user/profile")
                        .cookie(new Cookie("diet_token", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("profiletest"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.gender").value("male"))
                .andExpect(jsonPath("$.data.age").value(25));
    }

    @Test
    void getProfile_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/user/profile"))
                .andExpect(status().isUnauthorized());
    }

    // ========== updateProfile ==========

    @Test
    void updateProfile_success() throws Exception {
        String body = "{\"email\":\"new@example.com\",\"gender\":\"female\",\"age\":30,"
                + "\"height\":165.0,\"weight\":60.0,\"activityLevel\":\"moderate\",\"goal\":\"lose\"}";

        mockMvc.perform(put("/user/profile")
                        .cookie(new Cookie("diet_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("更新成功"));

        // Verify update
        mockMvc.perform(get("/user/profile")
                        .cookie(new Cookie("diet_token", token)))
                .andExpect(jsonPath("$.data.email").value("new@example.com"))
                .andExpect(jsonPath("$.data.gender").value("female"))
                .andExpect(jsonPath("$.data.age").value(30));
    }

    @Test
    void updateProfile_invalidEmail_returns400() throws Exception {
        String body = "{\"email\":\"invalid-email\",\"gender\":\"male\",\"age\":25}";

        mockMvc.perform(put("/user/profile")
                        .cookie(new Cookie("diet_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
