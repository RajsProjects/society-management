import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { BASE_URL, ADMIN_CREDENTIALS, THRESHOLDS } from './config.js';

const errorRate = new Rate('error_rate');
const responseTime = new Trend('response_time');

export const options = {
    stages: [
        { duration: '1m',  target: 10 },   // warm up
        { duration: '2m',  target: 50 },   // ramp to 50 users
        { duration: '3m',  target: 100 },  // ramp to 100 users
        { duration: '2m',  target: 200 },  // ramp to 200 users
        { duration: '1m',  target: 0 },    // ramp down
    ],
    thresholds: {
        'http_req_duration': ['p(95)<500', 'p(99)<1000'],
        'http_req_failed': ['rate<0.01'],
        'error_rate': ['rate<0.05'],
    },
};

function getAuthToken() {
    const res = http.post(
        `${BASE_URL}/auth/login`,
        JSON.stringify(ADMIN_CREDENTIALS),
        { headers: { 'Content-Type': 'application/json' } }
    );
    return JSON.parse(res.body).token;
}

export default function () {
    const token = getAuthToken();
    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        }
    };

    group('Auth', () => {
        const res = http.post(
            `${BASE_URL}/auth/login`,
            JSON.stringify(ADMIN_CREDENTIALS),
            { headers: { 'Content-Type': 'application/json' } }
        );
        responseTime.add(res.timings.duration);
        errorRate.add(res.status !== 200);
        check(res, { 'login ok': (r) => r.status === 200 });
        sleep(0.5);
    });

    group('Issues', () => {
        const res = http.get(`${BASE_URL}/issues`, params);
        responseTime.add(res.timings.duration);
        errorRate.add(res.status !== 200);
        check(res, { 'get issues ok': (r) => r.status === 200 });
        sleep(0.5);
    });

    group('Announcements', () => {
        const res = http.get(`${BASE_URL}/announcements`, params);
        responseTime.add(res.timings.duration);
        errorRate.add(res.status !== 200);
        check(res, { 'get announcements ok': (r) => r.status === 200 });
        sleep(0.5);
    });

    group('Finance', () => {
        const res = http.get(`${BASE_URL}/finance/bills`, params);
        responseTime.add(res.timings.duration);
        errorRate.add(res.status !== 200);
        check(res, { 'get bills ok': (r) => r.status === 200 });
        sleep(0.5);
    });

    sleep(1);
}