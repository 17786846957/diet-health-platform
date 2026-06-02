import { test, expect } from '@playwright/test'
import { loginUser, loginViaApi, injectAuth, uniqueUsername } from './helpers.js'

test.describe('认证流程', () => {
  test('注册新用户成功', async ({ page }) => {
    const username = uniqueUsername('reg')
    await page.goto('/register')
    await page.getByPlaceholder('请输入用户名').fill(username)
    await page.getByPlaceholder('请输入密码').first().fill('test123')
    await page.getByPlaceholder('请再次输入密码').fill('test123')
    await page.getByRole('button', { name: '注 册' }).click()
    await page.waitForURL('/login')
    await expect(page.getByPlaceholder('用户名')).toBeVisible()
  })

  test('注册校验：空用户名', async ({ page }) => {
    await page.goto('/register')
    await page.getByPlaceholder('请输入密码').first().fill('test123')
    await page.getByPlaceholder('请再次输入密码').fill('test123')
    await page.getByRole('button', { name: '注 册' }).click()
    await expect(page.getByText('请输入用户名')).toBeVisible()
  })

  test('注册校验：密码不含字母', async ({ page }) => {
    await page.goto('/register')
    await page.getByPlaceholder('请输入用户名').fill(uniqueUsername('reg'))
    await page.getByPlaceholder('请输入密码').first().fill('123456')
    await page.getByPlaceholder('请再次输入密码').fill('123456')
    await page.getByPlaceholder('请输入密码').first().blur()
    await expect(page.getByText('密码必须包含字母和数字')).toBeVisible()
  })

  test('注册校验：密码不一致', async ({ page }) => {
    await page.goto('/register')
    await page.getByPlaceholder('请输入用户名').fill(uniqueUsername('reg'))
    await page.getByPlaceholder('请输入密码').first().fill('test123')
    await page.getByPlaceholder('请再次输入密码').fill('test456')
    await page.getByPlaceholder('请再次输入密码').blur()
    await expect(page.getByText('两次输入的密码不一致')).toBeVisible()
  })

  test('登录成功', async ({ page, request }) => {
    const username = uniqueUsername('login')
    const password = 'test123'
    await loginViaApi(request, username, password)
    await loginUser(page, username, password)
    await expect(page.locator('.header-user')).toHaveText(username)
  })

  test('登录失败：错误密码', async ({ page, request }) => {
    const username = uniqueUsername('login')
    const password = 'test123'
    await loginViaApi(request, username, password)
    await page.goto('/login')
    await page.getByPlaceholder('用户名').fill(username)
    await page.getByPlaceholder('密码').fill('wrongpassword')
    await page.getByRole('button', { name: '登 录' }).click()
    await expect(page.getByText('用户名或密码错误')).toBeVisible({ timeout: 10000 })
  })

  test('退出登录', async ({ page, request }) => {
    const username = uniqueUsername('logout')
    const password = 'test123'
    await loginViaApi(request, username, password)
    await loginUser(page, username, password)
    await page.getByRole('button', { name: '退出登录' }).click()
    await page.waitForURL('/login')
    await expect(page.getByPlaceholder('用户名')).toBeVisible()
  })

  test('未登录访问受保护页 → 跳转 /login', async ({ page }) => {
    await page.goto('/')
    await page.waitForURL(/\/login/)
    await expect(page.getByPlaceholder('用户名')).toBeVisible()
  })

  test('已登录访问 /login → 跳转 /', async ({ page, request }) => {
    const username = uniqueUsername('guard')
    const password = 'test123'
    const auth = await loginViaApi(request, username, password)
    await injectAuth(page, auth)
    await page.goto('/login')
    await page.waitForURL('/')
    await expect(page.locator('.header-user')).toHaveText(username)
  })
})
