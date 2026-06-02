package com.diet.controller;

import com.diet.entity.DietRecord;
import com.diet.entity.Food;
import com.diet.entity.User;
import com.diet.mapper.DietRecordDetailMapper;
import com.diet.mapper.DietRecordMapper;
import com.diet.mapper.FamilyMemberMapper;
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
class DietRecordControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private DietRecordMapper dietRecordMapper;
    @Autowired private DietRecordDetailMapper detailMapper;
    @Autowired private FoodMapper foodMapper;
    @Autowired private FamilyMemberMapper familyMemberMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    private String token;
    private String otherToken;
    private Long userId;
    private Long foodId;

    @BeforeEach
    void setUp() {
        detailMapper.delete(null);
        dietRecordMapper.delete(null);
        familyMemberMapper.delete(null);
        userMapper.delete(null);
        foodMapper.delete(null);

        // Main test user
        User user = new User();
        user.setUsername("diettest");
        user.setPassword(passwordEncoder.encode("123456"));
        user.setRole("user");
        user.setGender("male");
        user.setAge(25);
        user.setHeight(170.0);
        user.setWeight(65.0);
        userMapper.insert(user);
        userId = user.getId();
        token = jwtUtil.generateToken(userId, "diettest", "user");

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

    // ========== addRecord ==========

    @Test
    void addRecord_success() throws Exception {
        String body = "{\"recordDate\":\"2026-05-12\",\"mealType\":\"lunch\"," +
                "\"details\":[{\"foodId\":" + foodId + ",\"amount\":200}]}";

        mockMvc.perform(post("/diet")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("添加成功"));
    }

    @Test
    void addRecord_invalidMealType_returns400() throws Exception {
        String body = "{\"recordDate\":\"2026-05-12\",\"mealType\":\"invalid\"," +
                "\"details\":[{\"foodId\":" + foodId + ",\"amount\":200}]}";

        mockMvc.perform(post("/diet")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addRecord_missingDate_returns400() throws Exception {
        String body = "{\"mealType\":\"lunch\"," +
                "\"details\":[{\"foodId\":" + foodId + ",\"amount\":200}]}";

        mockMvc.perform(post("/diet")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addRecord_noAuth_returns401() throws Exception {
        String body = "{\"recordDate\":\"2026-05-12\",\"mealType\":\"lunch\"," +
                "\"details\":[{\"foodId\":" + foodId + ",\"amount\":200}]}";

        mockMvc.perform(post("/diet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // ========== updateRecord ==========

    @Test
    void updateRecord_success() throws Exception {
        // Create a record first
        String createBody = "{\"recordDate\":\"2026-05-12\",\"mealType\":\"lunch\"," +
                "\"details\":[{\"foodId\":" + foodId + ",\"amount\":200}]}";

        String createResult = mockMvc.perform(post("/diet")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn().getResponse().getContentAsString();

        // Find the created record
        DietRecord record = dietRecordMapper.selectList(null).get(0);

        String updateBody = "{\"id\":" + record.getId() + ",\"recordDate\":\"2026-05-12\",\"mealType\":\"dinner\"," +
                "\"details\":[{\"foodId\":" + foodId + ",\"amount\":150}]}";

        mockMvc.perform(put("/diet")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void updateRecord_crossUser_forbidden() throws Exception {
        // Create record as user1
        String createBody = "{\"recordDate\":\"2026-05-12\",\"mealType\":\"lunch\"," +
                "\"details\":[{\"foodId\":" + foodId + ",\"amount\":200}]}";

        mockMvc.perform(post("/diet")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn().getResponse().getContentAsString();

        DietRecord record = dietRecordMapper.selectList(null).get(0);

        // Try to update as user2
        String updateBody = "{\"id\":" + record.getId() + ",\"recordDate\":\"2026-05-12\",\"mealType\":\"dinner\"," +
                "\"details\":[{\"foodId\":" + foodId + ",\"amount\":150}]}";

        mockMvc.perform(put("/diet")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isForbidden());
    }

    // ========== deleteRecord ==========

    @Test
    void deleteRecord_success() throws Exception {
        // Create a record
        String createBody = "{\"recordDate\":\"2026-05-12\",\"mealType\":\"lunch\"," +
                "\"details\":[{\"foodId\":" + foodId + ",\"amount\":200}]}";

        mockMvc.perform(post("/diet")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody));

        DietRecord record = dietRecordMapper.selectList(null).get(0);

        mockMvc.perform(delete("/diet/" + record.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deleteRecord_crossUser_forbidden() throws Exception {
        // Create record as user1
        String createBody = "{\"recordDate\":\"2026-05-12\",\"mealType\":\"lunch\"," +
                "\"details\":[{\"foodId\":" + foodId + ",\"amount\":200}]}";

        mockMvc.perform(post("/diet")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody));

        DietRecord record = dietRecordMapper.selectList(null).get(0);

        // Try to delete as user2
        mockMvc.perform(delete("/diet/" + record.getId())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteRecord_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/diet/999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ========== listByDate ==========

    @Test
    void listByDate_empty() throws Exception {
        mockMvc.perform(get("/diet/list")
                        .header("Authorization", "Bearer " + token)
                        .param("date", "2026-05-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ========== dailyStats ==========

    @Test
    void dailyStats_empty() throws Exception {
        mockMvc.perform(get("/diet/daily-stats")
                        .header("Authorization", "Bearer " + token)
                        .param("date", "2026-05-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalCalories").value(0));
    }

    // ========== weeklyStats ==========

    @Test
    void weeklyStats_returnsDatesAndCalories() throws Exception {
        mockMvc.perform(get("/diet/weekly-stats")
                        .header("Authorization", "Bearer " + token)
                        .param("start", "2026-05-10")
                        .param("end", "2026-05-16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.dates").isArray())
                .andExpect(jsonPath("$.data.calories").isArray())
                .andExpect(jsonPath("$.data.dates").isArray());
    }

    @Test
    void weeklyStats_nullDates_usesDefaults() throws Exception {
        mockMvc.perform(get("/diet/weekly-stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.dates").isArray());
    }

    // ========== monthlyStats ==========

    @Test
    void monthlyStats_returnsDaysOfMonth() throws Exception {
        mockMvc.perform(get("/diet/monthly-stats")
                        .header("Authorization", "Bearer " + token)
                        .param("year", "2026")
                        .param("month", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.dates").isArray())
                .andExpect(jsonPath("$.data.calories").isArray())
                .andExpect(jsonPath("$.data.avgCalories").exists());
    }

    // ========== nutritionGap ==========

    @Test
    void nutritionGap_withDefaultTargets() throws Exception {
        mockMvc.perform(get("/diet/nutrition-gap")
                        .header("Authorization", "Bearer " + token)
                        .param("date", "2026-05-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.gaps").isArray())
                .andExpect(jsonPath("$.data.gaps[0].nutrient").value("热量"));
    }

    // ========== recommend ==========

    @Test
    void recommend_noRecords_returnsList() throws Exception {
        mockMvc.perform(get("/diet/recommend")
                        .header("Authorization", "Bearer " + token)
                        .param("mealType", "lunch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ========== recentFoods ==========

    @Test
    void recentFoods_empty() throws Exception {
        mockMvc.perform(get("/diet/recent-foods")
                        .header("Authorization", "Bearer " + token)
                        .param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }
}
