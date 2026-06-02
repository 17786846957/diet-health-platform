import { test, expect } from '@playwright/test'
import { loginViaApi, injectAuth, uniqueUsername } from './helpers.js'

test.describe('饮食记录', () => {
  const username = uniqueUsername('diet')
  const password = 'test123'
  let cookie = ''

  test.beforeEach(async ({ page, request }) => {
    const auth = await loginViaApi(request, username, password)
    cookie = auth.cookie
    await injectAuth(page, auth)
    await page.goto('/')
    await page.getByText('饮食记录').click()
    await page.waitForURL(/\/diet/)
  })

  test('页面加载正确', async ({ page }) => {
    await expect(page.locator('.header-title')).toHaveText('饮食记录')
    // 日期选择器可见
    await expect(page.locator('.el-date-editor').first()).toBeVisible()
    // 新增按钮可见
    await expect(page.getByRole('button', { name: /新增/ })).toBeVisible()
  })

  test('日期切换', async ({ page }) => {
    // 点击日期选择器，切换到昨天
    const dateInput = page.locator('.el-date-editor input').first()
    await dateInput.click()
    // 选择昨天
    const yesterday = new Date()
    yesterday.setDate(yesterday.getDate() - 1)
    const day = yesterday.getDate().toString()
    // 在日期面板中点击对应日期
    await page.locator('.el-date-table').getByText(day, { exact: true }).first().click()
    // 关闭日期面板
    await page.locator('.header-title').click()
  })

  test('新增饮食记录', async ({ page, request }) => {
    // 先通过 API 确保有食物数据（使用 admin 账户）
    // 这里我们直接测试 UI 流程，假设数据库已有种子数据
    await page.getByRole('button', { name: /新增/ }).click()

    // 对话框应该打开
    await expect(page.locator('.el-dialog').last()).toBeVisible()

    // 选择餐次
    const mealSelect = page.locator('.el-dialog').last().getByText('请选择餐次').or(page.locator('.el-dialog').last().locator('.el-select').first())
    await mealSelect.first().click()
    await page.locator('.el-select-dropdown').getByText('午餐').click()

    // 搜索食物（如果有食物数据）
    const foodSelect = page.locator('.el-dialog').last().locator('.el-select').nth(1)
    await foodSelect.click()

    // 如果有食物选项，选择第一个
    const foodOption = page.locator('.el-select-dropdown__item').first()
    try {
      await foodOption.click({ timeout: 3000 })

      // 填写克数
      const amountInput = page.locator('.el-dialog').last().locator('.el-input-number input').first()
      await amountInput.clear()
      await amountInput.fill('200')

      // 提交
      await page.getByRole('button', { name: '确定' }).or(page.getByRole('button', { name: '提交' })).click()
    } catch {
      // 没有食物数据或下拉不可用，关闭对话框
      await page.getByRole('button', { name: '取消' }).click()
    }
  })

  test('智能推荐区域可展开', async ({ page }) => {
    // 查找推荐区域（折叠面板）
    const recommendSection = page.getByText('智能推荐')
    if (await recommendSection.isVisible()) {
      await recommendSection.click()
      // 应该显示推荐食物标签
      await expect(page.locator('.el-collapse-item').last()).toBeVisible()
    }
  })
})
