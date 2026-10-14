import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { BASE_URL, ADMIN_CREDENTIALS, THRESHOLDS } from './config.js';

const announcementDuration = new Trend('announcement_duration');
const announcementErrorRate = new Rate('announcement_error_rate');

export const options = {
    stages: [
        { duration: '10s', target: 50 },
        { duration: '20s', target: 200 },
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

    // 1. Fetch all announcements
    const resAll = http.get(`${BASE_URL}/announcements?page=0&size=20`, params);
    announcementDuration.add(resAll.timings.duration);
    const okAll = check(resAll, {
        'all announcements 200': (r) => r.status === 200,
        'all announcements < 300ms': (r) => r.timings.duration < 300,
    });
    announcementErrorRate.add(!okAll);

    sleep(0.3);

    // 2. Filter announcements by MAINTENANCE type
    const resFilter = http.get(`${BASE_URL}/announcements?type=MAINTENANCE&page=0&size=10`, params);
    announcementDuration.add(resFilter.timings.duration);
    const okFilter = check(resFilter, {
        'filtered announcements 200': (r) => r.status === 200,
        'filtered announcements < 300ms': (r) => r.timings.duration < 300,
    });
    announcementErrorRate.add(!okFilter);

    sleep(0.5);
}
