import { axiosClient } from './axiosClient';
import { ApiResponse, Category, CommentItem, AttachmentItem, PageResponse, TicketDetail, TicketPriority, TicketStatus, TicketSummary } from '../types';

export interface TicketFilterParams {
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
  status?: TicketStatus;
  priority?: TicketPriority;
  categoryId?: number;
  assignedToId?: number;
  search?: string;
  myTickets?: boolean;
  unassigned?: boolean;
}

export const ticketApi = {
  getCategories: async (): Promise<Category[]> => {
    const res = await axiosClient.get<ApiResponse<Category[]>>('/categories');
    return res.data.data;
  },

  getTickets: async (params?: TicketFilterParams): Promise<PageResponse<TicketSummary>> => {
    const res = await axiosClient.get<ApiResponse<PageResponse<TicketSummary>>>('/tickets', { params });
    return res.data.data;
  },

  getTicketById: async (id: number): Promise<TicketDetail> => {
    const res = await axiosClient.get<ApiResponse<TicketDetail>>(`/tickets/${id}`);
    return res.data.data;
  },

  createTicket: async (data: {
    title: string;
    description: string;
    categoryId: number;
    priority: TicketPriority;
  }): Promise<TicketDetail> => {
    const res = await axiosClient.post<ApiResponse<TicketDetail>>('/tickets', data);
    return res.data.data;
  },

  updateStatus: async (id: number, status: TicketStatus, notes?: string): Promise<TicketDetail> => {
    const res = await axiosClient.patch<ApiResponse<TicketDetail>>(`/tickets/${id}/status`, { status, notes });
    return res.data.data;
  },

  assignTicket: async (id: number, agentId?: number, notes?: string): Promise<TicketDetail> => {
    const res = await axiosClient.patch<ApiResponse<TicketDetail>>(`/tickets/${id}/assign`, { agentId, notes });
    return res.data.data;
  },

  resolveTicket: async (id: number, resolutionNotes: string): Promise<TicketDetail> => {
    const res = await axiosClient.patch<ApiResponse<TicketDetail>>(`/tickets/${id}/resolve`, { resolutionNotes });
    return res.data.data;
  },

  reopenTicket: async (id: number, reason: string): Promise<TicketDetail> => {
    const res = await axiosClient.patch<ApiResponse<TicketDetail>>(`/tickets/${id}/reopen`, { reason });
    return res.data.data;
  },

  closeTicket: async (id: number): Promise<TicketDetail> => {
    const res = await axiosClient.patch<ApiResponse<TicketDetail>>(`/tickets/${id}/close`);
    return res.data.data;
  },

  // Comments
  getComments: async (ticketId: number): Promise<CommentItem[]> => {
    const res = await axiosClient.get<ApiResponse<CommentItem[]>>(`/tickets/${ticketId}/comments`);
    return res.data.data;
  },

  addComment: async (ticketId: number, content: string, internalNote = false): Promise<CommentItem> => {
    const res = await axiosClient.post<ApiResponse<CommentItem>>(`/tickets/${ticketId}/comments`, { content, internalNote });
    return res.data.data;
  },

  // Attachments
  getAttachments: async (ticketId: number): Promise<AttachmentItem[]> => {
    const res = await axiosClient.get<ApiResponse<AttachmentItem[]>>(`/tickets/${ticketId}/attachments`);
    return res.data.data;
  },

  uploadAttachment: async (ticketId: number, file: File): Promise<AttachmentItem> => {
    const formData = new FormData();
    formData.append('file', file);
    const res = await axiosClient.post<ApiResponse<AttachmentItem>>(`/tickets/${ticketId}/attachments`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return res.data.data;
  },

  downloadAttachmentUrl: (attachmentId: number): string => `/api/attachments/${attachmentId}/download`,
};