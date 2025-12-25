import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Counter, Trend } from 'k6/metrics';
import { SharedArray } from 'k6/data';

// Custom metrics
const tradingErrors = new Rate('trading_errors');
const successfulTrades = new Counter('successful_trades');
const endToEndLatency = new Trend('e2e_latency');

// Configuration
const ORDER_SERVICE_URL = __ENV.ORDER_SERVICE_URL || 'http://localhost:8081';
const ASSET_SERVICE_URL = __ENV.ASSET_SERVICE_URL || 'http://localhost:8082';
const KEYCLOAK_URL = __ENV.KEYCLOAK_URL || 'http://localhost:8180';

export const options = {
    scenarios: {
        // Normal trading hours simulation
        normal_trading: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 20 },
                { duration: '2m', target: 50 },
                { duration: '1m', target: 50 },
                { duration: '30s', target: 0 },
            ],
            gracefulRampDown: '10s',
        },
        // Spike test - sudden increase in activity
        spike_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            startTime: '4m',
            stages: [
                { duration: '10s', target: 150 },
                { duration: '30s', target: 150 },
                { duration: '10s', target: 0 },
            ],
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<2000'],
        http_req_failed: ['rate<0.1'],
        trading_errors: ['rate<0.15'],
        e2e_latency: ['p(95)<5000'],
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
    'c0000000-0000-0000-0000-000000000004',
    'c0000000-0000-0000-0000-000000000005',
];

const assets = [
    { symbol: 'AAPL', minPrice: 150, maxPrice: 180 },
    { symbol: 'GOOG', minPrice: 130, maxPrice: 160 },
    { symbol: 'MSFT', minPrice: 350, maxPrice: 400 },
    { symbol: 'AMZN', minPrice: 140, maxPrice: 170 },
    { symbol: 'TSLA', minPrice: 200, maxPrice: 280 },
];

export function setup() {
    const token = getToken();
    return { token };
}

export default function (data) {
    const token = data.token || getToken();
    if (!token) {
        tradingErrors.add(1);
        return;
    }

    const headers = {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
    };

    const customerId = testCustomerIds[Math.floor(Math.random() * testCustomerIds.length)];
    const asset = assets[Math.floor(Math.random() * assets.length)];

    const startTime = Date.now();

    group('Full Trading Cycle', function () {
        // Step 1: Check balance
        group('Check Balance', function () {
            const balanceResponse = http.get(
                `${ASSET_SERVICE_URL}/api/assets?customerId=${customerId}`,
                { headers: headers }
            );

            check(balanceResponse, {
                'balance check (200)': (r) => r.status === 200,
            });
        });

        sleep(0.2);

        // Step 2: Create buy order
        let orderId = null;
        group('Create Order', function () {
            const price = (asset.minPrice + Math.random() * (asset.maxPrice - asset.minPrice)).toFixed(2);
            const size = (1 + Math.floor(Math.random() * 10)).toString();
            const orderSide = Math.random() > 0.5 ? 'BUY' : 'SELL';

            const orderPayload = JSON.stringify({
                customerId: customerId,
                assetSymbol: asset.symbol,
                orderSide: orderSide,
                orderType: 'LIMIT',
                price: price,
                size: size,
            });

            const orderResponse = http.post(
                `${ORDER_SERVICE_URL}/api/orders`,
                orderPayload,
                { headers: headers }
            );

            const orderSuccess = check(orderResponse, {
                'order created': (r) => r.status === 201 || r.status === 200,
            });

            if (orderSuccess && (orderResponse.status === 201 || orderResponse.status === 200)) {
                const orderData = JSON.parse(orderResponse.body);
                orderId = orderData.data?.id;
                successfulTrades.add(1);
            } else {
                tradingErrors.add(1);
            }
        });

        sleep(0.3);

        // Step 3: Check order status
        if (orderId) {
            group('Check Order Status', function () {
                const statusResponse = http.get(
                    `${ORDER_SERVICE_URL}/api/orders/${orderId}`,
                    { headers: headers }
                );

                check(statusResponse, {
                    'order status (200)': (r) => r.status === 200,
                });
            });

            sleep(0.2);

            // Step 4: List customer orders
            group('List Orders', function () {
                const listResponse = http.get(
                    `${ORDER_SERVICE_URL}/api/orders?customerId=${customerId}&page=0&size=10`,
                    { headers: headers }
                );

                check(listResponse, {
                    'list orders (200)': (r) => r.status === 200,
                });
            });

            // Step 5: Randomly cancel order (20% chance)
            if (Math.random() < 0.2) {
                sleep(0.2);
                group('Cancel Order', function () {
                    const cancelResponse = http.del(
                        `${ORDER_SERVICE_URL}/api/orders/${orderId}`,
                        null,
                        { headers: headers }
                    );

                    check(cancelResponse, {
                        'order cancelled': (r) => r.status === 200 || r.status === 400,
                    });
                });
            }
        }

        // Step 6: Final balance check
        group('Final Balance Check', function () {
            const finalBalanceResponse = http.get(
                `${ASSET_SERVICE_URL}/api/assets?customerId=${customerId}`,
                { headers: headers }
            );

            check(finalBalanceResponse, {
                'final balance check (200)': (r) => r.status === 200,
            });
        });
    });

    const totalTime = Date.now() - startTime;
    endToEndLatency.add(totalTime);

    if (totalTime < 5000) {
        tradingErrors.add(0);
    }

    sleep(Math.random() * 2 + 0.5);
}

export function handleSummary(data) {
    return {
        'stdout': textSummary(data),
        'reports/full-trading-summary.json': JSON.stringify(data),
    };
}

function textSummary(data) {
    let summary = '\n' + '='.repeat(70) + '\n';
    summary += 'FULL TRADING SCENARIO STRESS TEST SUMMARY\n';
    summary += '='.repeat(70) + '\n\n';

    summary += 'OVERALL METRICS:\n';
    summary += `-  Total Requests: ${data.metrics.http_reqs.values.count}\n`;
    summary += `-  Request Duration (avg): ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms\n`;
    summary += `-  Request Duration (p95): ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms\n`;
    summary += `-  Request Duration (p99): ${data.metrics.http_req_duration.values['p(99)'].toFixed(2)}ms\n`;
    summary += `-  Failed Requests: ${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)}%\n\n`;

    summary += 'TRADING METRICS:\n';
    summary += `-  Successful Trades: ${data.metrics.successful_trades ? data.metrics.successful_trades.values.count : 0}\n`;
    summary += `-  Trading Error Rate: ${(data.metrics.trading_errors.values.rate * 100).toFixed(2)}%\n`;

    if (data.metrics.e2e_latency) {
        summary += `-  E2E Latency (avg): ${data.metrics.e2e_latency.values.avg.toFixed(2)}ms\n`;
        summary += `-  E2E Latency (p95): ${data.metrics.e2e_latency.values['p(95)'].toFixed(2)}ms\n`;
    }

    summary += '\n' + '='.repeat(70) + '\n';
    return summary;
}
