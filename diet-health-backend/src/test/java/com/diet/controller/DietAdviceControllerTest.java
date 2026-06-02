package com.diet.controller;

import com.diet.entity.User;
import com.diet.entity.DietRecord;
import com.diet.entity.DietRecordDetail;
import com.diet.entity.Food;
import com.diet.mapper.UserMapper;
import com.diet.mapper.DietRecordMapper;
import com.diet.mapper.DietRecordDetailMapper;
import com.diet.mapper.FoodMapper;
import com.diet.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import javax.servlet.http.Cookie;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DietAdviceControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserMapper userMapper;
    @Autowired private DietRecordMapper dietRecordMapper;
    @Autowired private DietRecordDetailMapper dietRecordDetailMapper;
    @Autowired private FoodMapper foodMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    private String token;
    private Long userId;

    @BeforeEach
    void setUp() {
        dietRecordDetailMapper.delete(null);
        dietRecordMapper.delete(null);
        foodMapper.delete(null);
        userMapper.delete(null);

        // Create test user
        User user = new User();
        user.setUsername("advicetest");
        user.setPassword(passwordEncoder.encode("123456"));
        user.setRole("user");
        user.setGender("male");
        user.setAge(25);
        user.setHeight(170.0);
        user.setWeight(65.0);
        userMapper.insert(user);
        userId = user.getId();
        token = jwtUtil.generateToken(userId, "advicetest", "user");

        // Create test food
        Food food = new Food();
        food.setName("米饭");
        food.setCategory("主食");
        food.setCalories(116.0);
        food.setProtein(2.6);
        food.setFat(0.3);
        food.setCarbs(25.6);
        food.setFiber(0.3);
        foodMapper.insert(food);

        // Create diet record for today
        DietRecord record = new DietRecord();
        record.setUserId(userId);
        record.setRecordDate(LocalDate.now());
        record.setMealType("lunch");
        record.setTotalCalories(500.0);
        dietRecordMapper.insert(record);

        // Create diet record detail
        DietRecordDetail detail = new DietRecordDetail();
        detail.setRecordId(record.getId());
        detail.setFoodId(food.getId());
        detail.setFoodName("米饭");
        detail.setAmount(200.0);
        detail.setCalories(232.0);
        dietRecordDetailMapper.insert(detail);
    }

    // ========== getDailyAdvice ==========

    @Test
    void getDailyAdvice_success() throws Exception {
        mockMvc.perform(get("/diet-advice/daily")
                        .cookie(new Cookie("diet_token", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isMap());
    }

    @Test
    void getDailyAdvice_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/diet-advice/daily"))
                .andExpect(status().isUnauthorized());
    }

    // ========== getDietAnalysis ==========

    @Test
    void getDietAnalysis_success() throws Exception {
        mockMvc.perform(get("/diet-advice/analysis")
                        .cookie(new Cookie("diet_token", token))
                        .param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isMap());
    }

    @Test
    void getDietAnalysis_defaultDays_success() throws Exception {
        mockMvc.perform(get("/diet-advice/analysis")
                        .cookie(new Cookie("diet_token", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ========== getHealthAdvice ==========

    @Test
    void getHealthAdvice_success() throws Exception {
        mockMvc.perform(get("/diet-advice/health")
                        .cookie(new Cookie("diet_token", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isMap());
    }
}
