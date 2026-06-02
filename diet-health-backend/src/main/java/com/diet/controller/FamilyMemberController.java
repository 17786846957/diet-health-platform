package com.diet.controller;

import com.diet.common.R;
import com.diet.dto.FamilyMemberRequest;
import com.diet.entity.FamilyMember;
import com.diet.service.FamilyMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "家庭成员管理", description = "家庭成员档案的增删改查")
@RestController
@RequestMapping("/family-members")
public class FamilyMemberController {

    private final FamilyMemberService familyMemberService;

    public FamilyMemberController(FamilyMemberService familyMemberService) {
        this.familyMemberService = familyMemberService;
    }

    @Operation(summary = "获取成员列表", description = "获取当前用户的所有家庭成员")
    @GetMapping
    public R<List<FamilyMember>> list(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return R.ok(familyMemberService.listByUserId(userId));
    }

    @Operation(summary = "获取成员详情", description = "根据 ID 获取单个家庭成员信息")
    @GetMapping("/{id}")
    public R<FamilyMember> getById(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return R.ok(familyMemberService.getByIdAndCheck(id, userId));
    }

    @Operation(summary = "创建成员", description = "为当前用户创建新的家庭成员（每用户最多10人）")
    @PostMapping
    public R<FamilyMember> create(@Valid @RequestBody FamilyMemberRequest request, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        FamilyMember member = new FamilyMember();
        member.setUserId(userId);
        member.setName(request.getName());
        member.setGender(request.getGender());
        member.setAge(request.getAge());
        member.setHeight(request.getHeight());
        member.setWeight(request.getWeight());
        member.setActivityLevel(request.getActivityLevel());
        member.setGoal(request.getGoal());
        member.setAvatar(request.getAvatar());
        return R.ok(familyMemberService.create(member));
    }

    @Operation(summary = "更新成员", description = "更新家庭成员信息")
    @PutMapping("/{id}")
    public R<?> update(@PathVariable Long id, @Valid @RequestBody FamilyMemberRequest request, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        FamilyMember member = new FamilyMember();
        member.setId(id);
        member.setName(request.getName());
        member.setGender(request.getGender());
        member.setAge(request.getAge());
        member.setHeight(request.getHeight());
        member.setWeight(request.getWeight());
        member.setActivityLevel(request.getActivityLevel());
        member.setGoal(request.getGoal());
        member.setAvatar(request.getAvatar());
        familyMemberService.update(member, userId);
        return R.ok("更新成功", null);
    }

    @Operation(summary = "删除成员", description = "删除家庭成员（其饮食记录将变为主用户记录）")
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        familyMemberService.delete(id, userId);
        return R.ok("删除成功", null);
    }
}
