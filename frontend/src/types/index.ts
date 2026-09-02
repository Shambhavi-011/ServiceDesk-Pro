export type RoleName = 'ROLE_EMPLOYEE' | 'ROLE_SUPPORT_AGENT' | 'ROLE_ADMIN';

export type TicketStatus = 'OPEN' | 'ASSIGNED' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED' | 'REOPENED';
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type AuditAction = 
  | 'TICKET_CREATED'
  | 'STATUS_CHANGED'
  | 'AGENT_ASSIGNED'
  | 'AGENT_REASSIGNED'
  | 'PRIORITY_CHANGED'
  | 'COMMENT_ADDED'
  | 'ATTACHMENT_UPLOADED'
  | 'TICKET_REOPENED'
  | 'TICKET_RESOLVED'
  | 'TICKET_CLOSED'
  | 'USER_ROLE_CHANGED'
  | 'USER_STATUS_CHANGED';

export interface UserProfile {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  department?: string;
  active: boolean;
  roles: RoleName[];
  createdAt: string;
}

export interface JwtAuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userId: number;
  username: string;
  email: string;
  fullName: string;
  roles: RoleName[];
}

export interface Category {
  id: number;
  name: string;
  description?: string;
  defaultSlaHours: number;
  active: boolean;
  createdAt: string;
}

export interface TicketSummary {
  id: number;
  ticketNumber: string;
  title: string;
  status: TicketStatus;
  priority: TicketPriority;
  categoryName: string;
  createdByFullName: string;
  createdByUsername: string;
  assignedToFullName?: string;
  assignedToUsername?: string;
  slaDueAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface TicketDetail {
  id: number;
  ticketNumber: string;
  title: string;
  description: string;
  status: TicketStatus;
  priority: TicketPriority;
  categoryId: number;
  categoryName: string;
  createdBy: UserProfile;
  assignedTo?: UserProfile;
  resolutionNotes?: string;
  slaDueAt?: string;
  resolvedAt?: string;
  closedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CommentItem {
  id: number;
  ticketId: number;
  authorId: number;
  authorFullName: string;
  authorUsername: string;
  content: string;
  internalNote: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AttachmentItem {
  id: number;
  ticketId: number;
  originalFileName: string;
  storedFileName: string;
  contentType: string;
  fileSizeBytes: number;
  uploadedByFullName: string;
  uploadedByUsername: string;
  createdAt: string;
}

export interface AuditLogItem {
  id: number;
  ticketId: number;
  ticketNumber: string;
  performedById: number;
  performedByFullName: string;
  performedByUsername: string;
  action: AuditAction;
  oldValue?: string;
  newValue?: string;
  notes?: string;
  timestamp: string;
}

export interface NotificationItem {
  id: number;
  ticketId?: number;
  ticketNumber?: string;
  title: string;
  message: string;
  read: boolean;
  createdAt: string;
}

export interface DashboardStats {
  userRole: 'EMPLOYEE' | 'SUPPORT_AGENT' | 'ADMIN';
  metrics: Record<string, number>;
  unreadNotificationsCount: number;
}

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  isLast: boolean;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}