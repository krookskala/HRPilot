import { test, expect } from '@playwright/test';

test.describe('Login Page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
  });

  test('should display login form', async ({ page }) => {
    await expect(page.locator('input[formControlName="email"]')).toBeVisible();
    await expect(page.locator('input[formControlName="password"]')).toBeVisible();
    await expect(page.getByRole('button', { name: /sign in/i })).toBeVisible();
  });

  test('should show validation errors for empty fields', async ({ page }) => {
    await page.getByRole('button', { name: /sign in/i }).click();
    await expect(page.locator('mat-error')).toHaveCount(2);
  });

  test('should navigate to forgot password', async ({ page }) => {
    await page.getByText(/forgot your password/i).click();
    await expect(page).toHaveURL(/forgot-password/);
  });
});
