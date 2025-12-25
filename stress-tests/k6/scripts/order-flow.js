import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Counter, Trend } from 'k6/metrics';

// Custom metrics
const orderCreationErrors = new Rate('order_creation_errors');
const orderCancellationErrors = new Rate('order_cancellation_errors');
const ordersCreated = new Counter('orders_created');
const orderDuration = new Trend('order_creation_duration');

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
const KEYCLOAK_URL = __ENV.KEYCLOAK_URL || 'http://localhost:8180';

export const options = {
    stages: [
        { duration: '30s', target: 10 },   // Ramp up to 10 users
        { duration: '1m', target: 50 },    // Ramp up to 50 users
        { duration: '2m', target: 100 },   // Steady load at 100 users
        { duration: '30s', target: 200 },  // Spike to 200 users
        { duration: '1m', target: 100 },   // Back to 100 users
        { duration: '30s', target: 0 },    // Ramp down
    ],
    thresholds: {
        http_req_duration: ['p(95)<1000', 'p(99)<2000'],  // 95% under 1s, 99% under 2s
        http_req_failed: ['rate<0.05'],                   // Less than 5% failures
        order_creation_errors: ['rate<0.1'],              // Less than 10% order creation errors
    },
};

// Get token from Keycloak
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

// Customer IDs for testing (should be pre-created)
const testCustomerIds = [
    'c0000000-0000-0000-0000-000000000001',
    'c0000000-0000-0000-0000-000000000002',
    'c0000000-0000-0000-0000-000000000003',
];

const assets = ['AAPL', 'GOOG', 'MSFT', 'AMZN', 'TSLA'];
const orderSides = ['BUY', 'SELL'];

export function setup() {
    // Get a token for the setup phase
    const token = getToken();
    if (!token) {
        console.error('Failed to get token in setup');
    }
    return { token };
}

export default function (data) {
    const token = data.token || getToken();
    if (!token) {
        orderCreationErrors.add(1);
        return;
    }

    const headers = {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
    };

    group('Order Flow', function () {
        // Create Order
        const customerId = testCustomerIds[Math.floor(Math.random() * testCustomerIds.length)];
        const asset = assets[Math.floor(Math.random() * assets.length)];
        const orderSide = orderSides[Math.floor(Math.random() * orderSides.length)];
        const price = (100 + Math.random() * 100).toFixed(2);
        const size = (1 + Math.floor(Math.random() * 100)).toString();

        const createOrderPayload = JSON.stringify({
            customerId: customerId,
            assetSymbol: asset,
            orderSide: orderSide,
            orderType: 'LIMIT',
            price: price,
            size: size,
        });

        const startTime = Date.now();
        const createResponse = http.post(
            `${BASE_URL}/api/orders`,
            createOrderPayload,
            { headers: headers }
        );
        orderDuration.add(Date.now() - startTime);

        const createSuccess = check(createResponse, {
            'order created (201)': (r) => r.status === 201 || r.status === 200,
            'order has id': (r) => {
                if (r.status === 201 || r.status === 200) {
                    const body = JSON.parse(r.body);
                    return body.data && body.data.id;
                }
                return false;
            },
        });

        if (createSuccess) {
            orderCreationErrors.add(0);
            ordersCreated.add(1);

            const orderData = JSON.parse(createResponse.body);
            const orderId = orderData.data.id;

            sleep(0.5);

            // Get Order
            const getResponse = http.get(
                `${BASE_URL}/api/orders/${orderId}`,
                { headers: headers }
            );

            check(getResponse, {
                'get order (200)': (r) => r.status === 200,
            });

            // Randomly cancel some orders (30% chance)
            if (Math.random() < 0.3) {
                sleep(0.3);

                const cancelResponse = http.del(
                    `${BASE_URL}/api/orders/${orderId}`,
                    null,
                    { headers: headers }
                );

                const cancelSuccess = check(cancelResponse, {
                    'order cancelled (200)': (r) => r.status === 200,
                });

                if (!cancelSuccess) {
                    orderCancellationErrors.add(1);
                } else {
                    orderCancellationErrors.add(0);
                }
            }
        } else {
            orderCreationErrors.add(1);
            console.error(`Order creation failed: ${createResponse.status} - ${createResponse.body}`);
        }
    });

    sleep(Math.random() * 2);
}

export function handleSummary(data) {
    return {
        'stdout': textSummary(data, { indent: ' ', enableColors: true }),
        'reports/order-flow-summary.json': JSON.stringify(data),
    };
}

function textSummary(data, options) {
    const indent = options.indent || '  ';
    let summary = '\n' + '='.repeat(60) + '\n';
    summary += 'ORDER FLOW STRESS TEST SUMMARY\n';
    summary += '='.repeat(60) + '\n\n';

    summary += `Total Requests: ${data.metrics.http_reqs.values.count}\n`;
    summary += `Request Duration (avg): ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms\n`;
    summary += `Request Duration (p95): ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms\n`;
    summary += `Request Duration (p99): ${data.metrics.http_req_duration.values['p(99)'].toFixed(2)}ms\n`;
    summary += `Failed Requests: ${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)}%\n`;
    summary += `Orders Created: ${data.metrics.orders_created ? data.metrics.orders_created.values.count : 0}\n`;
    summary += `Order Creation Error Rate: ${(data.metrics.order_creation_errors.values.rate * 100).toFixed(2)}%\n`;

    summary += '\n' + '='.repeat(60) + '\n';
    return summary;
}
