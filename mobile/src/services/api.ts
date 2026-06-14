import axios from 'axios';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Platform } from 'react-native';

// Web preview uses localhost, Android emulator uses 10.0.2.2, iOS uses localhost
const getBaseUrl = () => {
  if (Platform.OS === 'web') return 'http://localhost:8080';
  if (Platform.OS === 'android') return 'http://10.0.2.2:8080';
  return 'http://localhost:8080';
};

const api = axios.create({ baseURL: getBaseUrl(), timeout: 15000 });

api.interceptors.request.use(async (config) => {
  const token = await AsyncStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (res) => res,
  async (err) => {
    if (err.response?.status === 401) {
      await AsyncStorage.multiRemove(['token', 'user']);
    }
    return Promise.reject(err);
  }
);

// ─── Auth ───
export const authApi = {
  login: (identifier: string, password: string) =>
    api.post('/api/v1/auth/login', { identifier, password }),
  register: (data: any) => api.post('/api/v1/auth/register', data),
};

// ─── Farms ───
export const farmApi = {
  list: () => api.get('/api/v1/farms/my'),
  getById: (id: string) => api.get(`/api/v1/farms/${id}`),
  create: (data: any) => api.post('/api/v1/farms', data),
  update: (id: string, data: any) => api.put(`/api/v1/farms/${id}`, data),
  listCrops: (farmId: string) => api.get(`/api/v1/farms/${farmId}/crops`),
  addCrop: (farmId: string, data: any) => api.post(`/api/v1/farms/${farmId}/crops`, data),
};

// ─── Crops / Lifecycle ───
export const cropApi = {
  getMyCrops: () => api.get('/api/v1/crops/my'),
  getTodaysTasks: () => api.get('/api/v1/crops/tasks/today'),
  completeTask: (taskId: string) => api.put(`/api/v1/crops/tasks/${taskId}/complete`),
  getLifecycle: (cropId: string) => api.get(`/api/v1/crops/${cropId}/lifecycle`),
  uploadImage: (formData: FormData) =>
    api.post('/api/v1/crops/analyze', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 60000,
    }),
  getAnalysisHistory: (page: number = 0) =>
    api.get('/api/v1/crops/analyses', { params: { page, size: 15 } }),
  getAnalysis: (uploadId: string) => api.get(`/api/v1/crops/analyses/${uploadId}`),
};

// ─── User ───
export const userApi = {
  getProfile: () => api.get('/api/v1/users/me'),
  updateProfile: (data: any) => api.put('/api/v1/users/me', data),
};

// ─── Weather ───
export const weatherApi = {
  getCurrent: (lat?: number, lon?: number) => {
    const params: any = {};
    if (lat != null && lon != null) { params.lat = lat; params.lon = lon; }
    return api.get('/api/v1/weather/current', { params });
  },
};

// ─── Advisory ───
export const advisoryApi = {
  getRecommendations: (data: any) =>
    api.post('/api/v1/advisories/ai/crop-recommendations', data),
  getAiAdvisory: (lat: number, lon: number) =>
    api.get('/api/v1/advisories/ai', { params: { lat, lon } }),
  refreshAdvisory: (lat: number, lon: number) =>
    api.post('/api/v1/advisories/ai/refresh', null, { params: { lat, lon } }),
};

// ─── Market ───
export const marketApi = {
  getPrices: (crop?: string) => api.get('/api/v1/market/prices', { params: { crop } }),
  getMyCropPrices: () => api.get('/api/v1/market/my-crop-prices'),
};

// ─── Notifications ───
export const notificationApi = {
  list: (page: number = 0) => api.get('/api/v1/notifications', { params: { page, size: 20 } }),
  unreadCount: () => api.get('/api/v1/notifications/unread-count'),
  markRead: (id: string) => api.put(`/api/v1/notifications/${id}/read`),
  markAllRead: () => api.put('/api/v1/notifications/read-all'),
};

// ─── AI Chat ───
export const aiChatApi = {
  send: (message: string) => api.post('/api/v1/ai/chat', { message }),
};

export default api;
