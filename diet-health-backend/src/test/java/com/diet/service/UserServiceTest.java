package com.diet.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diet.common.BusinessException;
import com.diet.entity.FamilyMember;
import com.diet.entity.User;
import com.diet.mapper.FamilyMemberMapper;
import com.diet.mapper.UserMapper;
import com.diet.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private FamilyMemberMapper familyMemberMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void register_success() {
        User user = new User();
        user.setUsername("newuser");
        user.setPassword("123456");

        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("123456")).thenReturn("encoded");
        when(userMapper.insert(any())).thenReturn(1);

        assertDoesNotThrow(() -> userService.register(user));
        assertEquals("encoded", user.getPassword());
        assertEquals("user", user.getRole());
        verify(userMapper).insert(user);
    }

    @Test
    void register_duplicateUsername_throws() {
        User user = new User();
        user.setUsername("existing");
        user.setPassword("123456");

        when(userMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.register(user));
        assertEquals(1001, ex.getCode());
    }

    @Test
    void login_success() {
        User user = new User();
        user.setId(1L);
        user.setUsername("test");
        user.setPassword("encoded");
        user.setRole("user");

        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("123456", "encoded")).thenReturn(true);
        when(jwtUtil.generateToken(1L, "test", "user")).thenReturn("token");

        var result = userService.login("test", "123456");
        assertEquals("token", result.get("token"));
        assertEquals(user, result.get("user"));
    }

    @Test
    void login_wrongPassword_throws() {
        User user = new User();
        user.setId(1L);
        user.setUsername("test");
        user.setPassword("encoded");
        user.setRole("user");

        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> userService.login("test", "wrong"));
    }

    @Test
    void login_userNotFound_throws() {
        when(userMapper.selectOne(any())).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> userService.login("nouser", "123456"));
    }

    @Test
    void updateProfile_notFound_throws() {
        when(userMapper.selectById(999L)).thenReturn(null);

        User user = new User();
        user.setId(999L);

        assertThrows(BusinessException.class,
                () -> userService.updateProfile(user));
    }

    @Test
    void updateProfile_success() {
        User existing = new User();
        existing.setId(1L);
        existing.setEmail("old@test.com");

        when(userMapper.selectById(1L)).thenReturn(existing);
        when(userMapper.updateById(any())).thenReturn(1);

        User update = new User();
        update.setId(1L);
        update.setEmail("new@test.com");
        update.setAge(25);
        update.setGender("male");
        update.setHeight(175.0);
        update.setWeight(70.0);
        update.setActivityLevel("moderate");
        update.setGoal("maintain");

        assertDoesNotThrow(() -> userService.updateProfile(update));
        assertEquals("new@test.com", existing.getEmail());
        assertEquals(25, existing.getAge());
        assertEquals("male", existing.getGender());
        assertEquals(175.0, existing.getHeight());
        verify(userMapper).updateById(existing);
    }

    @Test
    void deleteUser_notFound_throws() {
        when(userMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> userService.deleteUser(999L));
    }

    @Test
    void deleteUser_success() {
        User user = new User();
        user.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.deleteById(1L)).thenReturn(1);

        assertDoesNotThrow(() -> userService.deleteUser(1L));
        verify(userMapper).deleteById(1L);
    }

    @Test
    void listUsers_noFilters_returnsPage() {
        Page<User> page = new Page<>(1, 20);
        when(userMapper.selectPage(any(), any())).thenReturn(page);

        IPage<User> result = userService.listUsers(1, 20, null, null);
        assertNotNull(result);
        verify(userMapper).selectPage(any(), any());
    }

    @Test
    void listUsers_withKeywordAndRole_queriesCorrectly() {
        Page<User> page = new Page<>(1, 20);
        when(userMapper.selectPage(any(), any())).thenReturn(page);

        IPage<User> result = userService.listUsers(1, 20, "test", "admin");
        assertNotNull(result);
        verify(userMapper).selectPage(any(), any());
    }

    @Test
    void calculateTargets_nullUser_returnsDefaults() {
        when(userMapper.selectById(999L)).thenReturn(null);

        double[] targets = userService.calculateTargets(999L);
        assertEquals(2000, targets[0]);
        assertEquals(60, targets[1]);
        assertEquals(55, targets[2]);
        assertEquals(300, targets[3]);
    }

    @Test
    void calculateTargets_incompleteInfo_returnsDefaults() {
        User user = new User();
        user.setId(1L);
        // height, weight, age all null
        when(userMapper.selectById(1L)).thenReturn(user);

        double[] targets = userService.calculateTargets(1L);
        assertEquals(2000, targets[0]);
    }

    @Test
    void calculateTargets_maleUser_calculatesCorrectly() {
        User user = new User();
        user.setId(1L);
        user.setGender("male");
        user.setAge(25);
        user.setHeight(170.0);
        user.setWeight(65.0);
        user.setActivityLevel("moderate");
        user.setGoal("maintain");
        when(userMapper.selectById(1L)).thenReturn(user);

        double[] targets = userService.calculateTargets(1L);
        // BMR = 88.362 + 13.397*65 + 4.799*170 - 5.677*25 = 1663.877
        // TDEE = 1663.877 * 1.55 = 2579.0
        assertTrue(targets[0] > 2000 && targets[0] < 3000);
        assertTrue(targets[1] > 0); // protein
        assertTrue(targets[2] > 0); // fat
        assertTrue(targets[3] > 0); // carbs
    }

    @Test
    void calculateTargets_loseGoal_subtracts500() {
        User user = new User();
        user.setId(1L);
        user.setGender("male");
        user.setAge(25);
        user.setHeight(170.0);
        user.setWeight(65.0);
        user.setActivityLevel("moderate");
        user.setGoal("lose");
        when(userMapper.selectById(1L)).thenReturn(user);

        double[] targetsLose = userService.calculateTargets(1L);

        User userMaintain = new User();
        userMaintain.setId(2L);
        userMaintain.setGender("male");
        userMaintain.setAge(25);
        userMaintain.setHeight(170.0);
        userMaintain.setWeight(65.0);
        userMaintain.setActivityLevel("moderate");
        userMaintain.setGoal("maintain");
        when(userMapper.selectById(2L)).thenReturn(userMaintain);

        double[] targetsMaintain = userService.calculateTargets(2L);

        assertEquals(500.0, targetsMaintain[0] - targetsLose[0], 1.0);
    }

    @Test
    void calculateTargets_femaleUser_usesFemaleFormula() {
        User user = new User();
        user.setId(1L);
        user.setGender("female");
        user.setAge(30);
        user.setHeight(160.0);
        user.setWeight(55.0);
        user.setActivityLevel("light");
        user.setGoal("maintain");
        when(userMapper.selectById(1L)).thenReturn(user);

        double[] targets = userService.calculateTargets(1L);
        assertTrue(targets[0] > 1000 && targets[0] < 2500);
    }

    @Test
    void calculateTargetsForMember_memberNotFound_returnsDefaults() {
        when(familyMemberMapper.selectById(999L)).thenReturn(null);

        double[] targets = userService.calculateTargetsForMember(999L);
        assertEquals(2000, targets[0]);
    }

    @Test
    void calculateTargetsForMember_validMember_calculatesCorrectly() {
        FamilyMember member = new FamilyMember();
        member.setId(1L);
        member.setGender("male");
        member.setAge(10);
        member.setHeight(140.0);
        member.setWeight(35.0);
        member.setActivityLevel("active");
        member.setGoal("gain");
        when(familyMemberMapper.selectById(1L)).thenReturn(member);

        double[] targets = userService.calculateTargetsForMember(1L);
        assertTrue(targets[0] > 1500);
    }
}
