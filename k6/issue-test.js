import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';
import { BASE_URL, ADMIN_CREDENTIALS, THRESHOLDS } from './config.js';

const issueDuration = new Trend('issue_duration');
const issueErrors = new Counter('issue_errors');

export const options = {
    stages: [
        { duration: '30s', target: 20 },
        { duration: '1m',  target: 20 },
        { duration: '30s', target: 100 },
        { duration: '1m',  target: 100 },
        { duration: '30s', target: 0 },
    ],
    thresholds: THRESHOLDS,
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

    // create issue
    const createRes = http.post(
        `${BASE_URL}/issues`,
        JSON.stringify({
            title: `Load test issue ${Date.now()}`,
            description: 'This is a load test issue'
        }),
        params
    );

    issueDuration.add(createRes.timings.duration);

    check(createRes, {
        'create issue status is 201': (r) => r.status === 201,
        'create issue response time < 500ms': (r) => r.timings.duration < 500,
    });

    sleep(0.5);

    // get all issues
    const getRes = http.get(`${BASE_URL}/issues`, params);

    check(getRes, {
        'get issues status is 200': (r) => r.status === 200,
        'get issues response time < 300ms': (r) => r.timings.duration < 300,
        'get issues returns content': (r) => JSON.parse(r.body).content !== undefined,
    });

    if (getRes.status !== 200) {
        issueErrors.add(1);
    }

    sleep(1);
}