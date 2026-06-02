package com.diet.controller;

import com.diet.entity.User;
import com.diet.mapper.FamilyMemberMapper;
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
class FamilyMemberControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private FamilyMemberMapper familyMemberMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    private String token;
    private String otherToken;
    private Long userId;

    @BeforeEach
    void setUp() {
        familyMemberMapper.delete(null);
        userMapper.delete(null);

        User user = new User();
        user.setUsername("membertest");
        user.setPassword(passwordEncoder.encode("123456"));
        user.setRole("user");
        user.setGender("male");
        user.setAge(25);
        user.setHeight(170.0);
        user.setWeight(65.0);
        userMapper.insert(user);
        userId = user.getId();
        token = jwtUtil.generateToken(userId, "membertest", "user");

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

    @Test
    void listMembers_empty() throws Exception {
        mockMvc.perform(get("/family-members")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void createMember_success() throws Exception {
        String body = "{\"name\":\"test\",\"gender\":\"male\",\"age\":30," +
                "\"height\":175,\"weight\":70,\"activityLevel\":\"moderate\",\"goal\":\"maintain\"}";

        mockMvc.perform(post("/family-members")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("test"))
                .andExpect(jsonPath("$.data.userId").value(userId));
    }

    @Test
    void createMember_blankName_returns400() throws Exception {
        String body = "{\"name\":\"\",\"gender\":\"male\"}";

        mockMvc.perform(post("/family-members")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateMember_success() throws Exception {
        // Create
        String createBody = "{\"name\":\"old\",\"gender\":\"male\",\"age\":20}";
        String createResult = mockMvc.perform(post("/family-members")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn().getResponse().getContentAsString();
        Long memberId = objectMapper.readTree(createResult).get("data").get("id").asLong();

        // Update
        String updateBody = "{\"name\":\"new\",\"gender\":\"female\",\"age\":25}";
        mockMvc.perform(put("/family-members/" + memberId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void updateMember_crossUser_forbidden() throws Exception {
        // Create as user1
        String createBody = "{\"name\":\"test\",\"gender\":\"male\",\"age\":20}";
        String createResult = mockMvc.perform(post("/family-members")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn().getResponse().getContentAsString();
        Long memberId = objectMapper.readTree(createResult).get("data").get("id").asLong();

        // Try to update as user2
        String updateBody = "{\"name\":\"hacked\",\"gender\":\"female\",\"age\":99}";
        mockMvc.perform(put("/family-members/" + memberId)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateMember_notFound_returns404() throws Exception {
        String updateBody = "{\"name\":\"test\",\"gender\":\"male\"}";
        mockMvc.perform(put("/family-members/999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteMember_success() throws Exception {
        // Create
        String createBody = "{\"name\":\"todelete\",\"gender\":\"male\"}";
        String createResult = mockMvc.perform(post("/family-members")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn().getResponse().getContentAsString();
        Long memberId = objectMapper.readTree(createResult).get("data").get("id").asLong();

        // Delete
        mockMvc.perform(delete("/family-members/" + memberId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Verify deleted
        mockMvc.perform(get("/family-members")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void deleteMember_crossUser_forbidden() throws Exception {
        // Create as user1
        String createBody = "{\"name\":\"test\",\"gender\":\"male\"}";
        String createResult = mockMvc.perform(post("/family-members")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn().getResponse().getContentAsString();
        Long memberId = objectMapper.readTree(createResult).get("data").get("id").asLong();

        // Try to delete as user2
        mockMvc.perform(delete("/family-members/" + memberId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteMember_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/family-members/999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void createMember_noAuth_returns401() throws Exception {
        String body = "{\"name\":\"test\",\"gender\":\"male\"}";

        mockMvc.perform(post("/family-members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }
}
