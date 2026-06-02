import { test, expect } from '@playwright/test'
import { loginViaApi, injectAuth, uniqueUsername } from './helpers.js'

test.describe('个人中心', () => {
  const username = uniqueUsername('profile')
  const password = 'test123'

  test.beforeEach(async ({ page, request }) => {
    const auth = await loginViaApi(request, username, password)
    await injectAuth(page, auth)
    await page.goto('/')
    await page.getByText('个人信息', { exact: true }).click()
    await page.waitForURL(/\/profile/)
  })

  test('页面加载正确', async ({ page }) => {
    await expect(page.locator('.header-title')).toHaveText('个人中心')
    // 用户名字段可见且禁用
    await expect(page.locator('input[disabled]').first()).toBeVisible()
  })

  test('显示个人信息表单', async ({ page }) => {
    // 邮箱字段可见（通过 label 定位）
    await expect(page.getByText('邮箱')).toBeVisible()
    // 性别选择可见
    await expect(page.getByText('男')).toBeVisible()
    await expect(page.getByText('女')).toBeVisible()
  })

  test('更新个人信息', async ({ page }) => {
    // 填写邮箱（通过 label 定位输入框）
    const emailInput = page.getByText('邮箱').locator('..').locator('input')
    await emailInput.clear()
    await emailInput.fill(`${username}@test.com`)

    // 填写年龄
    const ageInput = page.locator('.el-input-number input').first()
    if (await ageInput.isVisible()) {
      await ageInput.clear()
      await ageInput.fill('25')
    }

    // 保存
    await page.getByRole('button', { name: /保存/ }).or(page.getByRole('button', { name: /更新/ })).click()

    // 验证成功提示
    await expect(page.getByText('保存成功').or(page.getByText('更新成功')).or(page.getByText('成功'))).toBeVisible({ timeout: 5000 })
  })

  test('每日热量推荐卡片可见', async ({ page }) => {
    // 热量推荐区域可见
    const calorieCard = page.getByText('每日热量推荐').or(page.getByText('目标热量')).or(page.getByText('kcal'))
    await expect(calorieCard.first()).toBeVisible()
  })

  test('家庭成员管理区域可见', async ({ page }) => {
    await expect(page.getByText('家庭成员管理')).toBeVisible()
    await expect(page.getByRole('button', { name: /添加成员/ })).toBeVisible()
  })
})
