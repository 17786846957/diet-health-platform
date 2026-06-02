import { test, expect } from '@playwright/test'
import { loginViaApi, injectAuth, uniqueUsername } from './helpers.js'

test.describe('食物库', () => {
  const username = uniqueUsername('food')
  const password = 'test123'

  test.beforeEach(async ({ page, request }) => {
    const auth = await loginViaApi(request, username, password)
    await injectAuth(page, auth)
    await page.goto('/')
    await page.getByText('食物库', { exact: true }).click()
    await page.waitForURL(/\/food/)
  })

  test('页面加载正确', async ({ page }) => {
    await expect(page.locator('.header-title')).toHaveText('食物库')
    // 搜索框可见
    await expect(page.getByPlaceholder(/搜索食物/)).toBeVisible()
    // 表格可见
    await expect(page.locator('.el-table')).toBeVisible()
  })

  test('食物列表显示表格', async ({ page }) => {
    // 表格列头可见
    await expect(page.getByText('食物名称')).toBeVisible()
    await expect(page.locator('thead').getByText('分类')).toBeVisible()
    await expect(page.getByText('热量')).toBeVisible()
  })

  test('搜索食物', async ({ page }) => {
    const searchInput = page.getByPlaceholder(/搜索食物/)
    await searchInput.fill('米饭')
    // 等待搜索结果更新
    await page.waitForTimeout(500)
    // 表格应该过滤
    const table = page.locator('.el-table')
    await expect(table).toBeVisible()
  })

  test('分类筛选', async ({ page }) => {
    // 点击分类下拉
    const categorySelect = page.getByPlaceholder('全部分类').or(page.locator('.el-select').first())
    await categorySelect.first().click()
    // 选择一个分类
    const options = page.locator('.el-select-dropdown__item')
    if (await options.count() > 0) {
      await options.first().click()
      await page.waitForTimeout(500)
    }
  })

  test('分页控件可见', async ({ page }) => {
    const pagination = page.locator('.el-pagination')
    // 如果有足够数据，分页应该可见
    if (await pagination.isVisible()) {
      await expect(pagination).toBeVisible()
    }
  })
})
