import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';
import { BASE_URL, ADMIN_CREDENTIALS, THRESHOLDS } from './config.js';

const billDuration = new Trend('bill_duration');

export const options = {
    stages: [
        { duration: '30s', target: 10 },
        { duration: '1m',  target: 10 },
        { duration: '30s', target: 30 },
        { duration: '1m',  target: 30 },
        { duration: '30s', target: 0 },
    ],
    thresholds: THRESHOLDS,
};

// authenticate once before all VUs start
export function setup() {
    const res = http.post(
        `${BASE_URL}/auth/login`,
        JSON.stringify(ADMIN_CREDENTIALS),
        { headers: { 'Content-Type': 'application/json' } }
    );
    return { token: JSON.parse(res.body).token };
}

export default function (data) {
    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${data.token}`
        }
    };

    // get all bills as admin
    const getBillsRes = http.get(
        `${BASE_URL}/finance/bills`,
        params
    );

    billDuration.add(getBillsRes.timings.duration);

    check(getBillsRes, {
        'get bills status is 200': (r) => r.status === 200,
        'get bills response time < 1000ms': (r) => r.timings.duration < 1000,
    });

    sleep(1);
}