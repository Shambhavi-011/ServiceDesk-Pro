import { axiosClient } from './axiosClient';
import { ApiResponse, JwtAuthResponse, UserProfile } from '../types';

export const authApi = {
  login: async (usernameOrEmail: string, password: string): Promise<JwtAuthResponse> => {
    const res = await axiosClient.post<ApiResponse<JwtAuthResponse>>('/auth/login', { usernameOrEmail, password });
    return res.data.data;
  },

  register: async (data: {
    username: string;
    email: string;
    password: string;
    firstName: string;
    lastName: string;
    department?: string;
  }): Promise<UserProfile> => {
    const res = await axiosClient.post<ApiResponse<UserProfile>>('/auth/register', data);
    return res.data.data;
  },

  getProfile: async (): Promise<UserProfile> => {
    const res = await axiosClient.get<ApiResponse<UserProfile>>('/auth/me');
    return res.data.data;
  },

  logout: async (): Promise<void> => {
    const refreshToken = localStorage.getItem('refreshToken');
    try {
      await axiosClient.post('/auth/logout', { refreshToken });
    } finally {
      localStorage.clear();
    }
  },
};