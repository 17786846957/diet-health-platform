import { test, expect } from '@playwright/test'
import { loginViaApi, injectAuth, uniqueUsername } from './helpers.js'

test.describe('家庭成员管理', () => {
  const username = uniqueUsername('fam')
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
    // 家庭成员管理区域可见
    await expect(page.getByText('家庭成员管理')).toBeVisible()
  })

  test('添加家庭成员', async ({ page }) => {
    const memberName = uniqueUsername('member')

    // 点击添加成员按钮
    await page.getByRole('button', { name: /添加成员/ }).click()

    // 对话框打开
    const dialog = page.locator('.el-dialog').last()
    await expect(dialog).toBeVisible()

    // 填写成员信息
    await dialog.getByPlaceholder('请输入姓名').or(dialog.getByPlaceholder('请输入成员姓名')).fill(memberName)

    // 选择性别
    const genderRadio = dialog.locator('.el-radio').first()
    await genderRadio.click()

    // 填写年龄
    const ageInput = dialog.getByPlaceholder('请输入年龄').or(dialog.locator('.el-input-number input').first())
    if (await ageInput.isVisible()) {
      await ageInput.fill('30')
    }

    // 提交
    await dialog.getByRole('button', { name: '确定' }).or(dialog.getByRole('button', { name: '保存' })).click()

    // 等待对话框关闭
    await expect(dialog).toBeHidden({ timeout: 5000 })

    // 验证新成员出现在表格中
    await expect(page.locator('.el-table').getByText(memberName)).toBeVisible()
  })

  test('编辑家庭成员', async ({ page }) => {
    // 先添加一个成员
    const memberName = uniqueUsername('edit')
    await page.getByRole('button', { name: /添加成员/ }).click()
    const dialog = page.locator('.el-dialog').last()
    await dialog.getByPlaceholder('请输入姓名').or(dialog.getByPlaceholder('请输入成员姓名')).fill(memberName)
    await dialog.locator('.el-radio').first().click()
    await dialog.getByRole('button', { name: '确定' }).or(dialog.getByRole('button', { name: '保存' })).click()
    await expect(dialog).toBeHidden({ timeout: 5000 })

    // 点击编辑按钮（同一行）
    const row = page.locator('.el-table__row').filter({ hasText: memberName })
    await row.getByRole('button', { name: /编辑/ }).first().click()

    // 修改名称
    const editDialog = page.locator('.el-dialog').last()
    await expect(editDialog).toBeVisible()
    const nameInput = editDialog.getByPlaceholder('请输入姓名').or(editDialog.getByPlaceholder('请输入成员姓名'))
    await nameInput.clear()
    await nameInput.fill(memberName + '_updated')

    // 保存
    await editDialog.getByRole('button', { name: '确定' }).or(editDialog.getByRole('button', { name: '保存' })).click()
    await expect(editDialog).toBeHidden({ timeout: 5000 })

    // 验证更新
    await expect(page.locator('.el-table').getByText(memberName + '_updated')).toBeVisible()
  })

  test('删除家庭成员', async ({ page }) => {
    // 先添加一个成员
    const memberName = uniqueUsername('del')
    await page.getByRole('button', { name: /添加成员/ }).click()
    const dialog = page.locator('.el-dialog').last()
    await dialog.getByPlaceholder('请输入姓名').or(dialog.getByPlaceholder('请输入成员姓名')).fill(memberName)
    await dialog.locator('.el-radio').first().click()
    await dialog.getByRole('button', { name: '确定' }).or(dialog.getByRole('button', { name: '保存' })).click()
    await expect(dialog).toBeHidden({ timeout: 5000 })

    // 点击删除按钮
    const row = page.locator('.el-table__row').filter({ hasText: memberName })
    await row.getByRole('button', { name: /删除/ }).first().click()

    // 确认删除（el-popconfirm）
    const confirmBtn = page.getByRole('button', { name: '确定' })
    if (await confirmBtn.isVisible()) {
      await confirmBtn.click()
    }

    // 验证成员消失
    await expect(page.locator('.el-table').getByText(memberName)).toBeHidden({ timeout: 5000 })
  })
})
