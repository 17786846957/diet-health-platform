import { test, expect } from '@playwright/test'
import { loginViaApi, injectAuth, uniqueUsername } from './helpers.js'

test.describe('路由与导航', () => {
  const username = uniqueUsername('nav')
  const password = 'test123'

  test.beforeEach(async ({ page, request }) => {
    const auth = await loginViaApi(request, username, password)
    await injectAuth(page, auth)
    await page.goto('/')
  })

  test('侧边栏导航到饮食记录', async ({ page }) => {
    await page.getByText('饮食记录').click()
    await expect(page).toHaveURL(/\/diet/)
    await expect(page.locator('.header-title')).toHaveText('饮食记录')
  })

  test('侧边栏导航到食物库', async ({ page }) => {
    await page.getByText('食物库', { exact: true }).click()
    await expect(page).toHaveURL(/\/food/)
    await expect(page.locator('.header-title')).toHaveText('食物库')
  })

  test('侧边栏导航到营养统计', async ({ page }) => {
    await page.getByText('营养统计').click()
    await expect(page).toHaveURL(/\/stats/)
    await expect(page.locator('.header-title')).toHaveText('营养统计')
  })

  test('侧边栏导航到个人信息', async ({ page }) => {
    await page.getByText('个人信息', { exact: true }).click()
    await expect(page).toHaveURL(/\/profile/)
    await expect(page.locator('.header-title')).toHaveText('个人中心')
  })

  test('侧边栏导航回首页', async ({ page }) => {
    await page.getByText('饮食记录').click()
    await page.getByText('首页概览').click()
    await expect(page).toHaveURL('/')
    await expect(page.locator('.header-title')).toHaveText('首页')
  })

  test('404 页面', async ({ page }) => {
    await page.goto('/nonexistent-page')
    await expect(page.getByText('404')).toBeVisible()
  })

  test('页面标题正确', async ({ page }) => {
    await expect(page).toHaveTitle(/首页/)
    await page.getByText('饮食记录').click()
    await expect(page).toHaveTitle(/饮食记录/)
  })
})
