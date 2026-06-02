const API_BASE = 'http://localhost:8082/api'

/**
 * 通过 UI 注册用户
 */
export async function registerUser(page, username, password, email = '') {
  await page.goto('/register')
  await page.getByPlaceholder('请输入用户名').fill(username)
  await page.getByPlaceholder('请输入密码').first().fill(password)
  await page.getByPlaceholder('请再次输入密码').fill(password)
  if (email) {
    await page.getByPlaceholder('请输入邮箱').fill(email)
  }
  await page.getByRole('button', { name: '注 册' }).click()
  await page.waitForURL('/login')
}

/**
 * 通过 UI 登录（等待 loading 遮罩消失）
 */
export async function loginUser(page, username, password) {
  await page.goto('/login')
  await page.getByPlaceholder('用户名').fill(username)
  await page.getByPlaceholder('密码').fill(password)
  await page.getByRole('button', { name: '登 录' }).click()
  await page.waitForURL('/')
  // 等待 loading 遮罩消失，避免阻塞后续交互
  await page.locator('.el-loading-mask').waitFor({ state: 'hidden', timeout: 5000 }).catch(() => {})
}

/**
 * 通过 API 注册 + 登录（更快，用于 setup）
 * 返回 { cookie, user }，不操作页面
 */
export async function loginViaApi(request, username, password) {
  // 注册（忽略"用户名已存在"，可能是之前测试留下的）
  const regRes = await request.post(`${API_BASE}/auth/register`, {
    data: { username, password },
  })
  const regBody = await regRes.json()
  if (regBody.code !== 200 && !regBody.message?.includes('已存在')) {
    throw new Error(`注册失败: ${regBody.message}`)
  }

  // 登录
  const res = await request.post(`${API_BASE}/auth/login`, {
    data: { username, password },
  })
  const body = await res.json()
  if (body.code !== 200) {
    throw new Error(`登录失败: ${body.message}`)
  }

  const cookie = extractCookie(res)
  const user = body.data?.user
  return { response: res, cookie, user }
}

/**
 * 将 API 登录态注入浏览器（cookie + localStorage）
 * 调用后页面会认为用户已登录
 */
export async function injectAuth(page, { cookie, user }) {
  if (cookie) {
    await page.context().addCookies([{
      name: 'diet_token',
      value: cookie.replace('diet_token=', ''),
      domain: 'localhost',
      path: '/',
      httpOnly: true,
      sameSite: 'Lax',
    }])
  }
  // 导航到同源页面以设置 localStorage
  await page.goto('/login')
  await page.evaluate((userData) => {
    localStorage.setItem('user', JSON.stringify(userData))
  }, user)
}

/**
 * 通过 API 创建记录（通用）
 */
async function seedRecord(request, cookie, endpoint, data) {
  await request.post(`${API_BASE}/${endpoint}`, {
    headers: { Cookie: cookie },
    data,
  })
}

export const seedFood = (request, cookie, data) => seedRecord(request, cookie, 'food', data)
export const seedDietRecord = (request, cookie, data) => seedRecord(request, cookie, 'diet', data)
export const seedWaterRecord = (request, cookie, data) => seedRecord(request, cookie, 'water', data)
export const seedExerciseRecord = (request, cookie, data) => seedRecord(request, cookie, 'exercise', data)
export const seedWeightRecord = (request, cookie, data) => seedRecord(request, cookie, 'weight', data)

/**
 * 生成唯一用户名（不超过20字符，适配后端 @Size(max=20)）
 */
export function uniqueUsername(prefix = 'e') {
  const ts = Date.now().toString(36).slice(-6)
  const rand = Math.random().toString(36).slice(2, 4)
  return `${prefix}${ts}${rand}`
}

/**
 * 从 API 响应中提取 diet_token cookie
 */
export function extractCookie(response) {
  const cookies = response.headers()['set-cookie'] || ''
  const match = cookies.match(/diet_token=([^;]+)/)
  return match ? `diet_token=${match[1]}` : ''
}
