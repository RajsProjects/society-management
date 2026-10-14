import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { BASE_URL, ADMIN_CREDENTIALS, THRESHOLDS } from './config.js';

const spikeErrorRate = new Rate('spike_error_rate');
const spikeResponseTime = new Trend('spike_response_time');
const droppedRequests = new Counter('dropped_requests');

export const options = {
    stages: [
        { duration: '10s', target: 20 },   // Warm-up baseline
        { duration: '10s', target: 250 },  // Intense spike to 250 concurrent users
        { duration: '20s', target: 250 },  // Maintain sustained spike pressure
        { duration: '10s', target: 50 },   // Recovery phase
        { duration: '10s', target: 0 },    // Graceful ramp down
    ],
    thresholds: {
        'spike_error_rate': ['rate<0.02'],
        'spike_response_time': ['p(95)<600', 'p(99)<1200'],
        'http_req_failed': ['rate<0.02'],
    },
};

export function setup() {
    const res = http.post(
        `${BASE_URL}/auth/login`,
        JSON.stringify(ADMIN_CREDENTIALS),
        { headers: { 'Content-Type': 'application/json', 'Connection': 'keep-alive' } }
    );
    if (res.status !== 200) {
        throw new Error(`Setup login failed with status ${res.status}: ${res.body}`);
    }
    return { token: JSON.parse(res.body).token };
}

export default function (data) {
    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${data.token}`,
            'Connection': 'keep-alive'
        }
    };

    // Mixed real-world resident traffic during a community emergency / spike
    group('Spike: User Profile Check', () => {
        const res = http.get(`${BASE_URL}/users/me`, params);
        spikeResponseTime.add(res.timings.duration);
        const ok = check(res, { 'profile 200': (r) => r.status === 200 });
        spikeErrorRate.add(!ok);
        if (!ok) droppedRequests.add(1);
    });

    sleep(0.2);

    group('Spike: Announcements Board', () => {
        const res = http.get(`${BASE_URL}/announcements?page=0&size=10`, params);
        spikeResponseTime.add(res.timings.duration);
        const ok = check(res, { 'announcements 200': (r) => r.status === 200 });
        spikeErrorRate.add(!ok);
        if (!ok) droppedRequests.add(1);
    });

    sleep(0.3);

    group('Spike: Maintenance Bills', () => {
        const res = http.get(`${BASE_URL}/finance/bills?page=0&size=10`, params);
        spikeResponseTime.add(res.timings.duration);
        const ok = check(res, { 'bills 200': (r) => r.status === 200 });
        spikeErrorRate.add(!ok);
        if (!ok) droppedRequests.add(1);
    });

    sleep(0.5);
}
