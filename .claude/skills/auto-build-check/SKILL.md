---
name: auto-build-check
description: "自动编译检查 hook 配置。为 PostToolUse 添加编译/构建检查，每次编辑 Java 或 Vue 文件后自动验证编译是否通过。适用于：配置自动检查、添加编译 hook、防止编译错误积累。当用户说'配置自动检查'、'添加编译 hook'、'每次改完自动编译'时触发。"
---

# Auto Build Check — 自动编译检查

通过 PostToolUse hook 实现编辑后自动编译检查。

## 实现方式

在 `.claude/settings.json` 的 `hooks.PostToolUse` 中添加编译检查：

### 后端（Java 文件编辑后）
当 matcher 匹配 `Write|Edit` 且文件路径包含 `.java` 时：
```bash
cd D:/bishe2/diet-health-backend && mvn compile -q 2>&1 | tail -5
```

### 前端（Vue/JS 文件编辑后）
当 matcher 匹配 `Write|Edit` 且文件路径包含 `.vue` 或 `.js` 时：
```bash
cd D:/bishe2/diet-health-frontend && npm run build 2>&1 | tail -5
```

## Hook 配置模板

在 `.claude/settings.json` 的 `hooks.PostToolUse` 数组中添加：

```json
{
  "matcher": "Write|Edit",
  "hooks": [
    {
      "type": "command",
      "command": "file=\"$TOOL_INPUT_FILE_PATH\"; if echo \"$file\" | grep -q '\\.java$'; then cd D:/bishe2/diet-health-backend && mvn compile -q 2>&1 | tail -3; elif echo \"$file\" | grep -qE '\\.(vue|js)$'; then cd D:/bishe2/diet-health-frontend && npm run build 2>&1 | tail -3; fi"
    }
  ]
}
```

## 注意事项
- hook 超时默认 60s，Maven 编译可能需要更长时间
- 如果 hook 超时，可改用增量编译或仅检查语法
- hook 失败不会阻塞主流程，仅输出警告
