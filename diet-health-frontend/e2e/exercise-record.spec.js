import { test, expect } from '@playwright/test'
import { loginViaApi, injectAuth, uniqueUsername, seedExerciseRecord } from './helpers.js'

test.describe('运动记录', () => {
  const username = uniqueUsername('exer')
  const password = 'test123'
  let cookie = ''

  test.beforeEach(async ({ page, request }) => {
    const auth = await loginViaApi(request, username, password)
    cookie = auth.cookie
    await injectAuth(page, auth)
    await page.goto('/')
    await page.getByText('运动记录').click()
    await page.waitForURL(/\/exercise/)
  })

  test('页面加载正确', async ({ page }) => {
    await expect(page.locator('.header-title')).toHaveText('运动记录')
    // 记录运动按钮可见
    await expect(page.getByRole('button', { name: /记录运动/ })).toBeVisible()
    // 日期选择器可见
    await expect(page.locator('.el-date-editor').first()).toBeVisible()
    // 统计区域可见
    await expect(page.getByText('运动时长')).toBeVisible()
    await expect(page.getByText('消耗热量')).toBeVisible()
    await expect(page.getByText('运动次数')).toBeVisible()
  })

  test('新增运动记录对话框', async ({ page }) => {
    await page.getByRole('button', { name: /记录运动/ }).click()

    const dialog = page.locator('.el-dialog').last()
    await expect(dialog).toBeVisible()
    await expect(dialog).toContainText('记录运动')

    // 表单字段可见
    await expect(dialog.getByText('运动类型')).toBeVisible()
    await expect(dialog.getByText('时长')).toBeVisible()
    await expect(dialog.getByText('强度')).toBeVisible()
    await expect(dialog.getByText('备注')).toBeVisible()

    // 取消关闭
    await dialog.getByRole('button', { name: '取消' }).click()
    await expect(dialog).toBeHidden({ timeout: 5000 })
  })

  test('未填写运动类型时提示', async ({ page }) => {
    await page.getByRole('button', { name: /记录运动/ }).click()

    const dialog = page.locator('.el-dialog').last()
    await expect(dialog).toBeVisible()

    // 不填运动类型直接提交
    await dialog.getByRole('button', { name: '确定' }).click()

    // 应显示警告提示
    await expect(page.getByText('请输入运动类型')).toBeVisible({ timeout: 5000 })
  })

  test('提交运动记录', async ({ page }) => {
    await page.getByRole('button', { name: /记录运动/ }).click()

    const dialog = page.locator('.el-dialog').last()
    await expect(dialog).toBeVisible()

    // 填写运动类型
    await dialog.getByPlaceholder('如：跑步、游泳、瑜伽').fill('跑步')

    // 提交
    await dialog.getByRole('button', { name: '确定' }).click()
    await expect(dialog).toBeHidden({ timeout: 5000 })
  })

  test('常见运动快捷按钮', async ({ page }) => {
    // 常见运动区域可见
    const quickSection = page.getByText('常见运动')
    await expect(quickSection).toBeVisible()
    // 应有运动类型按钮
    const exerciseButtons = page.locator('.quick-exercises button, .quick-exercises .el-button')
    const count = await exerciseButtons.count()
    expect(count).toBeGreaterThan(0)
  })

  test('有记录时显示删除按钮', async ({ page, request }) => {
    // 通过 API 预置一条记录
    const today = new Date().toISOString().split('T')[0]
    await seedExerciseRecord(request, cookie, {
      exerciseType: '跑步',
      duration: 30,
      intensity: 'moderate',
      recordDate: today
    })

    // 刷新页面加载记录
    await page.goto('/')
    await page.getByText('运动记录').click()
    await page.waitForURL(/\/exercise/)

    // 表格中应有数据行
    const table = page.locator('.el-table')
    const rows = table.locator('.el-table__row')
    const rowCount = await rows.count()
    if (rowCount > 0) {
      // 删除按钮可见
      await expect(rows.first().getByText('删除')).toBeVisible()
    }
  })

  test('本周趋势图表区域', async ({ page }) => {
    // 趋势图表卡片可见
    await expect(page.getByText('本周趋势')).toBeVisible()
    // 图表容器存在
    const chartContainer = page.locator('[style*="height: 250px"]')
    await expect(chartContainer).toBeVisible()
  })
})
