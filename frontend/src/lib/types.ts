// User Roles
export type Role = 'SUPER_ADMIN' | 'ADMIN' | 'ACCOUNTANT' | 'SECURITY' | 'RESIDENT';

// User Status
export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'PENDING';

// Auth Types
export interface User {
  id: number;
  email: string;
  name: string;
  phone: string;
  role: Role;
  status: UserStatus;
  societyId?: number;
  flatId?: number;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface SignupRequest {
  name: string;
  email: string;
  phone: string;
  password: string;
  role: Role;
  societyId?: number;
  flatId?: number;
}

// Society Types
export interface Society {
  id: number;
  name: string;
  address: string;
  city: string;
  state: string;
  pincode: string;
  totalFlats: number;
  createdAt: string;
}

// Flat Types
export interface Flat {
  id: number;
  flatNumber: string;
  floor: number;
  block: string;
  type: string;
  area: number;
  societyId: number;
  ownerId?: number;
  ownerName?: string;
  occupied: boolean;
}

export interface FlatResponse {
  id: number;
  flatNumber: string;
  floor: number;
  block: string;
  type: string;
  area: number;
  societyId: number;
  ownerName: string;
  ownerEmail: string;
  ownerPhone: string;
  occupied: boolean;
}

// Bill Types
export type BillStatus = 'PENDING' | 'PAID' | 'OVERDUE' | 'PARTIALLY_PAID';

export interface MaintenanceBill {
  id: number;
  flatId: number;
  flatNumber: string;
  amount: number;
  dueDate: string;
  status: BillStatus;
  billMonth: string;
  billYear: number;
  paidAmount: number;
  paidDate?: string;
  createdAt: string;
}

export interface CreateBillRequest {
  flatId: number;
  amount: number;
  dueDate: string;
  billMonth: string;
  billYear: number;
}

// Complaint Types
export type ComplaintStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';
export type ComplaintCategory = 'MAINTENANCE' | 'SECURITY' | 'NOISE' | 'PARKING' | 'CLEANLINESS' | 'OTHER';

export interface Complaint {
  id: number;
  title: string;
  description: string;
  category: ComplaintCategory;
  status: ComplaintStatus;
  priority: string;
  flatId: number;
  flatNumber: string;
  raisedBy: string;
  raisedByEmail: string;
  assignedTo?: string;
  resolution?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateComplaintRequest {
  title: string;
  description: string;
  category: ComplaintCategory;
  flatId: number;
}

// Issue Types
export type IssueStatus = 'REPORTED' | 'ACKNOWLEDGED' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';
export type IssuePriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface Issue {
  id: number;
  title: string;
  description: string;
  location: string;
  status: IssueStatus;
  priority: IssuePriority;
  reportedBy: string;
  reportedByEmail: string;
  assignedTo?: string;
  resolution?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateIssueRequest {
  title: string;
  description: string;
  location: string;
  priority: IssuePriority;
}

// Announcement Types
export type AnnouncementType = 'GENERAL' | 'URGENT' | 'MAINTENANCE' | 'EVENT' | 'MEETING';

export interface Announcement {
  id: number;
  title: string;
  content: string;
  type: AnnouncementType;
  societyId: number;
  createdBy: string;
  createdByEmail: string;
  expiresAt?: string;
  createdAt: string;
}

export interface CreateAnnouncementRequest {
  title: string;
  content: string;
  type: AnnouncementType;
  expiresAt?: string;
}

// Dashboard Types
export interface DashboardStats {
  totalFlats: number;
  occupiedFlats: number;
  totalResidents: number;
  pendingBills: number;
  overdueAmount: number;
  openComplaints: number;
  openIssues: number;
  recentAnnouncements: number;
}

// Pagination Types
export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// User Summary
export interface UserSummary {
  id: number;
  name: string;
  email: string;
  phone: string;
  role: Role;
  status: UserStatus;
  flatNumber?: string;
  createdAt: string;
}
