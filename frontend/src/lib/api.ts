import type {
  AuthResponse,
  LoginRequest,
  SignupRequest,
  DashboardStats,
  FlatResponse,
  MaintenanceBill,
  Complaint,
  Issue,
  Announcement,
  UserSummary,
  PagedResponse,
  CreateComplaintRequest,
  CreateIssueRequest,
  CreateAnnouncementRequest,
  CreateBillRequest,
} from './types';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'https://society-management-api-jys1.onrender.com';

class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
    this.name = 'ApiError';
  }
}

async function fetchApi<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
  
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...options.headers,
  };
  
  if (token) {
    (headers as Record<string, string>)['Authorization'] = `Bearer ${token}`;
  }
  
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });
  
  if (!response.ok) {
    const errorText = await response.text();
    let errorMessage = 'An error occurred';
    try {
      const errorJson = JSON.parse(errorText);
      errorMessage = errorJson.message || errorJson.error || errorText;
    } catch {
      errorMessage = errorText || `HTTP ${response.status}`;
    }
    throw new ApiError(response.status, errorMessage);
  }
  
  const text = await response.text();
  if (!text) return {} as T;
  return JSON.parse(text);
}

// Auth API
export const authApi = {
  login: (data: LoginRequest): Promise<AuthResponse> =>
    fetchApi('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
    
  signup: (data: SignupRequest): Promise<AuthResponse> =>
    fetchApi('/api/auth/signup', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
    
  getCurrentUser: (): Promise<AuthResponse['user']> =>
    fetchApi('/api/auth/me'),
};

// Dashboard API
export const dashboardApi = {
  getStats: (): Promise<DashboardStats> =>
    fetchApi('/api/dashboard/stats'),
};

// Flats API
export const flatsApi = {
  getAll: (page = 0, size = 10): Promise<PagedResponse<FlatResponse>> =>
    fetchApi(`/api/flats?page=${page}&size=${size}`),
    
  getById: (id: number): Promise<FlatResponse> =>
    fetchApi(`/api/flats/${id}`),
    
  create: (data: Partial<FlatResponse>): Promise<FlatResponse> =>
    fetchApi('/api/flats', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
    
  update: (id: number, data: Partial<FlatResponse>): Promise<FlatResponse> =>
    fetchApi(`/api/flats/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),
    
  delete: (id: number): Promise<void> =>
    fetchApi(`/api/flats/${id}`, {
      method: 'DELETE',
    }),
};

// Residents/Users API
export const usersApi = {
  getAll: (page = 0, size = 10): Promise<PagedResponse<UserSummary>> =>
    fetchApi(`/api/admin/users?page=${page}&size=${size}`),
    
  getById: (id: number): Promise<UserSummary> =>
    fetchApi(`/api/admin/users/${id}`),
    
  updateStatus: (id: number, status: string): Promise<UserSummary> =>
    fetchApi(`/api/admin/users/${id}/status?status=${status}`, {
      method: 'PUT',
    }),
    
  getPendingApprovals: (): Promise<UserSummary[]> =>
    fetchApi('/api/admin/users/pending'),
    
  approveUser: (id: number): Promise<UserSummary> =>
    fetchApi(`/api/admin/users/${id}/approve`, {
      method: 'PUT',
    }),
};

// Bills API
export const billsApi = {
  getAll: (page = 0, size = 10): Promise<PagedResponse<MaintenanceBill>> =>
    fetchApi(`/api/finance/bills?page=${page}&size=${size}`),
    
  getByFlat: (flatId: number): Promise<MaintenanceBill[]> =>
    fetchApi(`/api/finance/bills/flat/${flatId}`),
    
  getMyBills: (): Promise<MaintenanceBill[]> =>
    fetchApi('/api/finance/bills/my'),
    
  create: (data: CreateBillRequest): Promise<MaintenanceBill> =>
    fetchApi('/api/finance/bills', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
    
  generateMonthly: (month: string, year: number): Promise<void> =>
    fetchApi(`/api/finance/bills/generate?month=${month}&year=${year}`, {
      method: 'POST',
    }),
    
  markPaid: (id: number, amount: number): Promise<MaintenanceBill> =>
    fetchApi(`/api/finance/bills/${id}/pay?amount=${amount}`, {
      method: 'PUT',
    }),
};

// Complaints API
export const complaintsApi = {
  getAll: (page = 0, size = 10): Promise<PagedResponse<Complaint>> =>
    fetchApi(`/api/complaints?page=${page}&size=${size}`),
    
  getMyComplaints: (): Promise<Complaint[]> =>
    fetchApi('/api/complaints/my'),
    
  getById: (id: number): Promise<Complaint> =>
    fetchApi(`/api/complaints/${id}`),
    
  create: (data: CreateComplaintRequest): Promise<Complaint> =>
    fetchApi('/api/complaints', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
    
  updateStatus: (id: number, status: string, resolution?: string): Promise<Complaint> =>
    fetchApi(`/api/complaints/${id}/status?status=${status}${resolution ? `&resolution=${encodeURIComponent(resolution)}` : ''}`, {
      method: 'PUT',
    }),
};

// Issues API
export const issuesApi = {
  getAll: (page = 0, size = 10): Promise<PagedResponse<Issue>> =>
    fetchApi(`/api/issues?page=${page}&size=${size}`),
    
  getMyIssues: (): Promise<Issue[]> =>
    fetchApi('/api/issues/my'),
    
  getById: (id: number): Promise<Issue> =>
    fetchApi(`/api/issues/${id}`),
    
  create: (data: CreateIssueRequest): Promise<Issue> =>
    fetchApi('/api/issues', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
    
  updateStatus: (id: number, status: string, resolution?: string): Promise<Issue> =>
    fetchApi(`/api/issues/${id}/status?status=${status}${resolution ? `&resolution=${encodeURIComponent(resolution)}` : ''}`, {
      method: 'PUT',
    }),
    
  assign: (id: number, userId: number): Promise<Issue> =>
    fetchApi(`/api/issues/${id}/assign?userId=${userId}`, {
      method: 'PUT',
    }),
};

// Announcements API
export const announcementsApi = {
  getAll: (page = 0, size = 10): Promise<PagedResponse<Announcement>> =>
    fetchApi(`/api/announcements?page=${page}&size=${size}`),
    
  getActive: (): Promise<Announcement[]> =>
    fetchApi('/api/announcements/active'),
    
  getById: (id: number): Promise<Announcement> =>
    fetchApi(`/api/announcements/${id}`),
    
  create: (data: CreateAnnouncementRequest): Promise<Announcement> =>
    fetchApi('/api/announcements', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
    
  delete: (id: number): Promise<void> =>
    fetchApi(`/api/announcements/${id}`, {
      method: 'DELETE',
    }),
};

export { ApiError };
