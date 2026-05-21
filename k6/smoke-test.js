import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, ADMIN_CREDENTIALS } from './config.js';

export const options = {
    vus: 2,
    duration: '1m',
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<2000'],
    },
};

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

    // verify all core endpoints respond
    check(http.get(`${BASE_URL}/issues`, params), {
        'issues ok': (r) => r.status === 200
    });

    check(http.get(`${BASE_URL}/announcements`, params), {
        'announcements ok': (r) => r.status === 200
    });

    check(http.get(`${BASE_URL}/finance/bills`, params), {
        'finance ok': (r) => r.status === 200
    });

    sleep(1);
}