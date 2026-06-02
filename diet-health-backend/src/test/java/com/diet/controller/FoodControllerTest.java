package com.diet.controller;

import com.diet.entity.Food;
import com.diet.entity.User;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FoodControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FoodMapper foodMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        foodMapper.delete(null);
        userMapper.delete(null);

        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("123456"));
        admin.setRole("admin");
        userMapper.insert(admin);
        adminToken = jwtUtil.generateToken(admin.getId(), admin.getUsername(), admin.getRole());

        User user = new User();
        user.setUsername("user");
        user.setPassword(passwordEncoder.encode("123456"));
        user.setRole("user");
        userMapper.insert(user);
        userToken = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
    }

    @Test
    void listFoods_returnsPage() throws Exception {
        Food food = new Food();
        food.setName("苹果");
        food.setCategory("水果");
        food.setCalories(52.0);
        foodMapper.insert(food);

        mockMvc.perform(get("/food/list")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void getById_found() throws Exception {
        Food food = new Food();
        food.setName("米饭");
        food.setCategory("主食");
        food.setCalories(116.0);
        foodMapper.insert(food);

        mockMvc.perform(get("/food/" + food.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("米饭"));
    }

    @Test
    void getById_notFound_throws() throws Exception {
        mockMvc.perform(get("/food/999")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void addFood_admin_success() throws Exception {
        Food food = new Food();
        food.setName("苹果");
        food.setCategory("水果");
        food.setCalories(52.0);

        mockMvc.perform(post("/food")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(food)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void addFood_user_forbidden() throws Exception {
        Food food = new Food();
        food.setName("苹果");
        food.setCategory("水果");
        food.setCalories(52.0);

        mockMvc.perform(post("/food")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(food)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateFood_admin_success() throws Exception {
        Food food = new Food();
        food.setName("米饭");
        food.setCategory("主食");
        food.setCalories(116.0);
        foodMapper.insert(food);

        String body = "{\"id\":" + food.getId() + ",\"name\":\"糙米饭\",\"category\":\"主食\",\"calories\":111.0}";

        mockMvc.perform(put("/food")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void updateFood_user_forbidden() throws Exception {
        String body = "{\"id\":1,\"name\":\"test\",\"category\":\"主食\",\"calories\":100.0}";

        mockMvc.perform(put("/food")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateFood_notFound_returns404() throws Exception {
        String body = "{\"id\":999,\"name\":\"test\",\"category\":\"主食\",\"calories\":100.0}";

        mockMvc.perform(put("/food")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteFood_admin_success() throws Exception {
        Food food = new Food();
        food.setName("苹果");
        food.setCategory("水果");
        food.setCalories(52.0);
        foodMapper.insert(food);

        mockMvc.perform(delete("/food/" + food.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deleteFood_user_forbidden() throws Exception {
        mockMvc.perform(delete("/food/1")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteFood_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/food/999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeFoodImage_admin_success() throws Exception {
        Food food = new Food();
        food.setName("苹果");
        food.setCategory("水果");
        food.setCalories(52.0);
        food.setImageUrl("http://example.com/img.jpg");
        foodMapper.insert(food);

        mockMvc.perform(delete("/food/" + food.getId() + "/image")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void addFood_noAuth_returns401() throws Exception {
        Food food = new Food();
        food.setName("苹果");
        food.setCategory("水果");
        food.setCalories(52.0);

        mockMvc.perform(post("/food")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(food)))
                .andExpect(status().isUnauthorized());
    }
}
