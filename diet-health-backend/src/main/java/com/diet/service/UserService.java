package com.diet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diet.entity.FamilyMember;
import com.diet.entity.User;
import com.diet.mapper.FamilyMemberMapper;
import com.diet.mapper.UserMapper;
import com.diet.common.BusinessException;
import com.diet.common.ResultCode;
import com.diet.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final FamilyMemberMapper familyMemberMapper;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil, FamilyMemberMapper familyMemberMapper) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.familyMemberMapper = familyMemberMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public void register(User user) {
        log.info("用户注册: username={}", user.getUsername());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, user.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            log.warn("注册失败: 用户名已存在, username={}", user.getUsername());
            throw new BusinessException(ResultCode.USER_EXISTS);
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("user");
        userMapper.insert(user);
        log.info("注册成功: userId={}, username={}", user.getId(), user.getUsername());
    }

    public Map<String, Object> login(String username, String password) {
        log.info("用户登录: username={}", username);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            log.warn("登录失败: 用户名或密码错误, username={}", username);
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        log.info("登录成功: userId={}, username={}, role={}", user.getId(), user.getUsername(), user.getRole());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);
        return result;
    }

    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(User user) {
        log.info("更新个人信息: userId={}", user.getId());
        User existing = userMapper.selectById(user.getId());
        if (existing == null) {
            log.warn("更新失败: 用户不存在, userId={}", user.getId());
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        existing.setEmail(user.getEmail());
        existing.setGender(user.getGender());
        existing.setAge(user.getAge());
        existing.setHeight(user.getHeight());
        existing.setWeight(user.getWeight());
        existing.setActivityLevel(user.getActivityLevel());
        existing.setGoal(user.getGoal());
        userMapper.updateById(existing);
        log.info("个人信息更新成功: userId={}", user.getId());
    }

    public long count() {
        return userMapper.selectCount(null);
    }

    public IPage<User> listUsers(int page, int size, String keyword, String role) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(User::getUsername, keyword);
        }
        if (role != null && !role.isBlank()) {
            wrapper.eq(User::getRole, role);
        }
        wrapper.orderByDesc(User::getCreateTime);
        return userMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        if (userMapper.selectById(id) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        userMapper.deleteById(id);
    }

    public double[] calculateTargets(Long userId) {
        log.debug("计算用户营养目标: userId={}", userId);
        User user = userMapper.selectById(userId);
        if (user == null || user.getHeight() == null
                || user.getWeight() == null || user.getAge() == null) {
            log.debug("用户信息不完整，返回默认目标: userId={}", userId);
            return DEFAULT_TARGETS;
        }
        return buildTargets(user.getGender(), user.getAge(), user.getHeight(),
                user.getWeight(), user.getActivityLevel(), user.getGoal());
    }

    public double[] calculateTargetsForMember(Long memberId) {
        FamilyMember member = familyMemberMapper.selectById(memberId);
        if (member == null || member.getHeight() == null
                || member.getWeight() == null || member.getAge() == null) {
            return DEFAULT_TARGETS;
        }
        return buildTargets(member.getGender(), member.getAge(), member.getHeight(),
                member.getWeight(), member.getActivityLevel(), member.getGoal());
    }

    private static final double[] DEFAULT_TARGETS = new double[]{2000, 60, 55, 300};

    private static final Map<String, Double> ACTIVITY_MULTIPLIERS = Map.of(
            "sedentary", 1.2, "light", 1.375, "moderate", 1.55,
            "active", 1.725, "very_active", 1.9
    );

    private static final Map<String, Double> GOAL_ADJUSTMENTS = Map.of(
            "lose", -500.0, "maintain", 0.0, "gain", 300.0
    );

    private double[] buildTargets(String gender, Integer age, Double height,
                                  Double weight, String activityLevel, String goal) {
        double targetCalories = calculateTargetCalories(gender, age, height, weight, activityLevel, goal);
        double targetProtein = targetCalories * 0.15 / 4.0;
        double targetFat = targetCalories * 0.25 / 9.0;
        double targetCarbs = targetCalories * 0.60 / 4.0;
        return new double[]{
            Math.round(targetCalories * 10) / 10.0,
            Math.round(targetProtein * 10) / 10.0,
            Math.round(targetFat * 10) / 10.0,
            Math.round(targetCarbs * 10) / 10.0
        };
    }

    private double calculateTargetCalories(String gender, Integer age,
            Double height, Double weight, String activityLevel, String goal) {
        double w = weight != null ? weight : 65.0;
        double h = height != null ? height : 170.0;
        int a = age != null ? age : 25;
        double bmr;
        if ("male".equals(gender)) {
            bmr = 88.362 + 13.397 * w + 4.799 * h - 5.677 * a;
        } else {
            bmr = 447.593 + 9.247 * w + 3.098 * h - 4.330 * a;
        }
        String level = activityLevel != null ? activityLevel : "moderate";
        double multiplier = ACTIVITY_MULTIPLIERS.getOrDefault(level, 1.55);
        String g = goal != null ? goal : "maintain";
        double adjustment = GOAL_ADJUSTMENTS.getOrDefault(g, 0.0);
        return bmr * multiplier + adjustment;
    }
}
