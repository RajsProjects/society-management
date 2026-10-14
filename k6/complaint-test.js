import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';
import { BASE_URL, ADMIN_CREDENTIALS, THRESHOLDS } from './config.js';

const complaintDuration = new Trend('complaint_duration');
const complaintErrors = new Counter('complaint_errors');

export const options = {
    stages: [
        { duration: '10s', target: 30 },
        { duration: '15s', target: 100 },
        { duration: '10s', target: 0 },
    ],
    thresholds: THRESHOLDS,
};

export function setup() {
    const res = http.post(
        `${BASE_URL}/auth/login`,
        JSON.stringify(ADMIN_CREDENTIALS),
        { headers: { 'Content-Type': 'application/json' } }
    );
    if (res.status !== 200) {
        throw new Error(`Login failed with status ${res.status}: ${res.body}`);
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

    // 1. Create a complaint
    const payload = JSON.stringify({
        title: `Spike test complaint ${Date.now()}`,
        description: 'Water pressure low in B-wing during peak hours',
        category: 'PLUMBING'
    });

    const createRes = http.post(`${BASE_URL}/complaints`, payload, params);
    complaintDuration.add(createRes.timings.duration);

    const createdOk = check(createRes, {
        'create complaint is 201': (r) => r.status === 201,
        'create complaint < 500ms': (r) => r.timings.duration < 500,
    });

    if (!createdOk) complaintErrors.add(1);

    sleep(0.3);

    // 2. Fetch list of complaints
    const listRes = http.get(`${BASE_URL}/complaints?page=0&size=10`, params);
    complaintDuration.add(listRes.timings.duration);

    const listOk = check(listRes, {
        'get complaints is 200': (r) => r.status === 200,
        'get complaints < 300ms': (r) => r.timings.duration < 300,
    });

    if (!listOk) complaintErrors.add(1);

    sleep(0.5);
}
