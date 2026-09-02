import { axiosClient } from './axiosClient';
import { ApiResponse, NotificationItem } from '../types';

export const notificationApi = {
  getNotifications: async (unreadOnly = false): Promise<NotificationItem[]> => {
    const res = await axiosClient.get<ApiResponse<NotificationItem[]>>('/notifications', {
      params: { unreadOnly },
    });
    return res.data.data;
  },

  getUnreadCount: async (): Promise<number> => {
    const res = await axiosClient.get<ApiResponse<{ unreadCount: number }>>('/notifications/unread-count');
    return res.data.data.unreadCount;
  },

  markAsRead: async (id: number): Promise<void> => {
    await axiosClient.patch(`/notifications/${id}/read`);
  },

  markAllAsRead: async (): Promise<void> => {
    await axiosClient.patch('/notifications/read-all');
  },
};