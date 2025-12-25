import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Counter, Trend } from 'k6/metrics';

// Custom metrics
const depositErrors = new Rate('deposit_errors');
const withdrawErrors = new Rate('withdraw_errors');
const assetQueryErrors = new Rate('asset_query_errors');
const depositDuration = new Trend('deposit_duration');
const withdrawDuration = new Trend('withdraw_duration');

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8082';
const KEYCLOAK_URL = __ENV.KEYCLOAK_URL || 'http://localhost:8180';

export const options = {
    stages: [
        { duration: '20s', target: 20 },   // Ramp up
        { duration: '1m', target: 50 },    // Steady load
        { duration: '30s', target: 100 },  // Peak load
        { duration: '1m', target: 50 },    // Cool down
        { duration: '20s', target: 0 },    // Ramp down
    ],
    thresholds: {
        http_req_duration: ['p(95)<800', 'p(99)<1500'],
        http_req_failed: ['rate<0.05'],
        deposit_errors: ['rate<0.1'],
        withdraw_errors: ['rate<0.1'],
    },
};

function getToken() {
    const tokenResponse = http.post(
        `${KEYCLOAK_URL}/realms/brokage/protocol/openid-connect/token`,
        {
            grant_type: 'password',
            client_id: 'brokage-api',
            username: 'admin@brokage.com',
            password: 'admin123',
        },
        {
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        }
    );

    if (tokenResponse.status === 200) {
        const body = JSON.parse(tokenResponse.body);
        return body.access_token;
    }
    return null;
}

const testCustomerIds = [
    'c0000000-0000-0000-0000-000000000001',
    'c0000000-0000-0000-0000-000000000002',
    'c0000000-0000-0000-0000-000000000003',
];

export function setup() {
    const token = getToken();
    return { token };
}

export default function (data) {
    const token = data.token || getToken();
    if (!token) {
        depositErrors.add(1);
        return;
    }

    const headers = {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
    };

    const customerId = testCustomerIds[Math.floor(Math.random() * testCustomerIds.length)];

    group('Asset Operations', function () {
        // Get Assets
        group('Query Assets', function () {
            const getAssetsResponse = http.get(
                `${BASE_URL}/api/assets?customerId=${customerId}`,
                { headers: headers }
            );

            const querySuccess = check(getAssetsResponse, {
                'get assets (200)': (r) => r.status === 200,
                'has assets data': (r) => {
                    if (r.status === 200) {
                        const body = JSON.parse(r.body);
                        return body.data !== undefined;
                    }
                    return false;
                },
            });

            if (!querySuccess) {
                assetQueryErrors.add(1);
            } else {
                assetQueryErrors.add(0);
            }
        });

        sleep(0.5);

        // Deposit TRY
        group('Deposit', function () {
            const depositAmount = (1000 + Math.random() * 9000).toFixed(2);
            const depositPayload = JSON.stringify({
                customerId: customerId,
                assetSymbol: 'TRY',
                amount: depositAmount,
            });

            const startTime = Date.now();
            const depositResponse = http.post(
                `${BASE_URL}/api/assets/deposit`,
                depositPayload,
                { headers: headers }
            );
            depositDuration.add(Date.now() - startTime);

            const depositSuccess = check(depositResponse, {
                'deposit (200)': (r) => r.status === 200,
                'deposit success': (r) => {
                    if (r.status === 200) {
                        const body = JSON.parse(r.body);
                        return body.success === true;
                    }
                    return false;
                },
            });

            if (!depositSuccess) {
                depositErrors.add(1);
                console.error(`Deposit failed: ${depositResponse.status}`);
            } else {
                depositErrors.add(0);
            }
        });

        sleep(0.5);

        // Withdraw TRY (smaller amount)
        group('Withdraw', function () {
            const withdrawAmount = (100 + Math.random() * 500).toFixed(2);
            const withdrawPayload = JSON.stringify({
                customerId: customerId,
                assetSymbol: 'TRY',
                amount: withdrawAmount,
            });

            const startTime = Date.now();
            const withdrawResponse = http.post(
                `${BASE_URL}/api/assets/withdraw`,
                withdrawPayload,
                { headers: headers }
            );
            withdrawDuration.add(Date.now() - startTime);

            const withdrawSuccess = check(withdrawResponse, {
                'withdraw (200)': (r) => r.status === 200 || r.status === 400,  // 400 for insufficient funds is ok
            });

            if (!withdrawSuccess) {
                withdrawErrors.add(1);
            } else {
                withdrawErrors.add(0);
            }
        });
    });

    sleep(Math.random() * 1.5);
}

export function handleSummary(data) {
    return {
        'stdout': textSummary(data),
        'reports/asset-operations-summary.json': JSON.stringify(data),
    };
}

function textSummary(data) {
    let summary = '\n' + '='.repeat(60) + '\n';
    summary += 'ASSET OPERATIONS STRESS TEST SUMMARY\n';
    summary += '='.repeat(60) + '\n\n';

    summary += `Total Requests: ${data.metrics.http_reqs.values.count}\n`;
    summary += `Request Duration (avg): ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms\n`;
    summary += `Request Duration (p95): ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms\n`;
    summary += `Failed Requests: ${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)}%\n`;
    summary += `Deposit Error Rate: ${(data.metrics.deposit_errors.values.rate * 100).toFixed(2)}%\n`;
    summary += `Withdraw Error Rate: ${(data.metrics.withdraw_errors.values.rate * 100).toFixed(2)}%\n`;

    if (data.metrics.deposit_duration) {
        summary += `Deposit Duration (avg): ${data.metrics.deposit_duration.values.avg.toFixed(2)}ms\n`;
    }

    summary += '\n' + '='.repeat(60) + '\n';
    return summary;
}
