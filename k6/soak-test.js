import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { BASE_URL, ADMIN_CREDENTIALS } from './config.js';

const soakErrorRate = new Rate('soak_error_rate');
const soakResponseTime = new Trend('soak_response_time');

// Soak Test: Runs sustained, steady load for an extended period to identify:
// 1. ThreadLocal & Virtual thread memory leaks
// 2. MongoDB connection pool leaks (max-size 300)
// 3. Redis Lettuce connection exhaustion
// 4. Garbage collection pauses & memory creep over time
export const options = {
    stages: [
        { duration: '2m',  target: 50 },    // Ramp up to moderate operational traffic
        { duration: '30m', target: 50 },    // Sustained steady soak period (adjust up to 2-4 hours in staging)
        { duration: '2m',  target: 0 },     // Gentle ramp down
    ],
    thresholds: {
        'soak_error_rate': ['rate<0.01'],            // < 1% error rate over sustained duration
        'soak_response_time': ['p(95)<400', 'p(99)<900'], // Consistent latency without progressive degradation
        'http_req_failed': ['rate<0.01'],
    },
};

export function setup() {
    const res = http.post(
        `${BASE_URL}/auth/login`,
        JSON.stringify(ADMIN_CREDENTIALS),
        { headers: { 'Content-Type': 'application/json' } }
    );
    if (res.status !== 200) {
        throw new Error(`Soak test setup authentication failed (${res.status}): ${res.body}`);
    }
    return { token: JSON.parse(res.body).token };
}

export default function (data) {
    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${data.token}`
        }
    };

    group('Soak: User Profile & Tenant Identity', () => {
        const res = http.get(`${BASE_URL}/users/me`, params);
        soakResponseTime.add(res.timings.duration);
        soakErrorRate.add(res.status !== 200);
        check(res, { 'profile status 200': (r) => r.status === 200 });
        sleep(0.5);
    });

    group('Soak: Society Groups (Isolated Governance)', () => {
        const res = http.get(`${BASE_URL}/groups/my-groups`, params);
        soakResponseTime.add(res.timings.duration);
        soakErrorRate.add(res.status !== 200);
        check(res, { 'society group status 200': (r) => r.status === 200 });
        sleep(0.5);
    });

    group('Soak: Maintenance Billing Ledger', () => {
        const res = http.get(`${BASE_URL}/finance/bills?page=0&size=10`, params);
        soakResponseTime.add(res.timings.duration);
        soakErrorRate.add(res.status !== 200);
        check(res, { 'bills status 200': (r) => r.status === 200 });
        sleep(0.5);
    });

    group('Soak: Complaints Feed', () => {
        const res = http.get(`${BASE_URL}/complaints?page=0&size=10`, params);
        soakResponseTime.add(res.timings.duration);
        soakErrorRate.add(res.status !== 200);
        check(res, { 'complaints status 200': (r) => r.status === 200 });
        sleep(0.5);
    });

    group('Soak: Actuator Liveness Probe', () => {
        const res = http.get(`${BASE_URL}/health`);
        soakResponseTime.add(res.timings.duration);
        soakErrorRate.add(res.status !== 200);
        check(res, { 'health status 200': (r) => r.status === 200 });
        sleep(1);
    });
}
