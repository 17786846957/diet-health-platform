package com.diet.controller;

import com.diet.entity.Food;
import com.diet.entity.User;
import com.diet.mapper.FoodFavoriteMapper;
import com.diet.mapper.FoodMapper;
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
class FavoriteControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private FoodMapper foodMapper;
    @Autowired private FoodFavoriteMapper foodFavoriteMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    private String token;
    private String otherToken;
    private Long userId;
    private Long foodId;

    @BeforeEach
    void setUp() {
        foodFavoriteMapper.delete(null);
        foodMapper.delete(null);
        userMapper.delete(null);

        // Main test user
        User user = new User();
        user.setUsername("favtest");
        user.setPassword(passwordEncoder.encode("123456"));
        user.setRole("user");
        user.setGender("male");
        user.setAge(25);
        user.setHeight(170.0);
        user.setWeight(65.0);
        userMapper.insert(user);
        userId = user.getId();
        token = jwtUtil.generateToken(userId, "favtest", "user");

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

        // Test food
        Food food = new Food();
        food.setName("TestFood");
        food.setCategory("主食");
        food.setCalories(100.0);
        food.setProtein(5.0);
        food.setFat(2.0);
        food.setCarbs(18.0);
        foodMapper.insert(food);
        foodId = food.getId();
    }

    // ========== addFavorite ==========

    @Test
    void addFavorite_success() throws Exception {
        String body = "{\"foodId\":" + foodId + "}";

        mockMvc.perform(post("/favorites")
                        .cookie(new Cookie("diet_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("收藏成功"));
    }

    @Test
    void addFavorite_noAuth_returns401() throws Exception {
        String body = "{\"foodId\":" + foodId + "}";

        mockMvc.perform(post("/favorites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addFavorite_duplicate_returns200() throws Exception {
        String body = "{\"foodId\":" + foodId + "}";

        // First add
        mockMvc.perform(post("/favorites")
                        .cookie(new Cookie("diet_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Second add — service silently ignores duplicate
        mockMvc.perform(post("/favorites")
                        .cookie(new Cookie("diet_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ========== removeFavorite ==========

    @Test
    void removeFavorite_success() throws Exception {
        // Add favorite first
        String body = "{\"foodId\":" + foodId + "}";

        mockMvc.perform(post("/favorites")
                        .cookie(new Cookie("diet_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // Remove favorite
        mockMvc.perform(delete("/favorites/" + foodId)
                        .cookie(new Cookie("diet_token", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ========== getFavorites ==========

    @Test
    void getFavorites_success() throws Exception {
        // Add favorite first
        String body = "{\"foodId\":" + foodId + "}";

        mockMvc.perform(post("/favorites")
                        .cookie(new Cookie("diet_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // Get favorites list
        mockMvc.perform(get("/favorites")
                        .cookie(new Cookie("diet_token", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].foodId").value(foodId));
    }

    // ========== getFavoriteIds ==========

    @Test
    void getFavoriteIds_success() throws Exception {
        // Add favorite first
        String body = "{\"foodId\":" + foodId + "}";

        mockMvc.perform(post("/favorites")
                        .cookie(new Cookie("diet_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // Get favorite food ID set
        mockMvc.perform(get("/favorites/ids")
                        .cookie(new Cookie("diet_token", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0]").value(foodId));
    }
}
