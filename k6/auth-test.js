import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { BASE_URL, ADMIN_CREDENTIALS, THRESHOLDS } from './config.js';

const loginDuration = new Trend('login_duration');
const loginErrors = new Counter('login_errors');

export const options = {
    stages: [
        { duration: '30s', target: 10 },   // ramp up to 10 users
        { duration: '1m',  target: 10 },   // stay at 10 users
        { duration: '30s', target: 50 },   // ramp up to 50 users
        { duration: '1m',  target: 50 },   // stay at 50 users
        { duration: '30s', target: 0 },    // ramp down
    ],
    thresholds: THRESHOLDS,
};

export default function () {
    const params = {
        headers: { 'Content-Type': 'application/json' }
    };

    // test login
    const loginRes = http.post(
        `${BASE_URL}/auth/login`,
        JSON.stringify(ADMIN_CREDENTIALS),
        params
    );

    loginDuration.add(loginRes.timings.duration);

    const loginSuccess = check(loginRes, {
        'login status is 200': (r) => r.status === 200,
        'login returns token': (r) => JSON.parse(r.body).token !== undefined,
        'login response time < 500ms': (r) => r.timings.duration < 500,
    });

    if (!loginSuccess) {
        loginErrors.add(1);
    }

    sleep(1);
}