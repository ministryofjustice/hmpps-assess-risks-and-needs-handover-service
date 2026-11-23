const { test, expect } = require('@playwright/test')

test.describe('Create session and generate sentence plan link', () => {
  test('captures the CRN, starts a session, and verifies the sentence-plan page', async ({ page }) => {
    await page.goto('/training-session-launcher/browse?scenario=default')

    // 1. Find the <dt> containing "CRN" and get the sibling <dd> content
    const crnValue = page.locator('dt', { hasText: 'CRN' }).first().locator('xpath=following-sibling::dd[1]')
    const crn = (await crnValue.textContent()).trim()
    expect(crn).not.toBe('')

    // 2. Click "Start session" and wait for the alert heading
    await page.getByRole('button', { name: 'Start session' }).click()

    const alertHeading = page.locator('h2.moj-alert__heading')
    await expect(alertHeading).toBeVisible()
    await expect(alertHeading).toHaveText('Session created') // toHaveText normalises whitespace

    // 3. Click "Generate link" and verify the URL and CRN on the new page
    await page.getByRole('button', { name: 'Generate link' }).click()

    await expect(page).toHaveURL((url) => url.pathname.replace(/^\/+/, '').split('/')[0] === 'sentence-plan')

    await expect(
      page.locator('dd').filter({ hasText: new RegExp(`^\\s*${crn.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s*$`) }),
      `expected a <dd> element with CRN "${crn}"`,
    ).not.toHaveCount(0)
  })
})
