export const BASE_URL = 'http://localhost:8080/api/v1';

export const ADMIN_CREDENTIALS = {
    email: __ENV.ADMIN_EMAIL || 'admin@society.com',
    password: __ENV.ADMIN_PASSWORD || 'ChangeMe123!'
};

export const TEST_RESIDENT = {
    email: `resident_${Date.now()}@test.com`,
    password: __ENV.TEST_RESIDENT_PASSWORD || 'ChangeMe123!',
    firstName: 'Test',
    lastName: 'Resident',
    apartmentNumber: 'A-101'
};

export const THRESHOLDS = {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    http_req_failed: ['rate<0.01'],
};