package com.diet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diet.common.BusinessException;
import com.diet.common.ResultCode;
import com.diet.entity.FamilyMember;
import com.diet.mapper.FamilyMemberMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class FamilyMemberService {

    private final FamilyMemberMapper familyMemberMapper;

    public FamilyMemberService(FamilyMemberMapper familyMemberMapper) {
        this.familyMemberMapper = familyMemberMapper;
    }

    public List<FamilyMember> listByUserId(Long userId) {
        log.debug("查询家庭成员列表: userId={}", userId);
        LambdaQueryWrapper<FamilyMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FamilyMember::getUserId, userId)
               .orderByDesc(FamilyMember::getCreateTime);
        return familyMemberMapper.selectList(wrapper);
    }

    public FamilyMember getByIdAndCheck(Long id, Long userId) {
        log.debug("查询家庭成员: memberId={}, userId={}", id, userId);
        FamilyMember member = familyMemberMapper.selectById(id);
        if (member == null) {
            log.warn("家庭成员不存在: memberId={}", id);
            throw new BusinessException(ResultCode.MEMBER_NOT_FOUND);
        }
        if (!member.getUserId().equals(userId)) {
            log.warn("无权操作该家庭成员: memberId={}, requestUserId={}, ownerUserId={}", id, userId, member.getUserId());
            throw new BusinessException(ResultCode.MEMBER_FORBIDDEN);
        }
        return member;
    }

    @Transactional(rollbackFor = Exception.class)
    public FamilyMember create(FamilyMember member) {
        log.info("创建家庭成员: userId={}, name={}", member.getUserId(), member.getName());
        LambdaQueryWrapper<FamilyMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FamilyMember::getUserId, member.getUserId());
        long count = familyMemberMapper.selectCount(wrapper);
        if (count >= 10) {
            log.warn("创建失败: 已达上限, userId={}, currentCount={}", member.getUserId(), count);
            throw new BusinessException(ResultCode.BAD_REQUEST, "每个账户最多添加10个家庭成员");
        }
        familyMemberMapper.insert(member);
        log.info("家庭成员创建成功: memberId={}, name={}", member.getId(), member.getName());
        return member;
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(FamilyMember member, Long userId) {
        log.info("更新家庭成员: memberId={}, userId={}", member.getId(), userId);
        FamilyMember existing = getByIdAndCheck(member.getId(), userId);
        existing.setName(member.getName());
        existing.setGender(member.getGender());
        existing.setAge(member.getAge());
        existing.setHeight(member.getHeight());
        existing.setWeight(member.getWeight());
        existing.setActivityLevel(member.getActivityLevel());
        existing.setGoal(member.getGoal());
        existing.setAvatar(member.getAvatar());
        familyMemberMapper.updateById(existing);
        log.info("家庭成员更新成功: memberId={}", member.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long userId) {
        log.info("删除家庭成员: memberId={}, userId={}", id, userId);
        getByIdAndCheck(id, userId);
        familyMemberMapper.deleteById(id);
        log.info("家庭成员删除成功: memberId={}", id);
    }
}
