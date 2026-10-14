export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081/api/v1';

export const ADMIN_CREDENTIALS = {
    email: __ENV.TEST_ADMIN_EMAIL || __ENV.PLATFORM_ADMIN_EMAIL || 'sumanmandal8955@gmail.com',
    password: __ENV.TEST_ADMIN_PASSWORD || __ENV.PLATFORM_ADMIN_PASSWORD || '@Raj5214895584099'
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