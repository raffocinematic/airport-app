import { test, expect } from '../fixture/airport-test';

test('naviga dalla login alla registrazione e ritorna', async ({ page }) => {
    await page.goto('/login');

    await expect(
        page.getByRole('heading', {
            name: 'Accedi ad Airport App',
            exact: true
        })
    ).toBeVisible();

    await page.getByRole('link', {
        name: 'Registrati',
        exact: true
    }).click();

    await expect(page).toHaveURL(/\/registrazione$/);

    await expect(
        page.getByRole('heading', {
            name: 'Crea il tuo account',
            exact: true
        })
    ).toBeVisible();

    await page.getByRole('link', {
        name: 'Accedi',
        exact: true
    }).click();

    await expect(page).toHaveURL(/\/login$/);

    await expect(
        page.getByRole('heading', {
            name: 'Accedi ad Airport App',
            exact: true
        })
    ).toBeVisible();
});