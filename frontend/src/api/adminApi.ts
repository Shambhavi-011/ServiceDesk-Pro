import { axiosClient } from './axiosClient';
import { ApiResponse, AuditLogItem, Category, DashboardStats, PageResponse, UserProfile } from '../types';

export const adminApi = {
  getDashboardStats: async (): Promise<DashboardStats> => {
    const res = await axiosClient.get<ApiResponse<DashboardStats>>('/dashboard/stats');
    return res.data.data;
  },

  getUsers: async (page = 0, size = 10, activeOnly?: boolean): Promise<PageResponse<UserProfile>> => {
    const res = await axiosClient.get<ApiResponse<PageResponse<UserProfile>>>('/admin/users', {
      params: { page, size, activeOnly },
    });
    return res.data.data;
  },

  createAgent: async (data: {
    username: string;
    email: string;
    password: string;
    firstName: string;
    lastName: string;
    department: string;
  }): Promise<UserProfile> => {
    const res = await axiosClient.post<ApiResponse<UserProfile>>('/admin/agents', data);
    return res.data.data;
  },

  toggleUserStatus: async (userId: number, active: boolean): Promise<void> => {
    await axiosClient.patch(`/admin/users/${userId}/status`, { active });
  },

  createCategory: async (data: { name: string; description?: string; defaultSlaHours: number }): Promise<Category> => {
    const res = await axiosClient.post<ApiResponse<Category>>('/admin/categories', data);
    return res.data.data;
  },

  updateCategory: async (id: number, data: { name: string; description?: string; defaultSlaHours: number }): Promise<Category> => {
    const res = await axiosClient.put<ApiResponse<Category>>(`/admin/categories/${id}`, data);
    return res.data.data;
  },

  getAuditLogs: async (page = 0, size = 20): Promise<PageResponse<AuditLogItem>> => {
    const res = await axiosClient.get<ApiResponse<PageResponse<AuditLogItem>>>('/admin/audit-logs', {
      params: { page, size },
    });
    return res.data.data;
  },
};