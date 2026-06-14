import { create } from 'zustand';
import AsyncStorage from '@react-native-async-storage/async-storage';

interface User { name?: string; email?: string; mobile?: string; village?: string; state?: string; }
interface AuthState {
  token: string | null;
  user: User | null;
  isAuthenticated: boolean;
  setAuth: (token: string, user: User) => void;
  logout: () => void;
  hydrate: () => Promise<void>;
}

// Minimal zustand-like store without the library
let _state: AuthState;
const listeners = new Set<() => void>();

const notify = () => listeners.forEach(fn => fn());

export const useAuthStore = (): AuthState => {
  // This is a simplified store - in production use zustand
  return _state;
};

export const authStore = {
  token: null as string | null,
  user: null as User | null,
  isAuthenticated: false,

  async setAuth(token: string, user: User) {
    this.token = token;
    this.user = user;
    this.isAuthenticated = true;
    await AsyncStorage.setItem('token', token);
    await AsyncStorage.setItem('user', JSON.stringify(user));
  },

  async logout() {
    this.token = null;
    this.user = null;
    this.isAuthenticated = false;
    await AsyncStorage.multiRemove(['token', 'user']);
  },

  async hydrate() {
    const token = await AsyncStorage.getItem('token');
    const userStr = await AsyncStorage.getItem('user');
    if (token) {
      this.token = token;
      this.isAuthenticated = true;
      if (userStr) this.user = JSON.parse(userStr);
    }
  },
};
