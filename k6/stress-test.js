import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { BASE_URL, ADMIN_CREDENTIALS, THRESHOLDS } from './config.js';

const stressErrorRate = new Rate('stress_error_rate');
const stressResponseTime = new Trend('stress_response_time');

export const options = {
    stages: [
        { duration: '10s', target: 50 },    // Low baseline
        { duration: '15s', target: 150 },   // Moderate load
        { duration: '20s', target: 300 },   // High load
        { duration: '15s', target: 450 },   // Stress limit load (450 VUs)
        { duration: '10s', target: 0 },     // Ramp down
    ],
    thresholds: {
        'stress_error_rate': ['rate<0.02'],
        'stress_response_time': ['p(95)<750', 'p(99)<1500'],
        'http_req_failed': ['rate<0.02'],
    },
};

export function setup() {
    const res = http.post(
        `${BASE_URL}/auth/login`,
        JSON.stringify(ADMIN_CREDENTIALS),
        { headers: { 'Content-Type': 'application/json' } }
    );
    if (res.status !== 200) {
        throw new Error(`Setup failed with status ${res.status}: ${res.body}`);
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

    group('Stress: Issues', () => {
        const res = http.get(`${BASE_URL}/issues?page=0&size=20`, params);
        stressResponseTime.add(res.timings.duration);
        stressErrorRate.add(res.status !== 200);
        check(res, { 'issues status 200': (r) => r.status === 200 });
        sleep(0.3);
    });

    group('Stress: Bills', () => {
        const res = http.get(`${BASE_URL}/finance/bills?page=0&size=20`, params);
        stressResponseTime.add(res.timings.duration);
        stressErrorRate.add(res.status !== 200);
        check(res, { 'bills status 200': (r) => r.status === 200 });
        sleep(0.3);
    });

    group('Stress: Announcements', () => {
        const res = http.get(`${BASE_URL}/announcements?page=0&size=20`, params);
        stressResponseTime.add(res.timings.duration);
        stressErrorRate.add(res.status !== 200);
        check(res, { 'announcements status 200': (r) => r.status === 200 });
        sleep(0.4);
    });
}
