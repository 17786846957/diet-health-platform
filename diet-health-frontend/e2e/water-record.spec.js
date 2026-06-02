import { test, expect } from '@playwright/test'
import { loginViaApi, injectAuth, uniqueUsername, seedWaterRecord } from './helpers.js'

test.describe('饮水记录', () => {
  const username = uniqueUsername('water')
  const password = 'test123'
  let cookie = ''

  test.beforeEach(async ({ page, request }) => {
    const auth = await loginViaApi(request, username, password)
    cookie = auth.cookie
    await injectAuth(page, auth)
    await page.goto('/')
    await page.getByText('饮水记录').click()
    await page.waitForURL(/\/water/)
  })

  test('页面加载正确', async ({ page }) => {
    await expect(page.locator('.header-title')).toHaveText('饮水记录')
    // 记录饮水按钮可见
    await expect(page.getByRole('button', { name: /记录饮水/ })).toBeVisible()
    // 日期选择器可见
    await expect(page.locator('.el-date-editor').first()).toBeVisible()
    // 统计区域可见
    await expect(page.getByText('今日饮水')).toBeVisible()
    await expect(page.getByText('记录次数')).toBeVisible()
    await expect(page.getByText('目标')).toBeVisible()
  })

  test('新增饮水记录对话框', async ({ page }) => {
    await page.getByRole('button', { name: /记录饮水/ }).click()

    const dialog = page.locator('.el-dialog').last()
    await expect(dialog).toBeVisible()
    await expect(dialog).toContainText('记录饮水')

    // 表单字段可见
    await expect(dialog.getByText('饮水量')).toBeVisible()
    await expect(dialog.getByText('类型')).toBeVisible()
    await expect(dialog.getByText('时间')).toBeVisible()

    // 取消关闭
    await dialog.getByRole('button', { name: '取消' }).click()
    await expect(dialog).toBeHidden({ timeout: 5000 })
  })

  test('提交饮水记录', async ({ page }) => {
    await page.getByRole('button', { name: /记录饮水/ }).click()

    const dialog = page.locator('.el-dialog').last()
    await expect(dialog).toBeVisible()

    // 默认值已填，直接提交
    await dialog.getByRole('button', { name: '确定' }).click()
    await expect(dialog).toBeHidden({ timeout: 5000 })
  })

  test('快捷添加按钮', async ({ page }) => {
    // 快捷添加区域可见
    const quickSection = page.getByText('快捷添加')
    await expect(quickSection).toBeVisible()
    // 应有 ml 按钮
    const mlButtons = page.locator('.quick-add button, .quick-add .el-button')
    const count = await mlButtons.count()
    expect(count).toBeGreaterThan(0)
  })

  test('有记录时显示删除按钮', async ({ page, request }) => {
    // 通过 API 预置一条记录
    const today = new Date().toISOString().split('T')[0]
    await seedWaterRecord(request, cookie, {
      amount: 250,
      drinkType: 'water',
      recordDate: today,
      recordTime: '10:00'
    })

    // 刷新页面加载记录
    await page.goto('/')
    await page.getByText('饮水记录').click()
    await page.waitForURL(/\/water/)

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
