import { test as base, expect } from '@playwright/test';

type AirportFixtures = {
    apiGuard: void;
};

export const test = base.extend<AirportFixtures>({
    apiGuard: [
        async ({ page }, use) => {
            const unexpectedRequests: string[] = [];

            await page.route('**/api/**', async route => {
                const request = route.request();

                unexpectedRequests.push(
                    `${request.method()} ${request.url()}`
                );

                await route.abort();
            });

            await use();

            expect(
                unexpectedRequests,
                'Il test ha effettuato chiamate API non simulate'
            ).toEqual([]);
        },
        { auto: true }
    ]
});

export { expect };