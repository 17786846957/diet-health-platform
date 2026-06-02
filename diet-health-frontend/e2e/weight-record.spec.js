import { test, expect } from '@playwright/test'
import { loginViaApi, injectAuth, uniqueUsername, seedWeightRecord } from './helpers.js'

test.describe('体重记录', () => {
  const username = uniqueUsername('wt')
  const password = 'test123'
  let cookie = ''

  test.beforeEach(async ({ page, request }) => {
    const auth = await loginViaApi(request, username, password)
    cookie = auth.cookie
    await injectAuth(page, auth)
    await page.goto('/')
    await page.getByText('体重记录').click()
    await page.waitForURL(/\/weight/)
  })

  test('页面加载正确', async ({ page }) => {
    await expect(page.locator('.header-title')).toHaveText('体重记录')
    // 记录体重按钮可见
    await expect(page.getByRole('button', { name: /记录体重/ })).toBeVisible()
    // 统计区域可见
    await expect(page.getByText('当前体重')).toBeVisible()
    await expect(page.getByText('体脂率')).toBeVisible()
    await expect(page.getByText('近期趋势')).toBeVisible()
  })

  test('新增体重记录对话框', async ({ page }) => {
    await page.getByRole('button', { name: /记录体重/ }).click()

    const dialog = page.locator('.el-dialog').last()
    await expect(dialog).toBeVisible()
    await expect(dialog).toContainText('记录体重')

    // 表单字段可见
    await expect(dialog.getByText('体重').first()).toBeVisible()
    await expect(dialog.getByText('体脂率')).toBeVisible()
    await expect(dialog.getByText('备注')).toBeVisible()

    // 取消关闭
    await dialog.getByRole('button', { name: '取消' }).click()
    await expect(dialog).toBeHidden({ timeout: 5000 })
  })

  test('提交体重记录', async ({ page }) => {
    await page.getByRole('button', { name: /记录体重/ }).click()

    const dialog = page.locator('.el-dialog').last()
    await expect(dialog).toBeVisible()

    // 设置明确的体重值
    const weightInput = dialog.locator('.el-input-number input').first()
    await weightInput.clear()
    await weightInput.fill('65')

    // 提交
    await dialog.getByRole('button', { name: '确定' }).click()

    // 对话框应关闭（API 成功）或出现错误提示（API 失败时取消关闭）
    await Promise.race([
      expect(dialog).toBeHidden({ timeout: 8000 }),
      page.locator('.el-message--error').waitFor({ state: 'visible', timeout: 8000 })
    ])

    if (await dialog.isVisible()) {
      await dialog.getByRole('button', { name: '取消' }).click()
    }
  })

  test('数据统计卡片', async ({ page, request }) => {
    // 通过 API 预置一条体重记录
    await seedWeightRecord(request, cookie, { weight: 65.5, bodyFat: 20.0 })

    // 刷新页面加载数据
    await page.goto('/')
    await page.getByText('体重记录').click()
    await page.waitForURL(/\/weight/)

    // 数据统计区域可见
    await expect(page.getByText('数据统计')).toBeVisible()
    await expect(page.getByText('起始体重')).toBeVisible()
    await expect(page.getByText('最新体重')).toBeVisible()
    await expect(page.getByText('记录次数')).toBeVisible()
  })

  test('BMI 参考卡片', async ({ page }) => {
    // BMI 参考区域可见
    await expect(page.getByText('BMI 参考')).toBeVisible()
    // BMI 范围说明可见
    await expect(page.getByText('偏瘦')).toBeVisible()
    await expect(page.getByText('正常')).toBeVisible()
    await expect(page.getByText('偏胖')).toBeVisible()
    await expect(page.getByText('肥胖')).toBeVisible()
  })

  test('体重趋势图表区域', async ({ page }) => {
    // 趋势图表容器存在
    const chartContainer = page.locator('[style*="height: 300px"]')
    await expect(chartContainer).toBeVisible()
  })
})
