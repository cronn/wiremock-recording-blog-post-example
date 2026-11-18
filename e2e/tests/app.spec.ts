import { test } from '@playwright/test'
import { expect } from '../fixtures'

test('neue Trinkwasserstellen in Bonn', async ({ page }) => {
  await page.goto('/')
  await page.getByRole('button', { name: 'Suchen' }).click()
  await expect(page.getByRole('status')).toContainText('gefunden')

  const table = page.getByRole('table')
  await expect(table).toMatchMarkdownTableSnapshotFile()
})
