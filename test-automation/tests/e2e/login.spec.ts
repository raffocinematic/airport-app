import { test, expect } from '../fixture/airport-test';

test('mostra gli errori quando la login viene inviata vuota', async ({ page }) => {
    await page.goto('/login');

    await page.getByRole('button', {
        name: 'Accedi',
        exact: true
    }).click();

    await expect(page.getByRole('alert')).toHaveText([
        "Inserisci un'email valida, massimo 255 caratteri.",
        'Inserisci una password, massimo 255 caratteri.'
    ]);

    await expect(page).toHaveURL(/\/login$/);

    await expect(
        page.getByRole('button', {
            name: 'Accedi',
            exact: true
        })
    ).toBeEnabled();
});