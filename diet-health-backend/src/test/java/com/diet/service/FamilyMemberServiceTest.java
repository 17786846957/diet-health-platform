package com.diet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diet.common.BusinessException;
import com.diet.entity.FamilyMember;
import com.diet.mapper.FamilyMemberMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FamilyMemberServiceTest {

    @Mock
    private FamilyMemberMapper familyMemberMapper;

    @InjectMocks
    private FamilyMemberService familyMemberService;

    @Test
    void listByUserId_returnsMembers() {
        FamilyMember m1 = new FamilyMember();
        m1.setId(1L);
        m1.setUserId(100L);
        m1.setName("爸爸");
        FamilyMember m2 = new FamilyMember();
        m2.setId(2L);
        m2.setUserId(100L);
        m2.setName("妈妈");

        when(familyMemberMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(m1, m2));

        List<FamilyMember> result = familyMemberService.listByUserId(100L);

        assertEquals(2, result.size());
        assertEquals("爸爸", result.get(0).getName());
    }

    @Test
    void listByUserId_emptyList() {
        when(familyMemberMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        List<FamilyMember> result = familyMemberService.listByUserId(100L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getByIdAndCheck_success() {
        FamilyMember member = new FamilyMember();
        member.setId(1L);
        member.setUserId(100L);
        member.setName("爸爸");

        when(familyMemberMapper.selectById(1L)).thenReturn(member);

        FamilyMember result = familyMemberService.getByIdAndCheck(1L, 100L);

        assertEquals("爸爸", result.getName());
    }

    @Test
    void getByIdAndCheck_notFound_throwsException() {
        when(familyMemberMapper.selectById(99L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> familyMemberService.getByIdAndCheck(99L, 100L));
    }

    @Test
    void getByIdAndCheck_wrongUser_throwsException() {
        FamilyMember member = new FamilyMember();
        member.setId(1L);
        member.setUserId(100L);

        when(familyMemberMapper.selectById(1L)).thenReturn(member);

        assertThrows(BusinessException.class,
                () -> familyMemberService.getByIdAndCheck(1L, 200L));
    }

    @Test
    void create_success() {
        FamilyMember member = new FamilyMember();
        member.setUserId(100L);
        member.setName("孩子");

        when(familyMemberMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);
        when(familyMemberMapper.insert(any(FamilyMember.class))).thenReturn(1);

        FamilyMember result = familyMemberService.create(member);

        assertNotNull(result);
        verify(familyMemberMapper).insert(member);
    }

    @Test
    void create_atLimit_throwsException() {
        FamilyMember member = new FamilyMember();
        member.setUserId(100L);
        member.setName("第11个");

        when(familyMemberMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(10L);

        assertThrows(BusinessException.class, () -> familyMemberService.create(member));
        verify(familyMemberMapper, never()).insert(any());
    }

    @Test
    void update_success() {
        FamilyMember existing = new FamilyMember();
        existing.setId(1L);
        existing.setUserId(100L);
        existing.setName("旧名字");

        FamilyMember updated = new FamilyMember();
        updated.setId(1L);
        updated.setName("新名字");
        updated.setGender("female");
        updated.setAge(30);

        when(familyMemberMapper.selectById(1L)).thenReturn(existing);
        when(familyMemberMapper.updateById(any(FamilyMember.class))).thenReturn(1);

        assertDoesNotThrow(() -> familyMemberService.update(updated, 100L));
        assertEquals("新名字", existing.getName());
        assertEquals("female", existing.getGender());
        verify(familyMemberMapper).updateById(existing);
    }

    @Test
    void update_wrongUser_throwsException() {
        FamilyMember existing = new FamilyMember();
        existing.setId(1L);
        existing.setUserId(100L);

        FamilyMember updated = new FamilyMember();
        updated.setId(1L);

        when(familyMemberMapper.selectById(1L)).thenReturn(existing);

        assertThrows(BusinessException.class,
                () -> familyMemberService.update(updated, 200L));
        verify(familyMemberMapper, never()).updateById(any());
    }

    @Test
    void delete_success() {
        FamilyMember existing = new FamilyMember();
        existing.setId(1L);
        existing.setUserId(100L);

        when(familyMemberMapper.selectById(1L)).thenReturn(existing);

        assertDoesNotThrow(() -> familyMemberService.delete(1L, 100L));
        verify(familyMemberMapper).deleteById(1L);
    }

    @Test
    void delete_notFound_throwsException() {
        when(familyMemberMapper.selectById(99L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> familyMemberService.delete(99L, 100L));
        verify(familyMemberMapper, never()).deleteById(anyLong());
    }
}
