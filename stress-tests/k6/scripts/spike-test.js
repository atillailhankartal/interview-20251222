import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Counter, Trend } from 'k6/metrics';

// Custom metrics
const spikeErrors = new Rate('spike_errors');
const requestsHandled = new Counter('requests_handled');
const responseTime = new Trend('response_time');

// Configuration
const ORDER_SERVICE_URL = __ENV.ORDER_SERVICE_URL || 'http://localhost:8081';
const ASSET_SERVICE_URL = __ENV.ASSET_SERVICE_URL || 'http://localhost:8082';
const KEYCLOAK_URL = __ENV.KEYCLOAK_URL || 'http://localhost:8180';

export const options = {
    scenarios: {
        // Sudden spike test
        sudden_spike: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: 10 },    // Warm up
                { duration: '10s', target: 10 },    // Steady state
                { duration: '5s', target: 300 },    // SPIKE! Sudden jump to 300 users
                { duration: '30s', target: 300 },   // Hold spike
                { duration: '10s', target: 10 },    // Quick recovery check
                { duration: '20s', target: 10 },    // Verify system stability
                { duration: '10s', target: 0 },     // Ramp down
            ],
        },
    },
    thresholds: {
        // During spike, allow higher latency but still reasonable
        http_req_duration: ['p(95)<3000', 'p(99)<5000'],
        http_req_failed: ['rate<0.2'],  // Allow up to 20% failures during spike
        spike_errors: ['rate<0.3'],      // 30% error rate threshold
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

const assets = ['AAPL', 'GOOG', 'MSFT', 'AMZN', 'TSLA'];

export function setup() {
    const token = getToken();
    return { token };
}

export default function (data) {
    const token = data.token || getToken();
    if (!token) {
        spikeErrors.add(1);
        return;
    }

    const headers = {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
    };

    const customerId = testCustomerIds[Math.floor(Math.random() * testCustomerIds.length)];
    const asset = assets[Math.floor(Math.random() * assets.length)];

    // Perform a mix of operations
    const operation = Math.random();

    const startTime = Date.now();
    let success = false;

    if (operation < 0.4) {
        // 40% - Create order
        const orderPayload = JSON.stringify({
            customerId: customerId,
            assetSymbol: asset,
            orderSide: Math.random() > 0.5 ? 'BUY' : 'SELL',
            orderType: 'LIMIT',
            price: (100 + Math.random() * 100).toFixed(2),
            size: (1 + Math.floor(Math.random() * 50)).toString(),
        });

        const response = http.post(
            `${ORDER_SERVICE_URL}/api/orders`,
            orderPayload,
            { headers: headers, timeout: '10s' }
        );

        success = check(response, {
            'order created': (r) => r.status === 201 || r.status === 200,
        });
    } else if (operation < 0.7) {
        // 30% - List orders
        const response = http.get(
            `${ORDER_SERVICE_URL}/api/orders?customerId=${customerId}&page=0&size=20`,
            { headers: headers, timeout: '10s' }
        );

        success = check(response, {
            'orders listed': (r) => r.status === 200,
        });
    } else {
        // 30% - Check assets
        const response = http.get(
            `${ASSET_SERVICE_URL}/api/assets?customerId=${customerId}`,
            { headers: headers, timeout: '10s' }
        );

        success = check(response, {
            'assets retrieved': (r) => r.status === 200,
        });
    }

    const endTime = Date.now();
    responseTime.add(endTime - startTime);

    if (success) {
        spikeErrors.add(0);
        requestsHandled.add(1);
    } else {
        spikeErrors.add(1);
    }

    // Minimal sleep during spike to maximize pressure
    sleep(Math.random() * 0.5);
}

export function handleSummary(data) {
    return {
        'stdout': textSummary(data),
        'reports/spike-test-summary.json': JSON.stringify(data),
    };
}

function textSummary(data) {
    let summary = '\n' + '='.repeat(70) + '\n';
    summary += 'SPIKE TEST SUMMARY\n';
    summary += '='.repeat(70) + '\n\n';

    summary += 'PERFORMANCE UNDER SPIKE:\n';
    summary += `-  Total Requests: ${data.metrics.http_reqs.values.count}\n`;
    summary += `-  Requests Handled Successfully: ${data.metrics.requests_handled ? data.metrics.requests_handled.values.count : 0}\n`;
    summary += `-  Request Duration (avg): ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms\n`;
    summary += `-  Request Duration (p95): ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms\n`;
    summary += `-  Request Duration (p99): ${data.metrics.http_req_duration.values['p(99)'].toFixed(2)}ms\n`;
    summary += `-  Request Duration (max): ${data.metrics.http_req_duration.values.max.toFixed(2)}ms\n\n`;

    summary += 'ERROR RATES:\n';
    summary += `-  HTTP Failed Requests: ${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)}%\n`;
    summary += `-  Spike Error Rate: ${(data.metrics.spike_errors.values.rate * 100).toFixed(2)}%\n\n`;

    // Determine pass/fail
    const p95Duration = data.metrics.http_req_duration.values['p(95)'];
    const failRate = data.metrics.http_req_failed.values.rate;

    summary += 'VERDICT:\n';
    if (p95Duration < 3000 && failRate < 0.2) {
        summary += '✓ PASSED - System handled spike gracefully\n';
    } else {
        summary += '✗ FAILED - System struggled under spike load\n';
        if (p95Duration >= 3000) {
            summary += `  - P95 latency exceeded threshold (${p95Duration.toFixed(0)}ms > 3000ms)\n`;
        }
        if (failRate >= 0.2) {
            summary += `  - Error rate exceeded threshold (${(failRate * 100).toFixed(1)}% > 20%)\n`;
        }
    }

    summary += '\n' + '='.repeat(70) + '\n';
    return summary;
}
