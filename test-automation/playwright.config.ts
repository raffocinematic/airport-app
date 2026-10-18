import { defineConfig, devices } from '@playwright/test';
import { resolve } from 'node:path';

const appUrl = 'http://127.0.0.1:4300';

export default defineConfig({
    testDir: './tests/e2e',

    fullyParallel: false,
    workers: 1,
    retries: 0,

    timeout: 30_000,

    expect: {
        timeout: 5_000
    },

    outputDir: './test-results',

    reporter: [
        ['list'],
        ['html', { open: 'never' }]
    ],

    use: {
        baseURL: appUrl,
        trace: 'retain-on-failure',
        screenshot: 'only-on-failure'
    },

    projects: [
        {
            name: 'chromium',
            use: {
                ...devices['Desktop Chrome']
            }
        }
    ],

    webServer: {
        command: 'npm run start -- --host 127.0.0.1 --port 4300',
        cwd: resolve(__dirname, '../frontend'),
        url: appUrl,
        reuseExistingServer: false,
        timeout: 120_000
    }
});